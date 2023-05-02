package vs_uebung_2_gruppe_31;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Stack;

public class MailInfo {
    String sender;
    Stack<String> RCPT;
    SocketChannel channel;
    String data;
    boolean isWriting;
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

    public boolean getIsWriting() {
        return isWriting;
    }

    public void setIsWriting(boolean a){
        isWriting = a;
    }

    public void storeMail() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(data.length()*2);
        buf.put(data.getBytes());
        buf.flip();
        FileOutputStream f;

        for (String recipient: RCPT) {
            new File("./storedMails/" + recipient).mkdirs();
            f = new FileOutputStream("./storedMails/"+recipient+"/"+sender+"_"+"MessageID");//#TODO Message ID
            FileChannel ch = f.getChannel();
            ch.write(buf);
            ch.close();
            buf.clear();
        }

    }

    public static void main(String[] args) throws IOException {

    }
}

