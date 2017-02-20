package com.kangyonggan.app.simclient;

import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * @author kangyonggan
 * @since 2017/2/20
 */
@Log4j2
public class HttpUtil {

    private static RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(15000)
            .setConnectTimeout(15000)
            .setConnectionRequestTimeout(15000)
            .build();

    /**
     * 发送 post请求
     *
     * @param httpUrl 地址
     * @param params  参数(格式:key1=value1&key2=value2)
     */
    public static byte[] sendHttpPost(String httpUrl, byte[] params) {
        HttpPost httpPost = new HttpPost(httpUrl);// 创建httpPost
        try {
            //设置参数
            ByteArrayEntity arrayEntity = new ByteArrayEntity(params);
            httpPost.setEntity(arrayEntity);
        } catch (Exception e) {
            log.error("发送失败", e);
        }
        return sendHttpPost(httpPost);
    }

    /**
     * 发送Post请求
     *
     * @param httpPost
     * @return
     */
    private static byte[] sendHttpPost(HttpPost httpPost) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        HttpEntity entity;
        byte[] responseContent = null;
        try {
            // 创建默认的httpClient实例.
            httpClient = HttpClients.createDefault();
            httpPost.setConfig(requestConfig);
            // 执行请求
            response = httpClient.execute(httpPost);
            entity = response.getEntity();
            responseContent = EntityUtils.toByteArray(entity);
            log.info("请求发送成功");
        } catch (Exception e) {
            log.error("发送失败", e);
        } finally {
            try {
                // 关闭连接,释放资源
                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                log.error(e);
            }
        }
        return responseContent;
    }

}
