package com.cw.chat;

import com.alibaba.fastjson.JSON;
import com.cw.tcp.RecieveFileThread;
import com.cw.tcp.SendFileThread;
import com.cw.utils.ActionEnum;
import com.cw.utils.ConsoleEnum;
import com.cw.utils.Utils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.*;

import static com.cw.udp.SendUDP.send;

@Slf4j
public class Chat {
    private static final CopyOnWriteArrayList<SendFileThread> fileTreads = new CopyOnWriteArrayList<>();
    private static final int HEARTBEAT_INTERVAL = 5; // 心跳检测间隔，单位为秒
    private static final int SCHEDULED_THREAD_POOL_SIZE = 1;
    private static final int FILE_THREAD_CORE_POOL_SIZE = 4;
    private static final int FILE_THREAD_MAX_POOL_SIZE = 6;
    private static final int FILE_THREAD_KEEP_ALIVE_TIME = 1000;
    private static final int FILE_THREAD_QUEUE_CAPACITY = 10;
    private static ExecutorService pool; // 专门创建线程池来文件收发
    private static User host;
    private static DatagramSocket udpSendSocket;
    private static DatagramSocket udpReceiveSocket;
    private final Scanner sc;
    private ServerSocket server;
    private boolean isRunning;
    private static OnlineFriends onlineFriends;
    private String dataPath;
    private UDPReceiveMsgThread udpReceiveMsgThread;
    ScheduledExecutorService scheduler;

    public Chat(String name, String port) {

        sc = new Scanner(System.in);
        this.isRunning = true;

        try {
            InetAddress addr = InetAddress.getLocalHost();
            host = new User(name, addr.getHostAddress(), Integer.parseInt(port));
            onlineFriends = new OnlineFriends();
            udpSendSocket = new DatagramSocket();
            server = new ServerSocket(Integer.parseInt(port));
            udpReceiveSocket = new DatagramSocket(Integer.parseInt(port));
            udpReceiveMsgThread = new UDPReceiveMsgThread(1024);
            new Thread(udpReceiveMsgThread, "udpReceiveMsgThread").start(); // udp listening
            this.dataPath = "data\\" + name;
            creatUserFile();
            TCPReceiveFileThread();
            HeartbeatDetector();
            pool = new ThreadPoolExecutor(FILE_THREAD_CORE_POOL_SIZE, FILE_THREAD_MAX_POOL_SIZE, FILE_THREAD_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<Runnable>(FILE_THREAD_QUEUE_CAPACITY),  // 有界任务队列
                    Executors.defaultThreadFactory(),
                    new ThreadPoolExecutor.DiscardPolicy()); // 丢弃无法处理的任务，不予任何处理

            log.info("Welcome! " + name + "@" + addr.getHostAddress() + ":" + port);
            showHelp();
        } catch (IOException e) {
            log.error("Construct Chat errors:" + e);
            release();
        }
    }

