package com.kangyonggan.app.simclient;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author kangyonggan
 * @since 2017/2/15
 */
public class SecretUtil {

    /**
     * 加载公钥
     */
    public static PublicKey getPublicKey(String publicKeyPath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(publicKeyPath);
            return CryptoUtil.getPublicKey(inputStream, "RSA");
        } catch (Exception e) {
            System.out.println("无法加载对方公钥[" + publicKeyPath + "]");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                System.out.println("关闭流失败");
            }
        }
        return null;
    }

    /**
     * 加载私钥
     */
    public static PrivateKey getPrivateKey(String privateKeyPath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(privateKeyPath);
            return CryptoUtil.getPrivateKey(inputStream, "RSA");
        } catch (Exception e) {
            System.out.println("无法加载已方私钥[" + privateKeyPath + "]");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                System.out.println("关闭流失败");
            }
        }
        return null;
    }

}
