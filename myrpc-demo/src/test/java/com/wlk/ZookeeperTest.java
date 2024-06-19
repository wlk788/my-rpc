package com.wlk;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ZookeeperTest {

    ZooKeeper zooKeeper;

    @Before
    public void createZk(){
        //定义连接参数
        String connectString = "127.0.0.1:2181";
        //超时时间
        int timeout = 10000;
        try {
            zooKeeper = new ZooKeeper(connectString, timeout, new MyWatcher());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCreatePNode(){
        String result = null;
        try {
            result = zooKeeper.create("/myclass", "hello".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.println("result=" + result);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if(zooKeeper != null){
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testDeletePNode(){
        try {
            zooKeeper.delete("/myclass", -1);
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        } finally {
            try {
                if(zooKeeper != null){
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testExistsPNode(){
        try {
            Stat stat = zooKeeper.exists("/myclass", null);
            zooKeeper.setData("/myclass","hi".getBytes(), -1);
            int version = stat.getVersion();
            System.out.println("version=" + version);
            int aversion = stat.getAversion();
            System.out.println("aversion=" + aversion);
            int cversion = stat.getCversion();
            System.out.println("cversion=" + cversion);

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }finally {
            try {
                if(zooKeeper != null){
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testWatcher(){
        try {
            // 以下三个方法可以注册watcher，可以直接new一个新的watcher，
            // 也可以使用true来选定默认的watcher
            zooKeeper.exists("/ydlclass", true);
            //            zooKeeper.getChildren();
            //            zooKeeper.getData();

            while(true){
                Thread.sleep(1000);
            }

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if(zooKeeper != null){
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
