package com.song.moja.zk;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ServiceProvider {

    private static Logger LOG = Logger.getLogger(ServiceProvider.class);
    // 用于等待 SyncConnected 事件触发后继续执行当前线�?
    private CountDownLatch latch = new CountDownLatch(1);

    // 发布ServerSocket地址�?ZooKeeper �?
    public void publish(String host, int port) {
        String url = publishService(host, port); // 发布 ServerSocket 服务并返�?ServerSocket 地址
        if (url != null) {
            ZooKeeper zk = connectServer(); // 连接 ZooKeeper 服务器并获取 ZooKeeper 对象
            if (zk != null) {
                createNode(zk, url); // 创建 ZNode 并将 ServerSocket 地址放入 ZNode �?
            }
        }
    }

    // 发布 RMI 服务
    private String publishService(String host, int port) {
        String url = null;
        try {
        	 url = String.format("%s,%d", host, port);
        	 System.out.println("格式化服务器地址"+url);
        } catch (Exception e) {
        	e.printStackTrace();
        	LOG.error("", e);
        }
        return url;
    }

    // 连接 ZooKeeper 服务�?
    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(ZkConstant.ZK_CONNECTION_STRING, ZkConstant.ZK_SESSION_TIMEOUT, new Watcher() {
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown(); // 唤醒当前正在执行的线�?
                    }
                }
            });
            latch.await(); // 使当前线程处于等待状�?
        } catch (IOException e1 ) {
        	e1.printStackTrace();
        	LOG.error("", e1);
        }catch(InterruptedException e){
        	e.printStackTrace();
        }
        return zk;
    }

    // 创建 ZNode
    private void createNode(ZooKeeper zk, String url) {
        try {
            byte[] data = url.getBytes();
            String path = zk.create(ZkConstant.ZK_PROVIDER_PATH, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL); // 创建�?��临时性且有序�?ZNode
            LOG.debug("create zookeeper node ({} => {})"+ path+ url);
        } catch (KeeperException e1) {
        	e1.printStackTrace();
        	LOG.error("", e1);
        }catch(InterruptedException e2){
        	e2.printStackTrace();
        	LOG.error("", e2);
        }
    }
}