    private void HeartbeatDetector() {
        scheduler = Executors.newScheduledThreadPool(SCHEDULED_THREAD_POOL_SIZE);
        scheduler.scheduleAtFixedRate(() -> {
            if (onlineFriends.getFriendsMap().isEmpty()) {
                return;
            }
            for (User toUser : onlineFriends.getFriendsMap().values()) {
                Date lastActiveTime = toUser.getLastActiveTime();
                int interval = Utils.calLastedTime(lastActiveTime);
                if (interval > 15) {
                    onlineFriends.removeOneFriend(toUser);
                } else {
                    send(udpSendSocket, toUser.getIp(), toUser.getPort(), ActionEnum.HEARTBEAT + "@" + JSON.toJSONString(host));
                }
            }
        }, 0, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    private void TCPReceiveFileThread() {
        new Thread(() -> {
            try {
                while (isRunning) {
                    Socket fileSocket = server.accept();
                    pool.execute(new RecieveFileThread(fileSocket, dataPath));
                }
            } catch (IOException e) {
                e.printStackTrace(); // 待解决，正常退出的时候总是 java.net.SocketException: socket closed
            }
        }).start();
    }

    private void creatUserFile() {
        File file = new File(dataPath);
        if (!file.exists()) { //如果不存在
            boolean dr = file.mkdirs(); //创建目录
        }
    }

    private void release() {
        this.isRunning = false;
        if (udpReceiveMsgThread != null) {
            udpReceiveMsgThread.release();
        }

        Utils.close(server, udpSendSocket, udpReceiveSocket);
    }

    private String getStrFromConsole() {
        return sc.nextLine();
    }

    private void showHelp() {
        System.out.println(
                "***************************************************************************************\n" +
                        "help:   \t Show help             \n" +
                        "list:   \t List online friends   \n" +
                        "task:   \t List all sending tasks\n" +
                        "exit:   \t Exit the chat system  \n" +
                        "connect:\t Connect with someone  \t connect@ip:port  \t e.g., connect@127.0.0.1:7777\n" +
                        "msg:    \t Send a message        \t msg@name@content \t e.g., msg@wang@hello        \n" +
                        "push:   \t Send a file           \t put@name@file    \t e.g., push@wang@111.txt     \n" +
                        "pause:  \t Pause a sending task  \t pause@taskNum    \t e.g., pause@1               \n" +
                        "restart:\t restart a sending task\t restart@taskNum  \t e.g., restart@1             \n" +
                        "***************************************************************************************");
    }

    public void run() {
        while (isRunning) {
            String msg = getStrFromConsole();
            if (!Objects.equals(msg, "")) {
                consoleHandle(msg);
            }
        }
    }
    private void consoleHandle(String msg) {
        String[] s = msg.split("@");
        String cmd = s[0].toUpperCase();
        if (!Utils.enumContains(ConsoleEnum.class, cmd)) {
            log.info("Unexpected command. Please refer to the following command.");
            showHelp();
            return;
        }
        ConsoleEnum consoleCommand = ConsoleEnum.valueOf(cmd);
        switch (consoleCommand) {
            case HELP: {
                showHelp();
                break;
            }
            case TASK:{
                if(fileTreads.isEmpty()){
                    log.info("There is no sending task");
                }else {
                    StringBuilder allTasks = new StringBuilder("Sending task include:\n");
                    for (int i = 0; i < fileTreads.size(); i++) {
                        allTasks.append(i).append(": ").append(fileTreads.get(i).toString()).append("\n");
                    }
                    log.info(allTasks.toString());
                }
                break;
            }
            case PAUSE: { // pause@1
                int taskIdx = Integer.parseInt(s[1]);
                if(fileTreads.size()+1<taskIdx){
                    log.info("There is no such sending task");
                }else {
                    fileTreads.get(taskIdx).pause();
                }
                break;
            }
            case RESTART: { // restart@name@file
                int taskIdx = Integer.parseInt(s[1]);
                if(fileTreads.size()+1<taskIdx){
                    log.info("There is no such sending task");
                }else {
                    fileTreads.get(taskIdx).restart();
                }
                break;
            }
            case MSG: { // msg@wang@hello
                String name = s[1];
                String content = s[2];
                if (!content.isEmpty()) {
                    sendMsg(name, content);
                } else {
                    log.info("Your message content is empty");
                }
                break;
            }
            case LIST: { // list
                onlineFriends.displayFriends();
                break;
            }
            case CONNECT: { // connect@127.0.0.1:7777
                String toIpPort = s[1];
                String[] Address = toIpPort.split(":");
                String toIp = Address[0];
                int toPort = Integer.parseInt(Address[1]);
                send(udpSendSocket, toIp, toPort, ActionEnum.REQUEST + "@" + JSON.toJSONString(host));
                break;
            }
            case PUSH: { //push@Bob@文件地址
                String toName = s[1];
                String fileName = s[2];
                pushFile(toName, fileName);
                break;
            }
            case EXIT: {
                if (!onlineFriends.isEmpty()) {
                    for (User u : onlineFriends.getAllFriends()) {
                        send(udpSendSocket, u.getIp(), u.getPort(), ActionEnum.EXIT + "@" + JSON.toJSONString(host));
                    }
                }
                release();
                System.exit(0);
            }
        }
    }

    private static void pushFile(String toName, String fileName) {
        File file = new File(fileName);
        if (!onlineFriends.contains(toName)) {
            log.info("The friend " + toName + " does not exist or is considered offline");
        } else if (!file.exists()) {
            log.info("The file " + fileName + " does not exist");
        } else{
            // 同一个socket不要用多个线程读，处理可以是多个线程，文件单独一个线程
            SendFileThread sendFileThread = new SendFileThread(host, file, onlineFriends.getFriendByName(toName));
            fileTreads.add(sendFileThread);
            pool.execute(sendFileThread);
            while (sendFileThread.isFinished()){
                fileTreads.remove(sendFileThread);
            }
        }
    }

    private static void sendMsg(String name, String content) {
        if (!onlineFriends.contains(name)) {
            log.info("The friend " + name + " does not exist or is considered offline");
        } else {
            User toUser = onlineFriends.getFriendByName(name);
            send(udpSendSocket, toUser.getIp(), toUser.getPort(), ActionEnum.MSG + "@" + host.getName() + ": " + content);
        }
    }

    @Setter
    static class UDPReceiveMsgThread implements Runnable {
        private final DatagramSocket socket;
        private final int byteNum;
        boolean isRunning;

        public UDPReceiveMsgThread(int byteNum) {
            this.byteNum = byteNum;
            this.socket = udpReceiveSocket;
            isRunning = true;
        }

        private void release() {
            this.isRunning = false;
            Utils.close(socket);
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    byte[] container = new byte[byteNum];
                    DatagramPacket packet = new DatagramPacket(container, 0, container.length);
                    //3.阻塞式接受包裹
                    this.socket.receive(packet);
                    //显示接受数据
                    byte[] datas = packet.getData();
                    String data = new String(datas).trim();
                    if (!data.equals("")) {
                        String[] s = data.split("@");
                        String actionType = s[0].toUpperCase();
                        String content = s[1];
                        if (Utils.enumContains(ActionEnum.class, actionType)) {
                            udpHandle(ActionEnum.valueOf(actionType), content);
                        }
                    }
                } catch (IOException e) {
                    this.release();
                }
            }
        }

        private void udpHandle(ActionEnum actionType, String content) {
            switch (actionType) {
                case RESPONSE: { // response {"ip":"172.31.160.1","name":"wang","port":7777} 收到响应 添加好友
                    User fromUser = JSON.parseObject(content, User.class);
                    onlineFriends.addOneFriend(fromUser);
                    break;
                }
                case REQUEST: {  // request {"ip":"172.31.160.1","name":"wang","port":7777} 收到连接请求 添加好友 响应回去
                    User fromUser = JSON.parseObject(content, User.class);
                    onlineFriends.addOneFriend(fromUser);
                    send(udpSendSocket, fromUser.getIp(), fromUser.getPort(), ActionEnum.RESPONSE + "@" + JSON.toJSONString(host));
                    break;
                }
                case MSG: { // msg@wang: hello
                    if (!content.isEmpty()) {
                        log.info(content);
                    }
                    break;
                }
                case EXIT: {
                    User exitUser = JSON.parseObject(content, User.class);
                    onlineFriends.removeOneFriend(exitUser);
                }
                case HEARTBEAT: {
                    User onlineUser = JSON.parseObject(content, User.class);
                    onlineFriends.updateOneFriendLastActiveTime(onlineUser);
                }
            }
        }
    }
}