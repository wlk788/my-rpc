package com.wlk.serialize.impl;

import com.wlk.exceptions.SerializeException;
import com.wlk.serialize.Serializer;
import com.wlk.transport.message.RequestPayload;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class JdkSerializer implements Serializer {

    @Override
    public byte[] serialize(Object object) {
        if (object == null){
            return null;
        }
        try (
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(bos);
        ){
            objectOutputStream.writeObject(object);
            //压缩
            byte[] result = bos.toByteArray();
            if(log.isDebugEnabled()){
                log.debug("对象【{}】已经完成了序列化操作，序列化后的字节数为【{}】",object,result.length);
            }
            return result;
        } catch (IOException e) {
            log.error("序列化对象【{}】时放生异常.",object);
            throw new SerializeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null){
            return null;
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis);
        ){

            Object object= ois.readObject();
            if(log.isDebugEnabled()){
                log.debug("类【{}】已经完成了反序列化操作.",clazz);
            }
            return (T)object;
        } catch (IOException | ClassNotFoundException e) {
            log.error("反序列化对象【{}】时放生异常.",clazz);
            throw new SerializeException(e);
        }
    }
}
