package com.wlk;

import java.util.concurrent.atomic.LongAdder;

public class IdGenerator {

//    //这是单机版本的线程安全的id发号器，一旦变成集群状态，就不行了
//    private static LongAdder longAdder = new LongAdder();
//
//    public static long getId(){
//        longAdder.increment();
//        return longAdder.sum();
//    }

    public static final long START_STAMP = DateUtil.get("2022-1-1").getTime();
    //机房
    public static final long DATA_CENTER_BIT = 5L;
    //机器
    public static final long MACHINE_BIT = 5L;
    public static final long SEQUENCE_BIT = 12L;

    // 最大值 Math.pow(2,5) -1
    public static final long DATA_CENTER_MAX = ~(-1L << DATA_CENTER_BIT);
    public static final long MACHINE_MAX = ~(-1L << MACHINE_BIT);
    public static final long SEQUENCE_MAX = ~(-1L << SEQUENCE_BIT);


    // 时间戳 （42） 机房好 （5） 机器号 （5） 序列号 （12）
    // 101010101010101010101010101010101010101011 10101 10101 101011010101
    public static final long TIMESTAMP_LEFT = DATA_CENTER_BIT + MACHINE_BIT + SEQUENCE_BIT;
    public static final long DATA_CENTER_LEFT = MACHINE_BIT + SEQUENCE_BIT;
    public static final long MACHINE_LEFT = SEQUENCE_BIT;

    private long dataCenterId;
    private long machineId;
    private LongAdder sequenceId = new LongAdder();
    // 时钟回拨的问题，我们需要去处理
    private long lastTimeStamp = -1L;

    public IdGenerator(long dataCenterId, long machineId) {
        // 判断传世的参数是否合法
        if(dataCenterId > DATA_CENTER_MAX || machineId > MACHINE_MAX){
            throw new IllegalArgumentException("你传入的数据中心编号或机器号不合法.");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    public long getId(){
        //1、处理时间戳
        long currentTime = System.currentTimeMillis();
        long timeStamp = currentTime - START_STAMP;
        //2、判断时钟回拨
        if(timeStamp < lastTimeStamp){
            throw new RuntimeException("您的服务器进行了时钟回调");
        }

        //sequeceId进行自增
        if(timeStamp == lastTimeStamp){
            sequenceId.increment();
        }
        if (sequenceId.sum() >= SEQUENCE_MAX){
            timeStamp = getNextTimeStamp();
            sequenceId.reset();
        }
        else {
            sequenceId.reset();
        }

        //更新lastTimeStamp
        lastTimeStamp = timeStamp;
        long sequence = sequenceId.sum();
        return timeStamp << TIMESTAMP_LEFT | dataCenterId << DATA_CENTER_LEFT | machineId << MACHINE_LEFT | sequence;
    }

    private long getNextTimeStamp() {
        long current = System.currentTimeMillis()-START_STAMP;

        while (current == lastTimeStamp){
            current = System.currentTimeMillis()-START_STAMP;
        }
        return current;
    }
}
