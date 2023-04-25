package org.example;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
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

                    try {
                        Charset messageCharset = StandardCharsets.US_ASCII;
                    } catch (UnsupportedCharsetException uce) {
                        System.err.println("Cannot create charset for this application. Exiting...");
                        System.exit(1);
                    }
                    String hostname = java.net.InetAddress.getLocalHost().getHostName().getBytes(messageCharset);

                    String response = "220 localhost Simple Mail Transfer Service Ready";
                    clientSocketChannel.write(ByteBuffer.wrap(response.getBytes()));
                } else if (key.isReadable()) {
                    // handle the incoming data from the client
                    SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    clientSocketChannel.read(buffer);
                    buffer.flip();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    String message = new String(bytes);
                    message = message.toUpperCase();
                    System.out.println("Received message: " + message);
                    String response = "500";
                    System.out.println("test");

                    switch(message){
                        case "HELO":
                            response = "250 OK";
                            break;
                        case "MAIL FROM":

                            break;
                        case "RCPT TO":

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

    //checks which command is sent by the client
    private String filterCommand(String message){

        switch(message.substring(0, Math.min(message.length(), 4))){ //check commands with len 4
            case "HELO":

                break;
            case "DATA":

                break;
            case "HELP":

                break;
            case "QUIT":

                break;
            default:
                if(message.substring(0, Math.min(message.length(), 9)).equals("RCPT TO: ")){

                }else if(message.substring(0, Math.min(message.length(), 11)).equals("MAIL FROM: ")){
                    
                }
        }


        switch(message){
            case "HELO":

                break;
            case "MAIL FROM":

                break;
            case "RCPT TO":

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

}