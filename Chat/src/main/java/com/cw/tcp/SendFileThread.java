package com.cw.tcp;

import com.cw.chat.User;
import com.cw.utils.ConsoleProgressBar;
import com.cw.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import javax.swing.*;


@Slf4j
public class SendFileThread extends Thread {
    private ProgressMonitorInputStream monitor;
    private ProgressMonitor progressMonitor;
    private Socket socket;
    private FileInputStream fis;
    private DataOutputStream dos;
    private final User fromUser;
    private final File file;
    private final User toUser;
    private int process;

    private volatile boolean isPaused = false; //暂停

    @Override
    public String toString() {
        return "{fromUser: " + fromUser.getName() +
                ", file: " + file.getName() +
                ", toUser: " + toUser.getName() +
                ", process: " + process + "%" +
                ", isPaused=" + isPaused + "}";
    }

    private boolean finished;

    public SendFileThread(User fromUser, File file, User toUser) {
        this.finished = false;
        this.file = file;
        this.toUser = toUser;
        this.fromUser = fromUser;
        Thread.currentThread().setName(toUser.getName() + "@" + file.getName());
        try {
            this.socket = new Socket();
            socket.connect(new InetSocketAddress(toUser.getIp(), toUser.getPort()), 5000);
            dos = new DataOutputStream(socket.getOutputStream());
            fis = new FileInputStream(file);
        } catch (IOException e) {
            log.error("An error occurred while creating the socket：", e);
        }
    }


    public boolean isFinished() {
        return finished;
    }

    public void pause() {
        isPaused = true;
    }

    public void restart() {
        isPaused = false;
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void run() {
        try {
            Date currentime = Utils.getCurrentime();
            log.info("Sending the file {} to {}, {} bytes in total.", file.getName(), toUser.getName(), file.length());
            ConsoleProgressBar cpb = new ConsoleProgressBar(50, '#');
            dos.writeUTF(fromUser.getName() + "@" + file.getName() + "@" + file.length());
            byte[] buffer = new byte[1024];
            int len;
            //读入
            float readed = 0.0F;
            while ((len = fis.read(buffer)) != -1) {
                //paused
                Thread.sleep(100); // 为了更方便调试
                readed += len;
                dos.write(buffer, 0, len);
                dos.flush();
                process = (int) (readed / file.length() * 100);
                synchronized (this) {
                    while (isPaused) {
                        log.info("Pause sending the file {} to {}.", file.getName(), toUser.getName());
                        wait();
                        log.info("Continue sending the file {} to {}.", file.getName(), toUser.getName());
                    }
                }
            }
            int seconds = Utils.calLastedTime(currentime);
            log.info("Done! It costs {} seconds", seconds);
        } catch (IOException e) {
            log.error("An error occurred while sending the file: " + e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            this.finished = true;
            Utils.close(fis, dos, socket);
        }
    }
}
