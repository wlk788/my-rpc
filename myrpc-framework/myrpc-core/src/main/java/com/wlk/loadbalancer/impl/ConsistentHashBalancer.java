package com.wlk.loadbalancer.impl;

import com.wlk.MyrpcBootstrap;
import com.wlk.loadbalancer.AbstractLoadBalancer;
import com.wlk.loadbalancer.Selector;
import com.wlk.transport.message.MyRpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Slf4j
public class ConsistentHashBalancer extends AbstractLoadBalancer {
    @Override
    public Selector getSelector(List<InetSocketAddress> serviceList) {
        return new ConsistentHashSelector(serviceList, 128);
    }

    private class ConsistentHashSelector implements Selector {
        private SortedMap<Integer, InetSocketAddress> circle = new TreeMap<>();

        private int virtualNodes;

        public ConsistentHashSelector(List<InetSocketAddress> serviceList, int virtualNodes) {
            this.virtualNodes = virtualNodes;
            for (InetSocketAddress inetSocketAddress : serviceList) {
                addNodeToCircle(inetSocketAddress);
            }
        }

        @Override
        public InetSocketAddress getNext() {
            MyRpcRequest myRpcRequest = MyrpcBootstrap.REQUEST_THREAD_LOCAL.get();
            String requestId = Long.toString(myRpcRequest.getRequestId());

            int hash = hash(requestId);

            if (!circle.containsKey(hash)) {
                SortedMap<Integer, InetSocketAddress> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }
            return circle.get(hash);
        }

        private void addNodeToCircle(InetSocketAddress inetSocketAddress) {
            for (int i = 0; i < virtualNodes; i++) {
                int hash = hash(inetSocketAddress.toString()+"-" + i);
                circle.put(hash, inetSocketAddress);
                if (log.isDebugEnabled()){
                    log.debug("hash为【{}】的节点已经挂载到了哈希环上", hash);
                }
            }
        }

        private int hash(String s){
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException();
            }
            byte[] digest = md.digest(s.getBytes());

            int res = 0;
            for (int i = 0; i < 4; i++) {
                res = res << 8;
                if (digest[i] < 0){
                    res = res | (digest[i] & 255);
                }
                else {
                    res = res | digest[i];
                }
            }
            return res;
        }

        private String toBinary(int i){
            String s = Integer.toBinaryString(i);
            int index = 32 - s.length();
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < index; j++) {
                sb.append(0);
            }
            sb.append(s);
            return sb.toString();
        }
    }
}
