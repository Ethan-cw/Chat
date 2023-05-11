package com.cw.udp;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

@Slf4j
public class SendUDP {

    public static void send(DatagramSocket socket, String toIP, int toPort, String msg){
        try {
            byte[] datas = msg.getBytes();
            //2.创建数据包
            //参数：数据，数据开始点，数据长度，发送的地址
            DatagramPacket packet = new DatagramPacket(datas, 0, datas.length, new InetSocketAddress(toIP, toPort));
            //3.发送数据包
            socket.send(packet);
        } catch (IOException e) {
           log.error("Sending UDP messages error:", e);
        }
    }
}
