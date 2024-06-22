package com.wlk;

import com.wlk.discovery.Registry;
import com.wlk.discovery.RegistryConfig;
import com.wlk.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MyrpcBootstrap {
    private static MyrpcBootstrap myrpcBootstrap = new MyrpcBootstrap();

    private String applicationName = "default";
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;

    private Registry registry;
    private ZooKeeper zookeeper;

    // 维护已经发布且暴露的服务列表 key-> interface的全限定名  value -> ServiceConfig
    public final static Map<String, ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

    public MyrpcBootstrap() {
        zookeeper = ZookeeperUtils.createZookeeper();
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
        this.registryConfig = registryConfig;
        this.registry = registryConfig.getRegistry();
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
        registry.registry(service);
        SERVERS_LIST.put(service.getInterface().getName(), service);
        return this;
    }

    /**
     * 批量发布服务
     * @param services
     * @return
     */
    public MyrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for(ServiceConfig<?> service : services){
            this.publish(service);
        }
        return this;
    }

    public void start(){
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public MyrpcBootstrap reference(ReferenceConfig<?> reference) {
        //在这个方法里我们是否可以拿到相关的配置项-注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        reference.setRegistry(registry);
        return this;
    }

}
