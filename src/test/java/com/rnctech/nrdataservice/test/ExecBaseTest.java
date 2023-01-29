package com.rnctech.nrdataservice.test;

import java.lang.reflect.Field;
import java.util.Map;

public class ExecBaseTest {

    public static void setEnv(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }

    public static void delEnv(String key) {
        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.remove(key);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }
    
    public static void disableKafka(){
        delEnv("kafka_brokers");
    }
    
    
}
