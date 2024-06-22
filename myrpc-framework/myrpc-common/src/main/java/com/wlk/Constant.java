package com.wlk;

public class Constant {
    //zookeeper连接地址
    public static final String DEFAULT_ZK_CONNECT = "127.0.0.1:2181";
    //zookeeper超时时间
    public static final int TIME_OUT = 10000;

    //服务提供者前置路径
    public static final String BASE_PROVIDERS_PATH = "/myrpc-metadata/providers";
    //服务消费者前置路径
    public static final String BASE_CONSUMERS_PATH = "/myrpc-metadata/consumers";
}
