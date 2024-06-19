package com.wlk.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class AppClientHello implements Serializable {
    private final String host;
    private final int port;

    public AppClientHello(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() throws Exception{
        //定义线程池
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bs = new Bootstrap();
            bs.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //添加Handler
                            socketChannel.pipeline().addLast(new HandlerClientHello());
                        }
                    });
            //连接远程节点
            ChannelFuture future = bs.connect().sync();
            //发送消息到服务端
            future.channel().writeAndFlush(Unpooled.copiedBuffer("Hello World", CharsetUtil.UTF_8));
            //阻塞操作
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public static void main(String[] args) throws Exception {
        new AppClientHello("127.0.0.1", 18080).run();
    }
}
