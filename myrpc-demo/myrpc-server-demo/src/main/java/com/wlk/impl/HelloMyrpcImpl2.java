package com.wlk.impl;

import com.wlk.HelloMyrpc;
import com.wlk.annotation.MyrpcApi;

@MyrpcApi
public class HelloMyrpcImpl2 implements HelloMyrpc {
    @Override
    public String sayHi(String msg) {
        return "i am 2" + msg;
    }
}
