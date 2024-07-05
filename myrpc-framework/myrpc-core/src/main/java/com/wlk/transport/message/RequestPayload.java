package com.wlk.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestPayload implements Serializable {
    //1、接口的名字
    private String interfaceName;

    //2、方法的名字
    private String methodName;

    //3、参数列表
    private Class<?>[] parametersType;
    private Object[] parametersValue;

    //4、返回值的封装
    private Class<?> returnType;
}
