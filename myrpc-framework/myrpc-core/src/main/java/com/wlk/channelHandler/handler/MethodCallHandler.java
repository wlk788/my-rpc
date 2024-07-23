package com.wlk.channelHandler.handler;

import com.wlk.MyrpcBootstrap;
import com.wlk.ServiceConfig;
import com.wlk.core.ShutDownHolder;
import com.wlk.enumeration.RequestType;
import com.wlk.enumeration.RespCode;
import com.wlk.protection.RateLimiter;
import com.wlk.protection.TokenBuketRateLimiter;
import com.wlk.transport.message.MyRpcRequest;
import com.wlk.transport.message.MyRpcResponse;
import com.wlk.transport.message.RequestPayload;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Map;

@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<MyRpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MyRpcRequest myRpcRequest) throws Exception {
        // 1、先封装部分响应
        MyRpcResponse myRpcResponse = new MyRpcResponse();
        myRpcResponse.setRequestId(myRpcRequest.getRequestId());
        myRpcResponse.setCompressType(myRpcRequest.getCompressType());
        myRpcResponse.setSerializeType(myRpcRequest.getSerializeType());

        // 2、 获得通道
        Channel channel = channelHandlerContext.channel();

        // 3、查看关闭的挡板是否打开，如果挡板已经打开，返回一个错误的响应
        if( ShutDownHolder.BAFFLE.get() ){
            myRpcResponse.setCode(RespCode.BECOLSING.getCode());
            channel.writeAndFlush(myRpcResponse);
            return;
        }

        // 4、计数器加一
        ShutDownHolder.REQUEST_COUNTER.increment();

        // 4、完成限流相关的操作
        SocketAddress socketAddress = channel.remoteAddress();
        Map<SocketAddress, RateLimiter> everyIpRateLimiter =
                MyrpcBootstrap.getInstance().getConfiguration().getEveryIpRateLimiter();

        RateLimiter rateLimiter = everyIpRateLimiter.get(socketAddress);
        if (rateLimiter == null) {
            rateLimiter = new TokenBuketRateLimiter(10, 10);
            everyIpRateLimiter.put(socketAddress, rateLimiter);
        }
        boolean allowRequest = rateLimiter.allowRequest();

        // 5、处理请求的逻辑
        // 限流
        if (!allowRequest) {
            // 需要封装响应并且返回了
            myRpcResponse.setCode(RespCode.RATE_LIMIT.getCode());

            // 处理心跳
        } else if (myRpcRequest.getRequestType() == RequestType.HEART_BEAT.getId()) {
            // 需要封装响应并且返回
            myRpcResponse.setCode(RespCode.SUCCESS_HEART_BEAT.getCode());

            // 正常调用
        } else {
            /** ---------------具体的调用过程--------------**/
            // （1）获取负载内容
            RequestPayload requestPayload = myRpcRequest.getRequestPayload();

            // （2）根据负载内容进行方法调用
            try {
                Object result = callTargetMethod(requestPayload);
                if (log.isDebugEnabled()) {
                    log.debug("请求【{}】已经在服务端完成方法调用。", myRpcRequest.getRequestId());
                }
                // （3）封装响应   我们是否需要考虑另外一个问题，响应码，响应类型
                myRpcResponse.setCode(RespCode.SUCCESS.getCode());
                myRpcResponse.setBody(result);
            } catch (Exception e){
                log.error("编号为【{}】的请求在调用过程中发生异常。",myRpcRequest.getRequestId(),e);
                myRpcResponse.setCode(RespCode.FAIL.getCode());
            }
        }

        // 6、写出响应
        channel.writeAndFlush(myRpcResponse);

        // 7、计数器减一
        ShutDownHolder.REQUEST_COUNTER.decrement();
    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String interfaceName = requestPayload.getInterfaceName();
        String methodName = requestPayload.getMethodName();
        Class<?>[] parametersType = requestPayload.getParametersType();
        Object[] parametersValue = requestPayload.getParametersValue();

        //寻找匹配的暴露出去的具体实现
        ServiceConfig<?> serviceConfig = MyrpcBootstrap.SERVERS_LIST.get(interfaceName);
        Object ref = serviceConfig.getRef();

        //通过反射调用
        Object returnValue;
        try {
            Class<?> aClass = ref.getClass();
            Method method = aClass.getMethod(methodName, parametersType);
            returnValue = method.invoke(ref, parametersValue);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("调用服务【{}】的方法【{}】时发生了异常", interfaceName, methodName);
            throw new RuntimeException(e);
        }
        return returnValue;
    }
}
