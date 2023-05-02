package vs_uebung_2_gruppe_31;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
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

                    String payload;
                    //checks which command is sent by the client
                    if(activeMailInfos.get(clientSocketChannel).getIsWriting()){
                        if(message.substring(message.length()-6)=="\r\n.\r\n"){
                            activeMailInfos.get(clientSocketChannel).setIsWriting(false);
                            message = message.substring(0,message.length()-6);
                        }
                        activeMailInfos.get(clientSocketChannel).appendData(message);
                        if(!activeMailInfos.get(clientSocketChannel).isWriting){
                            activeMailInfos.get(clientSocketChannel).storeMail();
                            activeMailInfos.remove(clientSocketChannel);
                        }

                    } else {
                        switch (message.substring(0, Math.min(message.length(), 4))) { //check commands with len 4 (math.min prevents an out of bounds error
                            case "HELO":
                                payload = message.substring(4, message.length() - 3);
                                response = "250 OK\r\n";        //TODO rückmeldung sollte "250 server_name" sein oder?
                                break;
                            case "DATA":
                                payload = message.substring(4, message.length() - 3);       //#TODO EdgeCase dass Data + Ende in einer Nachricht
                                activeMailInfos.get(clientSocketChannel).setIsWriting(true);
                                activeMailInfos.get(clientSocketChannel).appendData(payload);
                                System.out.println("Handling Data Packet");
                                break;
                            case "HELP":
                                payload = message.substring(4, message.length() - 3);
                                if (message.substring(0, Math.min(message.length(), 9)).equals("HELP HELO")) { //check for rcpt to command
                                    response = "help for HELO coming soon\r\n";
                                } else if (message.substring(0, Math.min(message.length(), 14)).equals("HELP MAIL FROM")) { //check for mail from command
                                    response = "help for MAIL FROM coming soon\r\n";
                                } else if (message.substring(0, Math.min(message.length(), 12)).equals("HELP RCPT TO")) { //check for rcpt to command
                                    response = "help for RCPT TO coming soon\r\n";
                                } else if (message.substring(0, Math.min(message.length(), 9)).equals("HELP DATA")) { //check for mail from command
                                    response = "help for DATA coming soon\r\n";
                                } else if (message.substring(0, Math.min(message.length(), 9)).equals("HELP QUIT")) { //check for mail from command
                                    response = "help for QUIT coming soon\r\n";
                                } else if (message.substring(0, Math.min(message.length(), 4)).equals("HELP")) {
                                    response = """
                                            The following commands are supported:
                                            HELO - The HELO command initiates the SMTP session conversation. The client greets the server and introduces itself. As a rule, HELO is attributed with an argument that specifies the domain name or IP address of the SMTP client.
                                            MAIL FROM - The MAIL FROM command initiates a mail transfer. As an argument, MAIL FROM includes a sender mailbox (reverse-path).
                                            RCPT TO - The RCPT TO command specifies the recipient. As an argument, RCPT TO includes a destination mailbox (forward-path). In case of multiple recipients, RCPT TO will be used to specify each recipient separately.
                                            DATA - With the DATA command, the client asks the server for permission to transfer the mail data. The response code 354 grants permission, and the client launches the delivery of the email contents line by line. This includes the date, from header, subject line, to header, attachments, and body text.
                                            HELP [command] - With the HELP command, the client requests a list of commands the server supports. HELP may be used with an argument (a specific command).
                                            QUIT - The QUIT command send the request to terminate the SMTP session. Once the server responses with 221, the client closes the SMTP connection.
                                                                                
                                            The explanantions of the commands above are taken from the following website: https://mailtrap.io/blog/smtp-commands-and-responses/#HELP
                                            \r\n
                                            """;
                                } else {
                                    response = """
                                            The following commands are supported:
                                            HELO - The HELO command initiates the SMTP session conversation. The client greets the server and introduces itself. As a rule, HELO is attributed with an argument that specifies the domain name or IP address of the SMTP client.
                                            MAIL FROM - The MAIL FROM command initiates a mail transfer. As an argument, MAIL FROM includes a sender mailbox (reverse-path).
                                            RCPT TO - The RCPT TO command specifies the recipient. As an argument, RCPT TO includes a destination mailbox (forward-path). In case of multiple recipients, RCPT TO will be used to specify each recipient separately.
                                            DATA - With the DATA command, the client asks the server for permission to transfer the mail data. The response code 354 grants permission, and the client launches the delivery of the email contents line by line. This includes the date, from header, subject line, to header, attachments, and body text.
                                            HELP [command] - With the HELP command, the client requests a list of commands the server supports. HELP may be used with an argument (a specific command).
                                            QUIT - The QUIT command send the request to terminate the SMTP session. Once the server responses with 221, the client closes the SMTP connection.
                                                                                
                                            The explanantions of the commands above are taken from the following website: https://mailtrap.io/blog/smtp-commands-and-responses/#HELP
                                            \r\n
                                            """;
                                }
                                System.out.println(response);
                                break;
                            case "QUIT":
                                payload = message.substring(4, message.length() - 3);
                                break;
                            default: //command doesn't match any len 4 command
                                if (message.substring(0, Math.min(message.length(), 9)).equals("RCPT TO: ")) { //check for rcpt to command
                                    payload = message.substring(9, message.length() - 3);
                                    activeMailInfos.get(clientSocketChannel).addRCPT(payload);
                                    response = "250 OK\r\n";
                                } else if (message.substring(0, Math.min(message.length(), 11)).equals("MAIL FROM: ")) { //check for mail from command
                                    payload = message.substring(11, message.length() - 3);
                                    activeMailInfos.put(clientSocketChannel, new MailInfo(clientSocketChannel));
                                    activeMailInfos.get(clientSocketChannel).setSender(payload);
                                    response = "250 OK\r\n";
                                } else {
                                    response = "500 Command unrecognized, send \"HELP\"  for more information.\r\n";
                                }
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