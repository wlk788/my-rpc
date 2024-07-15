package com.wlk.core;

import com.wlk.MyrpcBootstrap;
import com.wlk.NettyBootstrapInitializer;
import com.wlk.compress.CompressorFactory;
import com.wlk.discovery.Registry;
import com.wlk.enumeration.RequestType;
import com.wlk.serialize.SerializerFactory;
import com.wlk.transport.message.MyRpcRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class HeartbeatDetector {

    public static void detectHeartbeat(String serviceName){
        Registry registry = MyrpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry();
        List<InetSocketAddress> addresses = registry.lookup(serviceName);

        for (InetSocketAddress address : addresses) {
            try {
                if (!MyrpcBootstrap.CHANNEL_CACHE.containsKey(address)){
                    Channel channel = NettyBootstrapInitializer.getBootstrap().connect(address).sync().channel();
                    MyrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
        }

        Thread thread = new Thread(()->
                new Timer().scheduleAtFixedRate(new MyTimerTask(), 0, 2000), "myrpc-HeartbeatDetector-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private static class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            MyrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.clear();

            Map<InetSocketAddress, Channel> cache = MyrpcBootstrap.CHANNEL_CACHE;
            for (Map.Entry<InetSocketAddress, Channel> entry : cache.entrySet()) {
                int tryTimes = 3;
                while (tryTimes > 0){
                    Channel channel = entry.getValue();
                    long start = System.currentTimeMillis();

                    MyRpcRequest myRpcRequest = MyRpcRequest.builder()
                            .requestId(MyrpcBootstrap.getInstance().getConfiguration().idGenerator.getId())
                            .compressType(CompressorFactory.getCompressor(MyrpcBootstrap.getInstance().getConfiguration().getCompressType()).getCode())
                            .requestType(RequestType.HEART_BEAT.getId())
                            .serializeType(SerializerFactory.getSerializer(MyrpcBootstrap.getInstance().getConfiguration().getSerializeType()).getCode())
                            .timeStamp(start)
                            .build();

                    CompletableFuture<Object> completableFuture = new CompletableFuture<>();

                    MyrpcBootstrap.PENDING_REQUEST.put(myRpcRequest.getRequestId(), completableFuture);
                    channel.writeAndFlush(myRpcRequest).addListener((ChannelFutureListener) promise ->{
                        if (!promise.isSuccess()){
                            completableFuture.completeExceptionally(promise.cause());
                        }
                    });

                    long end = 0L;
                    try {
                        completableFuture.get(1, TimeUnit.SECONDS);
                        end = System.currentTimeMillis();
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        tryTimes--;
                        log.error("和地址为【{}】的主机连接发生异常.正在进行第【{}】次重试......",
                                channel.remoteAddress(), 3 - tryTimes);

                        if (tryTimes == 0){
                            MyrpcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                        }

                        try {
                            Thread.sleep(10*(new Random()).nextInt(5));
                        } catch (InterruptedException ex) {
                            throw new RuntimeException();
                        }
                        continue;
                    }
                    long time = end- start;

                    MyrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.put(time, channel);
                    log.debug("和[{}]服务器的响应时间是[{}].", entry.getKey(), time);
                    break;
                }
            }
            log.info("-----------------------响应时 " +
                    "间的treemap----------------------");
            for (Map.Entry<Long, Channel> entry : MyrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.entrySet()) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}]--->channelId:[{}]", entry.getKey(), entry.getValue().id());
                }
            }
        }
    }
}
