package com.kangyonggan.app.simclient;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * @author kangyonggan
 * @since 2017/2/16
 */
@Log4j2
public class SimClient extends PropertyPlaceholderConfigurer {

    public SimClient(String publicKeyPath, String privateKeyPath, String httpUrl, String projCode, String env, String localProp) {
        log.info("公钥路径:{}", publicKeyPath);
        log.info("私钥路径:{}", privateKeyPath);
        log.info("请求地址:{}", httpUrl);
        log.info("项目代码:{}", projCode);
        log.info("环境代码:{}", env);
        log.info("本地配置:{}, 使用说明===>>['null': 不使用本地配置, 'classpath:xxx.properties': 使用resources目录下的配置，'/root/config/xxx.properties': 使用本地配置]", localProp);

        // 私钥
        final PrivateKey privateKey = SecretUtil.getPrivateKey(privateKeyPath);

        // 公钥
        final PublicKey publicKey = SecretUtil.getPublicKey(publicKeyPath);

        try {
            // 报文
            String plain = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
            byte plainBytes[] = plain.getBytes("UTF-8");
            log.info("请求报文明文:{}", plain);

            // 签名
            byte[] signBytes = CryptoUtil.digitalSign(plainBytes, privateKey, "SHA1WithRSA");
            log.info("请求报文签名数据:signBytes.length={}", signBytes.length);

            // 加密
            byte[] encryptedBytes = CryptoUtil.encrypt(plainBytes, publicKey, 2048, 11, "RSA/ECB/PKCS1Padding");
            log.info("请求报文密文:encryptedBytes.length={}", encryptedBytes.length);

            // 报文头
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.leftPad(String.valueOf(15 + 8 + 4 + signBytes.length + encryptedBytes.length), 8, "0"));
            sb.append(StringUtils.leftPad(projCode, 15, " "));
            sb.append(StringUtils.leftPad(env, 8, " "));
            sb.append(StringUtils.leftPad(String.valueOf(signBytes.length), 4, "0"));
            log.info("请求报文头:{}", sb.toString());

            // 组装报文
            byte[] bytes = null;
            bytes = ArrayUtils.addAll(bytes, sb.toString().getBytes("UTF-8"));
            bytes = ArrayUtils.addAll(bytes, signBytes);
            bytes = ArrayUtils.addAll(bytes, encryptedBytes);

            // 发送
            byte result[] = HttpUtil.sendHttpPost(httpUrl, bytes);

            // 解析
            parse(result, publicKey, privateKey);

            // 加载本地配置
            if (StringUtils.isEmpty(localProp)) {
                log.info("本地配置文件路径为空，不使用本地配置");
            } else if (localProp.startsWith("classpath:")) {
                localProp = localProp.substring(10);
                log.info("加载项目配置:{}", localProp);
                loadAppProperties(localProp);
            } else {
                log.info("加载本地配置:{}", localProp);
                loadLocalProperties(localProp);
            }
        } catch (Exception e) {
            log.error("请求报文发送失败", e);
        }


    }

    /**
     * 加载项目配置
     *
     * @param localProp
     */
    private void loadAppProperties(String localProp) {
        InputStreamReader reader = null;
        try {
            Properties props = new Properties();
            reader = new InputStreamReader(SimClient.class.getClassLoader().getResourceAsStream(localProp), "UTF-8");
            props.load(reader);
            PropertiesUtil.loadProperties(props);
        } catch (Exception e) {
            log.error("加载项目配置文件失败", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    /**
     * 加载本地配置
     *
     * @param localProp
     */
    private void loadLocalProperties(String localProp) {
        InputStreamReader reader = null;
        try {
            Properties props = new Properties();
            reader = new InputStreamReader(new FileInputStream(localProp), "UTF-8");
            props.load(reader);
            PropertiesUtil.loadProperties(props);
        } catch (Exception e) {
            log.error("加载本地配置文件失败", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    /**
     * 解析报文
     *
     * @param b
     * @param publicKey
     * @param privateKey
     */
    private void parse(byte b[], PublicKey publicKey, PrivateKey privateKey) {
        try {
            // 报文头（总长度8+签名长度4）= 12位
            String header = new String(b, 0, 12);
            log.info("响应报文头:{}", header);

            // 总长度,0~8位
            int totalLen = Integer.parseInt(new String(b, 0, 8));
            log.info("响应报文总长度:{}", totalLen);

            // 从消息头中获取签名长度，用于读取签名
            String signLenStr = new String(b, 8, 4);// 签名长度,最后四位8~12
            int signLen = Integer.parseInt(signLenStr);
            log.info("响应报文签名长度:{}", signLen);

            // 计算加密后报文体的长度，用于读取报文体（总长-头-签=密）
            int encryptedBytesLen = totalLen - 12 - signLen;
            log.info("响应报文密文长度:{}", encryptedBytesLen);

            // 签名
            byte[] signBytes = ArrayUtils.subarray(b, 12, signLen + 12);
            log.info("响应报文的签名:signBytes.length={}", signBytes.length);

            // 密文
            byte[] encryptedBytes = ArrayUtils.subarray(b, 12 + signLen, encryptedBytesLen + 12 + signLen);
            log.info("响应报文的密文bytes:encryptedBytes.length={}", encryptedBytes.length);

            // 解密
            byte xmlBytes[] = CryptoUtil.decrypt(encryptedBytes, privateKey, 2048, 11, "RSA/ECB/PKCS1Padding");
            String xml = new String(xmlBytes, "UTF-8");
            log.info("响应报文解密后:{}", xml);

            // 验签
            boolean isValid = CryptoUtil.verifyDigitalSign(xmlBytes, signBytes, publicKey, "SHA1WithRSA");// 验签
            log.info("响应报文验签结果:{}", isValid);

            if (isValid) {
                JSONArray jsonArray = JSONArray.parseArray(xml);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String name = jsonObject.getString("name").trim();
                    String value = jsonObject.getString("value").trim();
                    log.info("加载远程配置: name={}, value={}", name, value);
                    System.setProperty(name, value);
                    PropertiesUtil.putProperties(name, value);
                }
            }
        } catch (Exception e) {
            log.error("报文解析出错", e);
        }
    }
}
