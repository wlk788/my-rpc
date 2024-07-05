package com.wlk.channelHandler.handler;

import com.wlk.serialize.Serializer;
import com.wlk.serialize.SerializerFactory;
import com.wlk.transport.message.MessageFormatConstant;
import com.wlk.transport.message.MyRpcRequest;
import com.wlk.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 *  * <pre>
 *  *   0    1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18   19   20   21   22
 *  *   +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
 *  *   |    magic          |ver |head  len|    full length    | qt | ser|comp|              RequestId                |
 *  *   +-----+-----+-------+----+----+----+----+-----------+----- ---+--------+----+----+----+----+----+----+---+---+
 *  *   |                                                                                                             |
 *  *   |                                         body                                                                |
 *  *   |                                                                                                             |
 *  *   +--------------------------------------------------------------------------------------------------------+---+
 *  * </pre>
 */
@Slf4j
public class MyRpcRequestEncoder extends MessageToByteEncoder<MyRpcRequest> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MyRpcRequest myRpcRequest, ByteBuf byteBuf) throws Exception {
        // 4个字节的魔数
        byteBuf.writeBytes(MessageFormatConstant.MAGIC);
        // 1个字节的版本号
        byteBuf.writeByte(MessageFormatConstant.VERSION);
        // 2个字节的头部长度
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);
        // 先空出总长度(包含body)
        byteBuf.writerIndex(byteBuf.writerIndex()+MessageFormatConstant.FULL_FIELD_LENGTH);

        // 1个字节的请求类型
        byteBuf.writeByte(myRpcRequest.getRequestType());
        // 1个字节的序列化类型
        byteBuf.writeByte(myRpcRequest.getSerializeType());
        // 1个字节的压缩方式
        byteBuf.writeByte(myRpcRequest.getCompressType());

        // 8个字节的请求id
        byteBuf.writeLong(myRpcRequest.getRequestId());

        //序列化+压缩
        //TODO 压缩方式
        Serializer serializer = SerializerFactory.getSerializer(myRpcRequest.getSerializeType()).getSerializer();
        byte[] body = serializer.serialize(myRpcRequest.getRequestPayload());
        //写入请求体
        if(body != null){
            byteBuf.writeBytes(body);
        }
        int bodyLength = body == null ? 0 : body.length;

        //回头写报文长度
        int writerIndex = byteBuf.writerIndex();
        byteBuf.writerIndex(7);
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + bodyLength);

        byteBuf.writerIndex(writerIndex);

        if(log.isDebugEnabled()){
            log.debug("响应【{}】已经在调用端完成编码工作。",myRpcRequest.getRequestId());
        }
    }
}
