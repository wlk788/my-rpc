package com.wlk;

import com.wlk.annotation.MyrpcApi;
import com.wlk.channelHandler.handler.MethodCallHandler;
import com.wlk.channelHandler.handler.MyRpcRequestDecoder;
import com.wlk.channelHandler.handler.MyRpcResponseEncoder;
import com.wlk.compress.CompressorFactory;
import com.wlk.config.Configuration;
import com.wlk.core.HeartbeatDetector;
import com.wlk.discovery.Registry;
import com.wlk.discovery.RegistryConfig;
import com.wlk.loadbalancer.LoadBalancer;
import com.wlk.loadbalancer.impl.ConsistentHashBalancer;
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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class MyrpcBootstrap {

    private static final MyrpcBootstrap myrpcBootstrap = new MyrpcBootstrap();

    private final Configuration configuration;
    // 保存request对象，可以到当前线程中随时获取
    public static final ThreadLocal<MyRpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    // 连接的缓存,如果使用InetSocketAddress这样的类做key，一定要看他有没有重写equals方法和toString方法
    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    public final static TreeMap<Long, Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();

    // 维护已经发布且暴露的服务列表 key-> interface的全限定名  value -> ServiceConfig
    public final static Map<String, ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

    // 定义全局的对外挂起的 completableFuture
    public final static Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);


    public MyrpcBootstrap() {
        configuration = new Configuration();
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
        configuration.setRegistryConfig(registryConfig);
        return this;
    }

    public MyrpcBootstrap loadBalancer(LoadBalancer loadBalancer) {
        configuration.setLoadBalancer(loadBalancer);
        return this;
    }

    /**
     * 封装需要发布的服务
     * @param serviceConfig
     * @return
     */
    public MyrpcBootstrap publish(ServiceConfig<?> serviceConfig) {
        configuration.getRegistryConfig().getRegistry().registry(serviceConfig);
        SERVERS_LIST.put(serviceConfig.getInterface().getName(), serviceConfig);
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
        reference.setRegistry(configuration.getRegistryConfig().getRegistry());
//        reference.setGroup(this.getConfiguration().getGroup());
        return this;
    }

    public MyrpcBootstrap serialize(String serializeString){
        byte serialize = SerializerFactory.SERIALIZER_CACHE_CODE.get(serializeString);
        this.serialize(serialize);
        return this;
    }

    public MyrpcBootstrap serialize(byte serialize){
        configuration.setSerializeType(serialize);
        if (log.isDebugEnabled()){
            log.debug("我们配置了使用的序列化的方式为【{}】.", serialize);
        }
        return this;
    }

    public MyrpcBootstrap compress(String compressString){
        byte compress = CompressorFactory.COMPRESSOR_CACHE_CODE.get(compressString);
        this.compress(compress);
        return this;
    }

    public MyrpcBootstrap compress(byte compress){
        configuration.setCompressType(compress);
        if (log.isDebugEnabled()){
            log.debug("我们配置了使用的压缩的方式为【{}】.", compress);
        }
        return this;
    }

    public MyrpcBootstrap scan(String packageName){
        List<String> classNames = getAllClassNames(packageName);
        //反射获取接口
        List<Class<?>> classes = classNames.stream().map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(clazz -> clazz.getAnnotation(MyrpcApi.class) != null)
                .collect(Collectors.toList());

        for (Class<?> clazz : classes) {
            Class<?>[] interfaces = clazz.getInterfaces();
            Object instance = null;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            MyrpcApi myrpcApi = clazz.getAnnotation(MyrpcApi.class);
            String group = myrpcApi.group();

            for (Class<?> anInterface : interfaces) {
                ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterface(anInterface);
                serviceConfig.setRef(instance);
                serviceConfig.setGroup(group);
                if (log.isDebugEnabled()){
                    log.debug("---->已经通过包扫描，将服务【{}】发布.",anInterface);
                }
                // 3、发布
                publish(serviceConfig);
            }
        }


        return this;
    }

    private List<String> getAllClassNames(String packageName) {
        String basePath = packageName.replaceAll("\\.", "/");
        URL url = ClassLoader.getSystemClassLoader().getResource(basePath);
        if (url == null){
            throw new RuntimeException("包扫描时，发现路径不存在.");
        }
        String absolutePath = url.getPath();
        List<String> classNames = new ArrayList<>();
        classNames = recursionFile(absolutePath,classNames,basePath);
        return classNames;
    }

    private List<String> recursionFile(String absolutePath, List<String> classNames, String basePath) {
        File file = new File(absolutePath);
        if (file.isDirectory()){
            File[] files = file.listFiles(pathname -> pathname.isDirectory() || pathname.getPath().contains(".class"));
            for (File child : files) {
                if (child.isDirectory()){
                    recursionFile(child.getAbsolutePath(), classNames, basePath);
                }
                else {
                    String className = getClassNameByAbsolutePath(child.getAbsolutePath(), basePath);
                    classNames.add(className);
                }
            }
        }
        else {
            String className = getClassNameByAbsolutePath(absolutePath, basePath);
            classNames.add(className);
        }
        return classNames;
    }

    private String getClassNameByAbsolutePath(String absolutePath, String basePath) {
        String fileName = absolutePath.substring(absolutePath.indexOf(basePath.replaceAll("/", "\\\\"))).replaceAll("\\\\", ".");
        return fileName.substring(0, fileName.indexOf(".class"));
    }

    public Configuration getConfiguration() {
        return configuration;
    }



    public static void main(String[] args) {
        MyrpcBootstrap.getInstance().getAllClassNames("com.wlk");
    }
}
