package com.wlk;

import com.wlk.utils.MyWatcher;
import com.wlk.utils.ZookeeperNode;
import com.wlk.utils.ZookeeperUtil;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
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

        ZooKeeper zooKeeper = ZookeeperUtil.createZookeeper();
        String basePath = "/Myrpc-metadata";
        String providerPath = basePath + "/providers";
        String consumersPath = basePath + "/consumers";
        ZookeeperNode baseNode = new ZookeeperNode(basePath, null);
        ZookeeperNode providersNode = new ZookeeperNode(providerPath, null);
        ZookeeperNode consumersNode = new ZookeeperNode(consumersPath, null);

        List.of(baseNode, providersNode, consumersNode).forEach(node -> ZookeeperUtil.createNode(zooKeeper, node, null, CreateMode.PERSISTENT));

        ZookeeperUtil.close(zooKeeper);
    }
}
