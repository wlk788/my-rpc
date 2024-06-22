package com.wlk;

import com.wlk.utils.zookeeper.ZookeeperNode;
import com.wlk.utils.zookeeper.ZookeeperUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 注册中心的管理页面
 */
public class Application {

    public static void main(String[] args) {
        // 帮我们创建基础目录
        // yrpc-metadata   (持久节点)
        //  └─ providers （持久节点）
        //  		└─ service1  （持久节点，接口的全限定名）
        //  		    ├─ node1 [data]     /ip:port
        //  		    ├─ node2 [data]
        //            └─ node3 [data]
        //  └─ consumers
        //        └─ service1
        //             ├─ node1 [data]
        //             ├─ node2 [data]
        //             └─ node3 [data]
        //  └─ config

        ZooKeeper zooKeeper = ZookeeperUtils.createZookeeper();
        String basePath = "/myrpc-metadata";
        String providerPath = basePath + "/providers";
        String consumersPath = basePath + "/consumers";
        ZookeeperNode baseNode = new ZookeeperNode(basePath, null);
        ZookeeperNode providersNode = new ZookeeperNode(providerPath, null);
        ZookeeperNode consumersNode = new ZookeeperNode(consumersPath, null);

        List<ZookeeperNode> list = new ArrayList<>();
        Collections.addAll(list, baseNode, providersNode, consumersNode);
        list.forEach(node -> ZookeeperUtils.createNode(zooKeeper, node, null, CreateMode.PERSISTENT));

        ZookeeperUtils.close(zooKeeper);
    }
}
