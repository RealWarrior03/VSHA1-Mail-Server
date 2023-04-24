package org.example;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("localhost",2525));


        serverSocketChannel.configureBlocking(false);


        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

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
                    String response = "Antwort";
                    clientSocketChannel.write(ByteBuffer.wrap(response.getBytes()));

                    switch(message){
                        case "HELO":

                            break;
                        case "MAIL FROM":

                            break;
                        case "RCPT TO":

                            break;
                        case "DATA":

                            break;
                        case "HELP":

                            break;
                        case "QUIT":

                            break;
                        default:


                    }




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