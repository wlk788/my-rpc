package com.wlk.channelHandler.handler;

import com.wlk.compress.Compressor;
import com.wlk.compress.CompressorFactory;
import com.wlk.enumeration.RequestType;
import com.wlk.serialize.Serializer;
import com.wlk.serialize.SerializerFactory;
import com.wlk.transport.message.MessageFormatConstant;
import com.wlk.transport.message.MyRpcRequest;
import com.wlk.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;

@Slf4j
public class MyRpcRequestDecoder extends LengthFieldBasedFrameDecoder {
    public MyRpcRequestDecoder() {
        super(
                // 找到当前报文的总长度，截取报文，截取出来的报文我们可以去进行解析
                // 最大帧的长度，超过这个maxFrameLength值会直接丢弃
                MessageFormatConstant.MAX_FRAME_LENGTH,
                // 长度的字段的偏移量，
                MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH,
                // 长度的字段的长度
                MessageFormatConstant.FULL_FIELD_LENGTH,
                // todo 负载的适配长度
                -(MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH + MessageFormatConstant.FULL_FIELD_LENGTH),
                0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
//        Thread.sleep(new Random().nextInt(50));

        Object decode = super.decode(ctx, in);
        if(decode instanceof ByteBuf){
            //使用jdk8的语法，避免jdk16
            ByteBuf byteBuf = (ByteBuf) decode;
            return decodeFrame(byteBuf);
        }
        return null;
    }

    private Object decodeFrame(ByteBuf byteBuf) {
        //1、解析魔数
        byte[] magic = new byte[MessageFormatConstant.MAGIC.length];
        byteBuf.readBytes(magic);
        for (int i = 0; i < magic.length; i++) {
            if(magic[i] != MessageFormatConstant.MAGIC[i]){
                throw new RuntimeException("魔数不合法");
            }
        }
        //2、解析版本号
        byte version = byteBuf.readByte();
        if(version > MessageFormatConstant.VERSION){
            throw new RuntimeException("获得的请求版本不被支持");
        }
        //3、解析头部长度
        short headLength = byteBuf.readShort();
        //4、解析总长度
        int fullLength = byteBuf.readInt();
        //5、获取请求类型
        byte requestType = byteBuf.readByte();
        //6、获取序列化类型
        byte serializeType = byteBuf.readByte();
        //7、获取压缩类型
        byte compressType = byteBuf.readByte();
        //8、获取请求id
        long requetId = byteBuf.readLong();
        //封装
        MyRpcRequest myRpcRequest = MyRpcRequest.builder()
                .requestType(requestType)
                .serializeType(serializeType)
                .compressType(compressType)
                .requestId(requetId)
                .build();
        //心跳请求直接返回
        if(requestType == RequestType.HEART_BEAT.getId()){
            return myRpcRequest;
        }
        //解析请求体，解压缩，反序列化
        int payloadLength = fullLength - headLength;
        byte[] payload = new byte[payloadLength];
        byteBuf.readBytes(payload);

        if (payload != null && payload.length != 0){
            //压缩方式
            Compressor compressor = CompressorFactory.getCompressor(compressType).getCompressor();
            payload = compressor.decompress(payload);
            //反序列化
            Serializer serializer = SerializerFactory.getSerializer(serializeType).getSerializer();
            RequestPayload requestPayload = serializer.deserialize(payload, RequestPayload.class);
            myRpcRequest.setRequestPayload(requestPayload);
        }
        if(log.isDebugEnabled()){
            log.debug("响应【{}】已经在服务端完成解码工作。",myRpcRequest.getRequestId());
        }
        return myRpcRequest;
    }
}
