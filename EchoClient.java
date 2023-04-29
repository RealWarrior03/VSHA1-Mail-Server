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
        System.out.println("HELO:");
        ec.sendMessage("HELO");
        System.out.println("HELP QUIT:");
        ec.sendMessage("HELP QUIT");
        System.out.println("MAIL FROM:");
        ec.sendMessage("MAIL FROM");
        System.out.println("RCPT TO:");
        ec.sendMessage("RCPT TO");
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
            client = SocketChannel.open(new InetSocketAddress("localhost", 2525));
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
            buffer = ByteBuffer.allocate(256);
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