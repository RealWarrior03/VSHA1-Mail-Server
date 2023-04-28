package vs_uebung_2_gruppe_31;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
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
                    //String hostname = java.net.InetAddress.getLocalHost().getHostName().getBytes(messageCharset);

                    String response = "220 127.0.0.1 Simple Mail Transfer Service Ready\r\n";
                    clientSocketChannel.write(ByteBuffer.wrap(response.getBytes()));
                } else if (key.isReadable()) {
                    // handle the incoming data from the client
                    SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);

                    clientSocketChannel.read(buffer);  //TODO implement termination of reading if \r\n(?)
                    System.out.println("finished reading buffer");
                    buffer.flip();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    String message = new String(bytes);
                    message = message.toUpperCase();
                    System.out.println("Received message: " + message);
                    String response = "500\r\n";

                    //checks which command is sent by the client
                    switch (message.substring(0, Math.min(message.length(), 4))) { //check commands with len 4 (math.min prevents an out of bounds error
                        case "HELO":
                            response = "250 OK\r\n";        //TODO rückmeldung sollte "250 server_name" sein oder?
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
                        default: //command doesn't match any len 4 command
                            if(message.substring(0, Math.min(message.length(), 9)).equals("RCPT TO: ")) { //check for rcpt to command

                            } else if(message.substring(0, Math.min(message.length(), 11)).equals("MAIL FROM: ")) { //check for mail from command

                            }
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