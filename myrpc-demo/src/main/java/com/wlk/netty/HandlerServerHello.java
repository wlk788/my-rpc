package com.wlk.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

@ChannelHandler.Sharable
public class HandlerServerHello extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //处理接受的数据
        ByteBuf in = (ByteBuf) msg;
        System.out.println("收到客户端信息："+in.toString(CharsetUtil.UTF_8));
        //写入发送到客户端
        ctx.writeAndFlush(Unpooled.copiedBuffer("服务端以接受客户端信息", CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
