package com.zym;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcTest {

    public static void main(String[] args) throws Exception {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                5,
                10,
                1000,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadPoolExecutor.AbortPolicy());
        CountDownLatch latch = new CountDownLatch(2);
        for (String s : args) {
            threadPoolExecutor.submit(new RpcServer(Integer.parseInt(s), latch));
        }
        Thread.sleep(20000);
        //查看 rpc url判断是否是轮询
        RpcClient rpcClient = new RpcClient();
        for (int i = 0; i < 3; i++) {
            rpcClient.run();
        }
        latch.await();
    }

}
