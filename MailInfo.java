package vs_uebung_2_gruppe_31;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Stack;

/**
 * Class stores the information already received by the server regarding an email and
 * saves the file with the emails content into the right folder.
 * It also stores the information about the state of the client regarding the DATA message in the isWriting boolean.
 */
public class MailInfo {
    String sender;
    Stack<String> RCPT;
    SocketChannel channel;
    String data;
    boolean isWriting;
    MailInfo(SocketChannel channel){
        this.channel=channel;
        RCPT = new Stack<>();
        isWriting=false;
        data = "";
    }

    public String getData() {
        return data;
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

    public void storeMail(int messageID) throws IOException {       //stores the mail with the information of the MailInfo-Object in the right directory and the passed MessageID in the storedMails Folder
        ByteBuffer buf = ByteBuffer.allocate(data.length()*2);
        buf.put(data.getBytes());
        buf.flip();
        FileOutputStream f;

        for (String recipient: RCPT) {
            new File("./storedMails/" + recipient).mkdirs();
            f = new FileOutputStream("./storedMails/"+recipient+"/"+sender+"_"+messageID);//#TODO Message ID
            FileChannel ch = f.getChannel();
            ch.write(buf);
            ch.close();
            buf.clear();
        }

    }
}






