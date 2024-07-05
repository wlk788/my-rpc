package com.wlk.channelHandler.handler;

import com.wlk.MyrpcBootstrap;
import com.wlk.transport.message.MyRpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<MyRpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MyRpcResponse myRpcResponse) throws Exception {
        Object result = myRpcResponse.getBody();
        CompletableFuture<Object> completableFuture = MyrpcBootstrap.PENDING_REQUEST.get(1L);
        completableFuture.complete(result);
    }
}
