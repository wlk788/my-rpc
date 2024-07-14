package com.wlk.channelHandler.handler;

import com.wlk.compress.Compressor;
import com.wlk.compress.CompressorFactory;
import com.wlk.serialize.Serializer;
import com.wlk.serialize.SerializerFactory;
import com.wlk.transport.message.MessageFormatConstant;
import com.wlk.transport.message.MyRpcRequest;
import com.wlk.transport.message.MyRpcResponse;
import com.wlk.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * 自定义协议编码器
 * <p>
 * <pre>
 *   0    1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18   19   20   21   22
 *   +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
 *   |    magic          |ver |head  len|    full length    |code  ser|comp|              RequestId                |
 *   +-----+-----+-------+----+----+----+----+-----------+----- ---+--------+----+----+----+----+----+----+---+---+
 *   |                                                                                                             |
 *   |                                         body                                                                |
 *   |                                                                                                             |
 *   +--------------------------------------------------------------------------------------------------------+---+
 * </pre>
 *
 * 4B magic(魔数)   --->yrpc.getBytes()
 * 1B version(版本)   ----> 1
 * 2B header length 首部的长度
 * 4B full length 报文总长度
 * 1B serialize
 * 1B compress
 * 1B requestType
 * 8B requestId
 *
 * body
 */
@Slf4j
public class MyRpcResponseEncoder extends MessageToByteEncoder<MyRpcResponse> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MyRpcResponse myRpcResponse, ByteBuf byteBuf) throws Exception {
        // 4个字节的魔数
        byteBuf.writeBytes(MessageFormatConstant.MAGIC);
        // 1个字节的版本号
        byteBuf.writeByte(MessageFormatConstant.VERSION);
        // 2个字节的头部长度
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);
        // 先空出总长度(包含body)
        byteBuf.writerIndex(byteBuf.writerIndex()+MessageFormatConstant.FULL_FIELD_LENGTH);

        // 1个字节的请求类型
        byteBuf.writeByte(myRpcResponse.getCode());
        // 1个字节的序列化类型
        byteBuf.writeByte(myRpcResponse.getSerializeType());
        // 1个字节的压缩方式
        byteBuf.writeByte(myRpcResponse.getCompressType());

        // 8个字节的请求id
        byteBuf.writeLong(myRpcResponse.getRequestId());
        byteBuf.writeLong(myRpcResponse.getTimeStamp());
        //序列化+压缩
        // 1、对响应做序列化
        byte[] body = null;
        if(myRpcResponse.getBody() != null) {
            Serializer serializer = SerializerFactory
                    .getSerializer(myRpcResponse.getSerializeType()).getSerializer();
            body = serializer.serialize(myRpcResponse.getBody());

            // 2、压缩
            Compressor compressor = CompressorFactory.getCompressor(
                    myRpcResponse.getCompressType()
            ).getCompressor();
            body = compressor.compress(body);
        }
        //写入请求体
        if(body != null){
            byteBuf.writeBytes(body);
        }
        int bodyLength = body == null ? 0 : body.length;

        //回头写报文长度
        int writerIndex = byteBuf.writerIndex();
        byteBuf.writerIndex(MessageFormatConstant.MAGIC.length
                + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH);
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + bodyLength);

        byteBuf.writerIndex(writerIndex);

        if(log.isDebugEnabled()){
            log.debug("响应【{}】已经在服务端完成编码工作。",myRpcResponse.getRequestId());
        }
    }

}
