package com.zym;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class BalanceServer {

    private static final String SERVER_PATH = "/servers";

    private static class SingletonHolder {

        private static final BalanceServer instance = new BalanceServer();

        private static final CuratorFramework client;

        private static final List<String> serverList = new CopyOnWriteArrayList<>();

        static {
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);

            client = CuratorFrameworkFactory.builder()
                    .connectString("192.168.31.240:2181")
                    .sessionTimeoutMs(20000)  // 会话超时时间
                    .connectionTimeoutMs(20000) // 连接超时时间
                    .retryPolicy(retryPolicy)
                    .namespace("") // 包含隔离名称
                    .build();

            client.start();

            try {
                client.create().withMode(CreateMode.PERSISTENT).forPath(SERVER_PATH, "user-service".getBytes());

                CuratorCache curatorCache = CuratorCache.builder(client, SERVER_PATH).build();

                CuratorCacheListener listener = CuratorCacheListener.builder().forPathChildrenCache(SERVER_PATH, client, (client, event) -> {
                    log.info("事件：" + event);
                    switch (event.getType()) {
                        case CHILD_ADDED: {
                            serverList.add(event.getData().getPath().replaceAll(SERVER_PATH + "/", ""));
                            break;
                        }
                        case CHILD_REMOVED: {
                            serverList.remove(event.getData().getPath());
                            break;
                        }
                    }
                    log.info("server list: " + serverList);
                }).build();

                curatorCache.listenable().addListener(listener);

                curatorCache.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private BalanceServer() {

    }

    public static BalanceServer getInstance() {
        return SingletonHolder.instance;
    }

    private CuratorFramework getClient() {
        return SingletonHolder.client;
    }

    private List<String> getServerList() {
        return SingletonHolder.serverList;
    }

    public void register(int port) throws Exception {
        InetAddress address = InetAddress.getLocalHost();
        String serverIp = address.getHostAddress();
        getClient().create().withMode(CreateMode.EPHEMERAL).forPath(SERVER_PATH + "/" + serverIp + ":" + port, "0".getBytes());
    }

    public void unRegister(String port) throws Exception {
        InetAddress address = InetAddress.getLocalHost();
        String serverIp = address.getHostAddress();
        getClient().delete().forPath(SERVER_PATH + "/" + serverIp + ":" + port);
    }

    private final AtomicInteger reqCount = new AtomicInteger(0);

    public String getServer() {
        int req = reqCount.incrementAndGet();
        int mod = req % getServerList().size();
        return getServerList().get(mod);
    }

}
