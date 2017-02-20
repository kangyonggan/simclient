package com.kangyonggan.app.simclient;

import java.util.Properties;

/**
 * properties工具类
 *
 * @author kangyonggan
 * @since 2016/12/6
 */
public class PropertiesUtil {

    private static Properties props = new Properties();

    private PropertiesUtil() {
    }

    /**
     * 获取properties的值，默认值""
     *
     * @param name
     * @return
     */
    public static String getProperties(String name) {
        return getPropertiesOrDefault(name, "");
    }

    /**
     * 获取properties的值
     *
     * @param name
     * @param defaultValue 默认值
     * @return
     */
    public static String getPropertiesOrDefault(String name, String defaultValue) {
        return props.getProperty(name, defaultValue);
    }

    /**
     * 设置properties的值
     *
     * @param name
     * @param value
     */
    public static void putProperties(String name, String value) {
        props.put(name, value);
    }

    /**
     * 加载配置
     *
     * @param properties
     */
    public static void loadProperties(Properties properties) {
        for (Object key : properties.keySet()) {
            String name = (String) key;
            String value = properties.getProperty(name);
            putProperties(name, value);
            System.setProperty(name, value);
        }
    }

}

