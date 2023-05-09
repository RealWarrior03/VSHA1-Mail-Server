package vs_uebung_2_gruppe_31;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class EchoClient {
    private static SocketChannel client;
    private static ByteBuffer buffer;
    private static EchoClient instance;

    public static void main(String[] args) {
        EchoClient ec = new EchoClient();
        ec.sendMessage("HELO\r\n");
        ec.sendMessage("MAIL FROM: abc\r\n");
        ec.sendMessage("RCPT TO: def\r\n");
        ec.sendMessage("RCPT TO: gih\r\n");
        ec.sendMessage("DATA\r\n");
        ec.sendMessage("bliblablub\r\n.\r\n");
        ec.sendMessage("DATA\r\n");
        ec.sendMessage("1\r\n.\r\n");
    }

    public static EchoClient start() {
        if (instance == null)
            instance = new EchoClient();

        return instance;
    }

    public static void stop() throws IOException {
        client.close();
        buffer = null;
    }

    private EchoClient() {
        try {
            client = SocketChannel.open(new InetSocketAddress(java.net.InetAddress.getLocalHost().getHostName(), 2525));
            buffer = ByteBuffer.allocate(256);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String sendMessage(String msg) {
        buffer = ByteBuffer.wrap(msg.getBytes());
        String response = null;
        try {
            client.write(buffer);
            buffer.clear();
            //buffer = ByteBuffer.allocate(256);
            buffer = ByteBuffer.allocate(4096);
            client.read(buffer);
            response = new String(buffer.array()).trim();
            System.out.println("response=" + response);
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;

    }
}