package org.example;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Stack;

public class MailInfo {
    String sender;
    Stack<String> RCPT;
    SocketChannel channel;
    MailInfo(SocketChannel channel){
        this.channel=channel;
        RCPT = new Stack<>();
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String popRCPT() {
        return RCPT.pop();
    }

    public void addRCPT(String s) {
        RCPT.push(s);
    }

}
