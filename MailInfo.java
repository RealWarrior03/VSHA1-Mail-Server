package vs_uebung_2_gruppe_31;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Stack;

public class MailInfo {
    String sender;
    Stack<String> RCPT;
    SocketChannel channel;
    String data;
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

    public void appendData(String s){
        data = data.concat(s);
    }

}
