package com.wlk.watch;

import com.wlk.MyrpcBootstrap;
import com.wlk.NettyBootstrapInitializer;
import com.wlk.discovery.Registry;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

@Slf4j
public class UpAndDownWatcher implements Watcher {
    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged){
            if (log.isDebugEnabled()){
                log.debug("检测到服务【{}】有节点上下线，将重新拉取服务列表", watchedEvent.getPath());
            }
            String serviceName = getServiceName(watchedEvent.getPath());
            Registry registry = MyrpcBootstrap.getInstance().getRegistry();
            List<InetSocketAddress> addresses = registry.lookup(serviceName);

            for (InetSocketAddress address : addresses) {
                if (!MyrpcBootstrap.CHANNEL_CACHE.containsKey(address)){
                    Channel channel = null;
                    try {
                        channel = NettyBootstrapInitializer.getBootstrap().connect(address).sync().channel();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    MyrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }
            }

            for (InetSocketAddress address : MyrpcBootstrap.CHANNEL_CACHE.keySet()) {
                if (!addresses.contains(address)){
                    MyrpcBootstrap.CHANNEL_CACHE.remove(address);
                }
            }

            // TODO 获得负载均衡器，进行重新的loadBalance
            MyrpcBootstrap.loadBalancer.reLoadBalance(serviceName, addresses);
        }
    }

    private String getServiceName(String path) {
        String[] split = path.split("/");
        return split[split.length - 1];
    }
}
