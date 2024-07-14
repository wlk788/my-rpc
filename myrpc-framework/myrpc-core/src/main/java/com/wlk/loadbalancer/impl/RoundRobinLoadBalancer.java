package com.wlk.loadbalancer.impl;

import com.wlk.exceptions.LoadBalancerException;
import com.wlk.loadbalancer.AbstractLoadBalancer;
import com.wlk.loadbalancer.Selector;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {
    @Override
    public Selector getSelector(List<InetSocketAddress> serviceList) {
        return new RoundRobinSelector(serviceList);
    }

    private class RoundRobinSelector implements Selector {
        private List<InetSocketAddress> serviceList;
        private AtomicInteger index;

        public RoundRobinSelector(List<InetSocketAddress> serviceList) {
            this.serviceList = serviceList;
            this.index = new AtomicInteger(0);
        }

        @Override
        public InetSocketAddress getNext() {
            if (serviceList == null || serviceList.size() == 0){
                log.error("进行负载均衡选取节点时发现服务列表为空");
                throw new LoadBalancerException();
            }

            InetSocketAddress address = serviceList.get(index.get());

            if (index.get() == serviceList.size()-1){
                index.set(0);
            }
            else {
                index.incrementAndGet();
            }

            return address;
        }
    }
}
