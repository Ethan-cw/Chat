package com.cw.chat;

import java.util.Date;

public class User {
    private final String name;
    private final String ip;
    private final int port;

    private Date lastActiveTime;
    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public User(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.lastActiveTime = new Date();
    }

    public String getName() {
        return name;
    }

    public void setLastActiveTime(Date lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public Date getLastActiveTime() {
        return lastActiveTime;
    }

    @Override
    public String toString() {
        return  name+" "+ip+":"+port;
    }
}
