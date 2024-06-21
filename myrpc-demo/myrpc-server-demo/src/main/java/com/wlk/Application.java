package com.wlk;

import com.wlk.impl.HelloMyrpcImpl;

public class Application {
    public static void main(String[] args) {
        // 1、服务提供方，需要注册服务，启动服务//
        // 1、封装要发布的服务
        ServiceConfig<HelloMyrpc> service = new ServiceConfig<>();
        service.setInterface(HelloMyrpc.class);
        service.setRef(new HelloMyrpcImpl());
        // 2、定义注册中心
        // 3、通过启动引导程序，启动服务提供方/ /
        //(1）配置--应用的名称--注册中心
        //(2）发布服务
        MyrpcBootstrap.getInstance()
                .application()
                .registry(new RegistryConfig())
                .protocol(new ProtocolConfig("jdk"))
                .publish(service)
                .start();

    }
}
