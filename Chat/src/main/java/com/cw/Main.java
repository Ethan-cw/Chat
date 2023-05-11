package com.cw;

import com.cw.chat.Chat;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
public class Main {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please enter your login information and split it with @. (e.g., wang@7777, where 7777 is the listening port.)");
        String loginInformation = br.readLine();
        String[] s = loginInformation.split("@");
        String name = s[0];
        String port = s[1];
        Chat chat = new Chat(name, port);
        chat.run();
    }
}
