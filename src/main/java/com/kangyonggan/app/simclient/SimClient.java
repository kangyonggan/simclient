package com.kangyonggan.app.simclient;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author kangyonggan
 * @since 2017/2/16
 */
public class SimClient {

    public static void main(String[] args) throws Exception {
        // 私钥
        String privateKeyPath = "/Users/kyg/data/ssl/test-local/pkcs8_rsa_private_key_2048.pem";
        PrivateKey privateKey = SecretUtil.getPrivateKey(privateKeyPath);

        // 公钥
        String publicKeyPath = "/Users/kyg/data/ssl/sim-test-local/rsa_public_key_2048.pem";
        PublicKey publicKey = SecretUtil.getPublicKey(publicKeyPath);

        // 报文
        String plain = "test|local";
        byte plainBytes[] = plain.getBytes("UTF-8");

        // 签名
        byte[] signBytes = CryptoUtil.digitalSign(plainBytes, privateKey, "SHA1WithRSA");

        // 加密
        byte[] encryptedBytes = CryptoUtil.encrypt(plainBytes, publicKey, 2048, 11, "RSA/ECB/PKCS1Padding");

        StringBuffer buffer = new StringBuffer();
        buffer.append(StringUtils.leftPad(String.valueOf(15 + 8 + 4 + signBytes.length + encryptedBytes.length), 8, "0"));
        buffer.append(StringUtils.leftPad("test", 15, " "));
        buffer.append(StringUtils.leftPad("local", 8, " "));
        buffer.append(StringUtils.leftPad(String.valueOf(signBytes.length), 4, "0"));

        byte[] bytes = null;
        bytes = ArrayUtils.addAll(bytes, buffer.toString().getBytes("UTF-8"));
        bytes = ArrayUtils.addAll(bytes, signBytes);
        bytes = ArrayUtils.addAll(bytes, encryptedBytes);

        Socket socket = new Socket("127.0.0.1", 17777);
        OutputStream os = socket.getOutputStream();
        os.write(bytes);
        os.flush();

        InputStream in = socket.getInputStream();
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
            signBytes = ArrayUtils.subarray(b, 0, signLen);

            // 密文
            encryptedBytes = new byte[encryptedBytesLen];
            in.read(encryptedBytes, 0, encryptedBytesLen);

            // 解密
            byte xmlBytes[] = CryptoUtil.decrypt(encryptedBytes, privateKey, 2048, 11, "RSA/ECB/PKCS1Padding");
            String xml = new String(xmlBytes, "UTF-8");

            // 验签
            boolean isValid = CryptoUtil.verifyDigitalSign(xmlBytes, signBytes, publicKey, "SHA1WithRSA");// 验签

            System.out.println(isValid);
            System.out.println("响应报文:" + xml);
        }


    }

}
