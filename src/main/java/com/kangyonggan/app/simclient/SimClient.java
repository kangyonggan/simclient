package com.kangyonggan.app.simclient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * @author kangyonggan
 * @since 2017/2/16
 */
public class SimClient {

    private String publicKeyPath;

    private String privateKeyPath;

    private String host;

    private int port;

    private String projCode;

    private String env;

    public SimClient() {
        // 私钥
        final PrivateKey privateKey = SecretUtil.getPrivateKey(privateKeyPath);

        // 公钥
        final PublicKey publicKey = SecretUtil.getPublicKey(publicKeyPath);

        Socket socket = null;
        try {
            // 报文
            String plain = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
            byte plainBytes[] = plain.getBytes("UTF-8");

            // 签名
            byte[] signBytes = CryptoUtil.digitalSign(plainBytes, privateKey, "SHA1WithRSA");

            // 加密
            byte[] encryptedBytes = CryptoUtil.encrypt(plainBytes, publicKey, 2048, 11, "RSA/ECB/PKCS1Padding");

            StringBuffer buffer = new StringBuffer();
            buffer.append(StringUtils.leftPad(String.valueOf(15 + 8 + 4 + signBytes.length + encryptedBytes.length), 8, "0"));
            buffer.append(StringUtils.leftPad(projCode, 15, " "));
            buffer.append(StringUtils.leftPad(env, 8, " "));
            buffer.append(StringUtils.leftPad(String.valueOf(signBytes.length), 4, "0"));

            byte[] bytes = null;
            bytes = ArrayUtils.addAll(bytes, buffer.toString().getBytes("UTF-8"));
            bytes = ArrayUtils.addAll(bytes, signBytes);
            bytes = ArrayUtils.addAll(bytes, encryptedBytes);

            socket = new Socket(host, port);
            OutputStream os = socket.getOutputStream();
            os.write(bytes);
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final Socket finalSocket = socket;
        new Thread(){
            @Override
            public void run() {
                try {
                    InputStream in = finalSocket.getInputStream();
                    int len;
                    byte b[] = new byte[9999];
                    while ((len = in.read(b, 0, 12)) != -1) {
                        // 报文头（总长度8+签名长度4）= 12位
                        String header = new String(b, 0, len);

                        // 总长度,0~8位
                        int totalLen = Integer.parseInt(new String(b, 0, 8)) + 8;

                        // 从消息头中获取签名长度，用于读取签名
                        String signLenStr = new String(b, 8, 4);// 签名长度,最后四位8~12
                        int signLen = Integer.parseInt(signLenStr);

                        // 计算加密后报文体的长度，用于读取报文体（总长-头-签=密）
                        int encryptedBytesLen = totalLen - 12 - signLen;

                        // 签名
                        in.read(b, 0, signLen);
                        byte[] signBytes = ArrayUtils.subarray(b, 0, signLen);

                        // 密文
                        byte[] encryptedBytes = new byte[encryptedBytesLen];
                        in.read(encryptedBytes, 0, encryptedBytesLen);

                        // 解密
                        byte xmlBytes[] = CryptoUtil.decrypt(encryptedBytes, privateKey, 2048, 11, "RSA/ECB/PKCS1Padding");
                        String xml = new String(xmlBytes, "UTF-8");

                        // 验签
                        boolean isValid = CryptoUtil.verifyDigitalSign(xmlBytes, signBytes, publicKey, "SHA1WithRSA");// 验签

                        if (isValid) {
                            JSONArray jsonArray = JSONArray.parseArray(xml);
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String name = jsonObject.getString("name").trim();
                                String value = jsonObject.getString("value").trim();
                                System.setProperty(name, value);
                                PropertiesUtil.putProperties(name, value);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProjCode() {
        return projCode;
    }

    public void setProjCode(String projCode) {
        this.projCode = projCode;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }
}
