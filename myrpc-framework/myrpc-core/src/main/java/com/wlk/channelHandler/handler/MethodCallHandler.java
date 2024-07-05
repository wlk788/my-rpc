package com.wlk.channelHandler.handler;

import com.wlk.MyrpcBootstrap;
import com.wlk.ServiceConfig;
import com.wlk.enumeration.RespCode;
import com.wlk.transport.message.MyRpcRequest;
import com.wlk.transport.message.MyRpcResponse;
import com.wlk.transport.message.RequestPayload;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<MyRpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MyRpcRequest myRpcRequest) throws Exception {
        //1、先获取负载内容
        RequestPayload requestPayload = myRpcRequest.getRequestPayload();

        //2、根据负载内容进行方法调用
        Object result = callTargetMethod(requestPayload);
        if (log.isDebugEnabled()){
            log.debug("请求【{}】已经在服务端完成方法调用", myRpcRequest.getRequestId());
        }
        MyRpcResponse myRpcResponse = MyRpcResponse.builder()
                .code(RespCode.SUCCESS.getCode())
                .requestId(myRpcRequest.getRequestId())
                .serializeType(myRpcRequest.getSerializeType())
                .compressType(myRpcRequest.getCompressType())
                .body(result)
                .build();
        channelHandlerContext.channel().writeAndFlush(myRpcResponse);
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
