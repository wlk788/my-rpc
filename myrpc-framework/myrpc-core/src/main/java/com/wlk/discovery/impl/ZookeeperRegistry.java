package com.wlk.discovery.impl;

import com.wlk.Constant;
import com.wlk.MyrpcBootstrap;
import com.wlk.ServiceConfig;
import com.wlk.discovery.Registry;
import com.wlk.exceptions.DiscoveryException;
import com.wlk.utils.NetUtils;
import com.wlk.utils.zookeeper.ZookeeperNode;
import com.wlk.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ZookeeperRegistry implements Registry {

    // 维护一个zk实例
    private ZooKeeper zooKeeper;
    private int port = 8088;

    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    public ZookeeperRegistry(String host, int timeOut) {
        this.zooKeeper = ZookeeperUtils.createZookeeper(host,timeOut);
    }

    @Override
    public void registry(ServiceConfig<?> serviceConfig) {
        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + serviceConfig.getInterface().getName();
        if(!ZookeeperUtils.exists(zooKeeper, parentNode, null)){
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }
        //todo: 后续处理端口的问题
        String hostNode = parentNode + "/" + NetUtils.getIp() + ":" + port;
        if(!ZookeeperUtils.exists(zooKeeper, hostNode, null)){
            ZookeeperNode zookeeperNode = new ZookeeperNode(hostNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.EPHEMERAL);
        }
        if(log.isDebugEnabled()){
            log.debug("服务{}，已经被注册",serviceConfig.getInterface().getName());
        }
    }

    @Override
    public List<InetSocketAddress> lookup(String serviceName, String group) {
        //1、找到服务的节点
        String serviceNode = Constant.BASE_PROVIDERS_PATH + "/" + serviceName + "/" + group;
        //2、找到子节点
        List<String> children = ZookeeperUtils.getChildren(zooKeeper, serviceNode, null);
        // 获取了所有可用的列表
        List<InetSocketAddress> inetSocketAddresses = new ArrayList<>();
        for(String ipString : children){
            String[] ipAndPort = ipString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.parseInt(ipAndPort[1]);
            inetSocketAddresses.add(new InetSocketAddress(ip, port));
        }

        if(inetSocketAddresses.size() == 0){
            throw new DiscoveryException("未发现注册的服务");
        }
        return inetSocketAddresses;
    }
}
