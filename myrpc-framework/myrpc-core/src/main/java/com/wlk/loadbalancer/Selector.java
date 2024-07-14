package com.wlk.loadbalancer;

import java.net.InetSocketAddress;

public interface Selector {

    /**
     * 获得服务节点
     * @return 获得服务节点
     */
    public InetSocketAddress getNext();
}
