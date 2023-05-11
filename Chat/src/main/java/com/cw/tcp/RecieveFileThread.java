package com.cw.tcp;

import com.cw.utils.ConsoleProgressBar;
import com.cw.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;

@Slf4j
public class RecieveFileThread extends Thread {
    private final String dataPath;
    private final Socket socket;
    private boolean isRunning;
    private ProgressMonitorInputStream monitor;
    public RecieveFileThread(Socket socket, String dataPath) {
        this.isRunning = true;
        this.socket = socket;
        this.dataPath = dataPath;
    }

    @Override
    public void run() {
        try {
            Date currentime = Utils.getCurrentime();
            ConsoleProgressBar cpb = new ConsoleProgressBar(50, '#');
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            String userAndFile = dis.readUTF();
            String[] s = userAndFile.split("@");
            String fromUserName = s[0];
            String fileName = s[1];
            String length = s[2];
            log.info("Receiving {}'s file {}, {} bytes in total", fromUserName, fileName, length);
            File file = Utils.buildFile(dataPath + "\\", fileName, 0);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len = 0;
            //读入
            float readed = 0.0F;
            while ((len = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                fos.flush();
                readed += len;
                int process = (int) (readed / Integer.parseInt(length) * 100);// 算出百分比
                cpb.show(process);
            }
            int seconds = Utils.calLastedTime(currentime);
            log.info("Done! It costs {} seconds", seconds);
            Utils.close(dis, fos, socket);
        } catch (IOException e) {
            log.error("An error occurred while receiving the file");
        }
    }
}
