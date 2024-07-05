package com.wlk.channelHandler;

import com.wlk.channelHandler.handler.MyRpcRequestEncoder;
import com.wlk.channelHandler.handler.MyRpcResponseDecoder;
import com.wlk.channelHandler.handler.MySimpleChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                .addLast(new MyRpcRequestEncoder())
                .addLast(new MyRpcResponseDecoder())
                .addLast(new MySimpleChannelInboundHandler());
    }
}
