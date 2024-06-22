package com.wlk;

import com.wlk.discovery.Registry;
import com.wlk.discovery.RegistryConfig;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println("hello proxy");
                registry.lookup(interfaceRef.getName(), group);
                return null;
            }
        });

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

}
