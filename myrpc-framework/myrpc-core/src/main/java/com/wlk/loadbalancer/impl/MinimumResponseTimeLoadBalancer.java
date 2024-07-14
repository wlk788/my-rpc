package com.wlk.loadbalancer.impl;

import com.wlk.MyrpcBootstrap;
import com.wlk.loadbalancer.AbstractLoadBalancer;
import com.wlk.loadbalancer.Selector;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class MinimumResponseTimeLoadBalancer extends AbstractLoadBalancer {
    @Override
    public Selector getSelector(List<InetSocketAddress> serviceList) {
        return new MinimumResponseTimeSelector(serviceList);
    }

    private class MinimumResponseTimeSelector implements Selector {
        public MinimumResponseTimeSelector(List<InetSocketAddress> serviceList) {
        }

        @Override
        public InetSocketAddress getNext() {
            Map.Entry<Long, Channel> entry = MyrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry();
            if (entry != null) {
                if (log.isDebugEnabled()){
                    log.debug("选取了响应时间为【{}ms】的服务节点.",entry.getKey());
                }
                return (InetSocketAddress) entry.getValue().remoteAddress();
            }

            // 直接从缓存中获取一个可用的就行了
            System.out.println("----->"+ Arrays.toString(MyrpcBootstrap.CHANNEL_CACHE.values().toArray()));
            Channel channel = (Channel)MyrpcBootstrap.CHANNEL_CACHE.values().toArray()[0];
            return (InetSocketAddress)channel.remoteAddress();
        }
    }
}
