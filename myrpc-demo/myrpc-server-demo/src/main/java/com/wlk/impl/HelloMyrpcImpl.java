package com.wlk.impl;

import com.wlk.HelloMyrpc;

public class HelloMyrpcImpl implements HelloMyrpc {
    @Override
    public String sayHi(String msg) {
        return "Hi! client"+msg;
    }
}
