package com.wlk;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class MyrpcBootstrap {
    private static MyrpcBootstrap myrpcBootstrap = new MyrpcBootstrap();

    public MyrpcBootstrap() {
    }

    public static MyrpcBootstrap getInstance(){
        return myrpcBootstrap;
    }

    public MyrpcBootstrap application() {
        return this;
    }

    /**
     * 注册中心
     * @param registryConfig
     * @return
     */
    public MyrpcBootstrap registry(RegistryConfig registryConfig){
        return this;
    }

    /**
     * 序列化协议
     * @param protocolConfig
     * @return
     */
    public MyrpcBootstrap protocol(ProtocolConfig protocolConfig){
        return this;
    }

    /**
     * 封装需要发布的服务
     * @param service
     * @return
     */
    public MyrpcBootstrap publish(ServiceConfig<?> service) {
        if(log.isDebugEnabled()){
            log.debug("服务{}，已经被注册",service.getInterface().getName());
        }
        return this;
    }

    /**
     * 批量发布服务
     * @param services
     * @return
     */
    public MyrpcBootstrap publish(List<ServiceConfig<?>> services) {
        return this;
    }

    public void start(){

    }

    public MyrpcBootstrap reference(ReferenceConfig<?> reference) {
        //在这个方法里我们是否可以拿到相关的配置项-注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        return this;
    }

}
