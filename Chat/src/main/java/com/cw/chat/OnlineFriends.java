package com.cw.chat;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class OnlineFriends {
    private final ConcurrentHashMap<String, User> friendsMap;

    public ConcurrentHashMap<String, User> getFriendsMap() {
        return friendsMap;
    }

    public OnlineFriends() {
        this.friendsMap = new ConcurrentHashMap<>();
    }

    void addOneFriend(User user) {
        if (!friendsMap.containsKey(user.getName())) {
            user.setLastActiveTime(new Date());
            friendsMap.put(user.getName(), user);
            log.info("Connected to {}.", user);
        }
    }

    public void displayFriends() {
        if (friendsMap.isEmpty()) {
            log.info("No friends are currently online.");
        } else {
            StringBuilder userInfo = new StringBuilder(friendsMap.size() + "friends are online, including:\n");
            for (User friend : friendsMap.values()) {
                userInfo.append(friend.toString()).append("\n");
            }
            log.info(userInfo.toString());
        }
    }

    public boolean contains(String name) {
        return friendsMap.containsKey(name);
    }

    public User getFriendByName(String name) {
        return friendsMap.get(name);
    }

    public void removeOneFriend(User exitUser) {
        if (friendsMap.containsKey(exitUser.getName())) {
            friendsMap.remove(exitUser.getName());
            log.info("the friend: {} is offline", exitUser);
        }
    }

    public void updateOneFriendLastActiveTime(User onlineUser) {
        if (friendsMap.containsKey(onlineUser.getName())) {
            friendsMap.get(onlineUser.getName()).setLastActiveTime(new Date());
        }
    }

    public boolean isEmpty() {
        return friendsMap.isEmpty();
    }

    public Collection<User> getAllFriends() {
        return friendsMap.values();
    }
}
