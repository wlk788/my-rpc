package com.wlk.proxy.handler;

import com.wlk.IdGenerator;
import com.wlk.MyrpcBootstrap;
import com.wlk.NettyBootstrapInitializer;
import com.wlk.discovery.Registry;
import com.wlk.enumeration.RequestType;
import com.wlk.exceptions.DiscoveryException;
import com.wlk.transport.message.MyRpcRequest;
import com.wlk.transport.message.RequestPayload;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.wlk.MyrpcBootstrap.PENDING_REQUEST;

@Slf4j
public class RpcConsumerInvocationHandler implements InvocationHandler {
    private Registry registry;

    private Class<?> interfaceRef;
    // 分组信息
    private String group;

    public RpcConsumerInvocationHandler(Registry registry, Class<?> interfaceRef,String group) {
        this.registry = registry;
        this.interfaceRef = interfaceRef;
        this.group = group;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        /*
         * ------------------ 1、封装报文 ---------------------------
         */
        /*
         * ------------------ 2、将请求存入本地线程，需要在合适的时候remove ---------------------------
         */
        /*
         * ------------------ 3、发现服务，从注册中心拉取服务列表，并通过客户端负载均衡寻找一个可用的服务 ---------------------------
         */
        System.out.println("hello proxy");
        List<InetSocketAddress> addresses = registry.lookup(interfaceRef.getName(), group);

        //TODO 处理连接列表，可能使用负载均衡
        InetSocketAddress address = addresses.get(0);
        if(log.isDebugEnabled()){
            log.debug("服务调用方，发现了服务【{}】的可用主机【{}】", interfaceRef.getName(), address);
        }
        /*
         * ------------------ 4、获取当前地址所对应的断路器，如果断路器是打开的则不发送请求，抛出异常 ---------------------------
         */
        /*
         * ------------------ 5、尝试获取一个可用通道 ---------------------------
         */
        Channel channel = getAvaiableChannel(address);

        /*
         * ------------------ 6、写出报文 ---------------------------
         */
        //使用异步策略获取结果
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();

        //TODO 将CompletableFuture暴露出去
        PENDING_REQUEST.put(1L, completableFuture);

        //TODO 封装报文
        RequestPayload requestPayload = RequestPayload.builder()
                .interfaceName(interfaceRef.getName())
                .methodName(method.getName())
                .parametersType(method.getParameterTypes())
                .parametersValue(args)
                .returnType(method.getReturnType())
                .build();
        MyRpcRequest myRpcRequest = MyRpcRequest.builder()
                .requestId(MyrpcBootstrap.idGenerator.getId())
                .compressType(MyrpcBootstrap.compressType)
                .serializeType(MyrpcBootstrap.serializeType)
                .requestType(RequestType.REQUEST.getId())
                .requestPayload(requestPayload)
                .build();
        //写出
        channel.writeAndFlush(myRpcRequest).addListener(
                (ChannelFutureListener)promise ->{
                    // 只需要处理以下异常就行了
                    if (!promise.isSuccess()) {
                        completableFuture.completeExceptionally(promise.cause());
                    }
                });

        return completableFuture.get(3, TimeUnit.SECONDS);
    }

    private Channel getAvaiableChannel(InetSocketAddress address) {
        Channel channel = MyrpcBootstrap.CHANNEL_CACHE.get(address);
        if(channel == null){
            //尝试使用异步获取
            CompletableFuture<Channel> channelFuture = new CompletableFuture<>();
            NettyBootstrapInitializer.getBootstrap().connect(address).addListener(
                    (ChannelFutureListener) promise -> {
                        if(promise.isDone()){
                            if(log.isDebugEnabled()){
                                log.debug("已经和【{}】建立连接",address);
                            }
                            channelFuture.complete(promise.channel());
                        }
                        else if (!promise.isSuccess()){
                            channelFuture.completeExceptionally(promise.cause());
                        }
                    }
            );
            try {
                channel = channelFuture.get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("获取通道时，发生异常。", e);
                throw new DiscoveryException(e);
            }
            MyrpcBootstrap.CHANNEL_CACHE.put(address, channel);
        }
        if(channel == null){
            log.error("获取或建立与【{}】的通道时发生了异常。", address);
            throw new RuntimeException("获取通道时发生了异常");
        }
        return channel;
    }
}
