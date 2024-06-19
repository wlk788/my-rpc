package com.wlk;

import com.wlk.netty.AppClientHello;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class NettyTest {

    @Test
    public void testCompositeByteBuf(){
        ByteBuf header = Unpooled.buffer();
        ByteBuf body = Unpooled.buffer();
        CompositeByteBuf httpBuf = Unpooled.compositeBuffer();
        httpBuf.addComponents(header, body);
    }

    @Test
    public void testWrapper(){
        byte[] bytes = new byte[1024];
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
    }

    @Test
    public void testSlice(){
        ByteBuf byteBuf = Unpooled.buffer();
        ByteBuf header = byteBuf.slice(0, 5);
        ByteBuf body = byteBuf.slice(5, 10);
    }

    @Test
    public void testMessage() throws IOException {
        ByteBuf message = Unpooled.buffer();
        message.writeBytes("my".getBytes(StandardCharsets.UTF_8));
        message.writeByte(1);
        message.writeShort(125);
        message.writeInt(256);
        message.writeByte(1);
        message.writeByte(0);
        message.writeByte(2);
        message.writeLong(251455L);

        AppClientHello appClientHello = new AppClientHello("127.0.0.1", 18080);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(appClientHello);
        byte[] bytes = outputStream.toByteArray();
        message.writeBytes(bytes);
        printAsBinary(message);
        System.out.println(message);
    }

    public static void printAsBinary(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(byteBuf.readerIndex(), bytes);
        String binaryString = ByteBufUtil.hexDump(bytes);
        StringBuilder formattedBinary = new StringBuilder();
        for (int i = 0; i < binaryString.length(); i += 2) {
            formattedBinary.append(binaryString.substring(i, i + 2)).append(" ");
        }
        System.out.println("Binary representation: " + formattedBinary.toString());
    }
}
