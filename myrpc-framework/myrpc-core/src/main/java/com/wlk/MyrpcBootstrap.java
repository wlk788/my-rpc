package com.wlk;

import com.wlk.channelHandler.handler.MethodCallHandler;
import com.wlk.channelHandler.handler.MyRpcRequestDecoder;
import com.wlk.channelHandler.handler.MyRpcResponseEncoder;
import com.wlk.compress.CompressorFactory;
import com.wlk.core.HeartbeatDetector;
import com.wlk.discovery.Registry;
import com.wlk.discovery.RegistryConfig;
import com.wlk.loadbalancer.LoadBalancer;
import com.wlk.loadbalancer.impl.ConsistentHashBalancer;
import com.wlk.loadbalancer.impl.RoundRobinLoadBalancer;
import com.wlk.serialize.SerializerFactory;
import com.wlk.transport.message.MyRpcRequest;
import com.wlk.utils.zookeeper.ZookeeperUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Id;

import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MyrpcBootstrap {

    private static MyrpcBootstrap myrpcBootstrap = new MyrpcBootstrap();

    private String applicationName = "default";
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;

    private Registry registry;
    private ZooKeeper zookeeper;

    // 保存request对象，可以到当前线程中随时获取
    public static final ThreadLocal<MyRpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    // 连接的缓存,如果使用InetSocketAddress这样的类做key，一定要看他有没有重写equals方法和toString方法
    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    public final static TreeMap<Long, Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();

    // 维护已经发布且暴露的服务列表 key-> interface的全限定名  value -> ServiceConfig
    public final static Map<String, ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

    // 定义全局的对外挂起的 completableFuture
    public final static Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);

    //Id生成器
    public final static IdGenerator idGenerator = new IdGenerator(1, 2);

    public static LoadBalancer loadBalancer;

    public static byte serializeType = (byte) 1;
    public static byte compressType = (byte) 1;

    public MyrpcBootstrap() {
        zookeeper = ZookeeperUtils.createZookeeper();
    }

    public static MyrpcBootstrap getInstance(){
        return myrpcBootstrap;
    }

    public MyrpcBootstrap application() {
        return this;
    }

    /**
     * 注册中心
     * @param registryConfig
     * @return
     */
    public MyrpcBootstrap registry(RegistryConfig registryConfig){
        this.registryConfig = registryConfig;
        this.registry = registryConfig.getRegistry();
        return this;
    }

    /**
     * 序列化协议
     * @param protocolConfig
     * @return
     */
    public MyrpcBootstrap protocol(ProtocolConfig protocolConfig){
        return this;
    }

    /**
     * 封装需要发布的服务
     * @param service
     * @return
     */
    public MyrpcBootstrap publish(ServiceConfig<?> service) {
        registry.registry(service);
        SERVERS_LIST.put(service.getInterface().getName(), service);
        return this;
    }

    /**
     * 批量发布服务
     * @param services
     * @return
     */
    public MyrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for(ServiceConfig<?> service : services){
            this.publish(service);
        }
        return this;
    }

    public void start(){
        // 注册关闭应用程序的钩子函数
//        Runtime.getRuntime().addShutdownHook(new MyrpcShutdownHook());

        // 1、创建eventLoop，老板只负责处理请求，之后会将请求分发至worker
        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup(10);
        try {

            // 2、需要一个服务器引导程序
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 3、配置服务器
            serverBootstrap = serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 是核心，我们需要添加很多入站和出站的handler
                            socketChannel.pipeline()
                                    .addLast(new LoggingHandler())
                                    .addLast(new MyRpcRequestDecoder())
                                    // 根据请求进行方法调用
                                    .addLast(new MethodCallHandler())
                                    .addLast(new MyRpcResponseEncoder())
                            ;
                        }
                    });

            // 4、绑定端口
            ChannelFuture channelFuture = serverBootstrap.bind(8088).sync();

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                boss.shutdownGracefully().sync();
                worker.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public MyrpcBootstrap reference(ReferenceConfig<?> reference) {
        // 开启对这个服务的心跳检测
        HeartbeatDetector.detectHeartbeat(reference.getInterface().getName());
        //在这个方法里我们是否可以拿到相关的配置项-注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        reference.setRegistry(registry);
        loadBalancer = new ConsistentHashBalancer();
        return this;
    }

    public MyrpcBootstrap serialize(String serializeString){
        byte serialize = SerializerFactory.SERIALIZER_CACHE_CODE.get(serializeString);
        this.serialize(serialize);
        return this;
    }

    public MyrpcBootstrap serialize(byte serialize){
        serializeType = serialize;
        if (log.isDebugEnabled()){
            log.debug("我们配置了使用的序列化的方式为【{}】.", serializeType);
        }
        return this;
    }

    public MyrpcBootstrap compress(String compressString){
        byte compress = CompressorFactory.COMPRESSOR_CACHE_CODE.get(compressString);
        this.compress(compress);
        return this;
    }

    public MyrpcBootstrap compress(byte compress){
        compressType = compress;
        if (log.isDebugEnabled()){
            log.debug("我们配置了使用的压缩的方式为【{}】.", serializeType);
        }
        return this;
    }

    public Registry getRegistry() {
        return registry;
    }
}
