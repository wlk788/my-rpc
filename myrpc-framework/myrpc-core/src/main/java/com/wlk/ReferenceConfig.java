package com.wlk;

import com.wlk.discovery.Registry;
import com.wlk.proxy.handler.RpcConsumerInvocationHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;

@Slf4j
public class ReferenceConfig<T> {
    private Class<T> interfaceRef;
    private Registry registry;

    // 分组信息
    private String group;

    public ReferenceConfig() {
    }

    public ReferenceConfig(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

//    public ReferenceConfig(Registry registry) {
//        this.registry = registry;
//    }

    /**
     * 代理设计模式，生成一个api接口的代理对象，helloYrpc.sayHi("你好");
     * @return 代理对象
     */
    public T get(){
        // 使用动态代理完成了一些工作
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class[] classes = new Class[]{interfaceRef};
        RpcConsumerInvocationHandler handler = new RpcConsumerInvocationHandler(registry, interfaceRef, group);
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, handler);

        return (T)helloProxy;
    }

    public Class<T> getInterface() {
        return interfaceRef;
    }

    public void setInterface(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

}
