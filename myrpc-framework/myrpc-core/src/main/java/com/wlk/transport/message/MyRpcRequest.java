package com.wlk.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class MyRpcRequest {

    //请求id
    private long requestId;

    //请求的类型，压缩方式，序列化方式
    private byte requestType;
    private byte compressType;
    private byte serializeType;
//
//    private long timeStamp;

    //具体的消息体
    private RequestPayload requestPayload;
}
