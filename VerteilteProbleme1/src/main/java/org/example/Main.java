package org.example;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("localhost",2525));


        serverSocketChannel.configureBlocking(false);


        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        HashMap<SocketChannel,MailInfo> activeMailInfos = new HashMap<>();

        // listen for incoming client connections
        System.out.println("Server listening on port 2525");
        while (true) {
            selector.select();
            for (SelectionKey key : selector.selectedKeys()) {
                if (key.isAcceptable()) {
                    // handle the incoming client connection
                    System.out.println("Acceptable Key received!");
                    SocketChannel clientSocketChannel = serverSocketChannel.accept();
                    clientSocketChannel.configureBlocking(false);
                    clientSocketChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("Client connected!");
                } else if (key.isReadable()) {
                    // handle the incoming data from the client
                    SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    clientSocketChannel.read(buffer);
                    buffer.flip();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    String message = new String(bytes);
                    System.out.println("Received message: " + message);
                    String response = "500";

                    switch(message){
                        case "HELO":
                            response = "250";
                            break;
                        case "MAIL FROM":
                            activeMailInfos.put(clientSocketChannel,new MailInfo(clientSocketChannel));
                            break;
                        case "RCPT TO":
                            activeMailInfos.get(clientSocketChannel).addRCPT(s);
                            break;
                        case "DATA":
                            System.out.println("Handling Data Packet");
                            byte [] test = {'T','e','s','t'};
                            ByteBuffer buf = ByteBuffer.allocate(8);
                            buf.put(test);
                            buf.flip();
                            FileOutputStream f;
                            f = new FileOutputStream("test.txt");
                            FileChannel ch = f.getChannel();
                            ch.write(buf);
                            ch.close();
                            buf.clear();
                            break;
                        case "HELP":

                            break;
                        case "QUIT":

                            break;
                        default:


                    }
                clientSocketChannel.write(ByteBuffer.wrap(response.getBytes()));



                    /*
                    clientSocketChannel.close();//!!Falsch!!
                    System.out.println("Client disconnected!");
                    */
                }
            }
            selector.selectedKeys().clear();
        }
    }
}