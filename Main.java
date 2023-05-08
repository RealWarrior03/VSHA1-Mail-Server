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

    // taken from the client
    private static boolean readCommandLine(SocketChannel socketChannel, ByteBuffer buffer) throws IOException {

        boolean foundHyphen = false;
        int pos = buffer.position();

        socketChannel.read(buffer);

        for (int i = pos; i < buffer.position(); i++) {

            if (buffer.get(i) == '-' && (i == 3)) {
                foundHyphen = true;
            }

            if (buffer.get(i) == '\n') {
                if ((i - 1) >= 0 && buffer.get(i - 1) == '\r') {
                    if (foundHyphen) {
                        foundHyphen = false;
                    } else {
                        buffer.flip();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static void main(String[] args) throws IOException {
        String hostname = java.net.InetAddress.getLocalHost().getHostName();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(hostname, 2525));


        serverSocketChannel.configureBlocking(false);


        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        HashMap<SocketChannel, MailInfo> activeMailInfos = new HashMap<>();

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

                    String response = "220 " + hostname + " Simple Mail Transfer Service Ready\r\n";
                    clientSocketChannel.write(ByteBuffer.wrap(response.getBytes()));
                } else if (key.isReadable()) {
                    // handle the incoming data from the client
                    SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);

                    if (!readCommandLine(clientSocketChannel, buffer))
                        continue;

                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    String message = new String(bytes);
                    System.out.println("Received message: " + message);
                    String response = "500\r\n";

                    String payload;
                    //checks which command is sent by the client
                    if (activeMailInfos.containsKey(clientSocketChannel)&& activeMailInfos.get(clientSocketChannel).getIsWriting()) { // Client der die Nachricht geschickt hat ist gerade dabei Daten zu senden
                        if (message.substring(message.length() - 6) == "\r\n.\r\n") { // Client will Datenübertragung beenden
                            activeMailInfos.get(clientSocketChannel).setIsWriting(false);
                            message = message.substring(0, message.length() - 6);
                        }
                        activeMailInfos.get(clientSocketChannel).appendData(message);//Übertragenen Daten werden an die Mail angehangen
                        if (!activeMailInfos.get(clientSocketChannel).isWriting) { // Wenn Datentransfer fertig ist
                            activeMailInfos.get(clientSocketChannel).storeMail(); // Speicher sie und entferne sie aus der Datenstruktur
                            activeMailInfos.remove(clientSocketChannel);
                            response = "250 OK";
                        }

                    } else {
                        switch (message.toUpperCase().substring(0, Math.min(message.length(), 4))) { //check commands with len 4 (math.min prevents an out of bounds error
                            case "HELO":
                                payload = message.substring(4, message.length() - 2);
                                response = "250 " + hostname + " \r\n";
                                break;
                            case "DATA":
                                payload = message.substring(4, message.length() - 2);       //#TODO EdgeCase dass Data + Ende in einer Nachricht
                                activeMailInfos.get(clientSocketChannel).setIsWriting(true);
                                try{
                                    activeMailInfos.get(clientSocketChannel).appendData(payload);
                                }catch (NullPointerException ignored){}
                                System.out.println("Handling Data Packet");
                                response = "354"; // Start mail input; end with <CRLF>.<CRLF>
                                break;
                            case "HELP":
                                payload = message.substring(4, message.length() - 2);
                                String code = "214 ";

                                response = code + """
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

                                //TODO different help cases?
                            /*
                            if(message.substring(0, Math.min(message.length(), 9)).equals("HELP HELO")) { //check for rcpt to command
                                response = "help for HELO coming soon\r\n";
                            } else if(message.substring(0, Math.min(message.length(), 14)).equals("HELP MAIL FROM")) { //check for mail from command
                                response = "help for MAIL FROM coming soon\r\n";
                            } else if(message.substring(0, Math.min(message.length(), 12)).equals("HELP RCPT TO")) { //check for rcpt to command
                                response = "help for RCPT TO coming soon\r\n";
                            } else if(message.substring(0, Math.min(message.length(), 9)).equals("HELP DATA")) { //check for mail from command
                                response = "help for DATA coming soon\r\n";
                            } else if(message.substring(0, Math.min(message.length(), 9)).equals("HELP QUIT")) { //check for mail from command
                                response = "help for QUIT coming soon\r\n";
                            } else if (message.substring(0, Math.min(message.length(), 4)).equals("HELP")) {
                                response = code + """
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
                                response = code + """
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
                             */

                                break;
                            case "QUIT":
                                payload = message.substring(4, message.length() - 2);
                                response = "221 " + hostname;
                                //TODO maybe kick client from selectors
                                break;
                            default: //command doesn't match any len 4 command
                                if (message.substring(0, Math.min(message.length(), 9)).equals("RCPT TO: ")) { //check for rcpt to command
                                    if((message.length() - 2) > 9){
                                        payload = message.substring(9, message.length() - 2);
                                    }
                                    String rcpt = message.substring(9, message.length() - 4);
                                    activeMailInfos.get(clientSocketChannel).addRCPT(rcpt);
                                    response = "250 OK\r\n";
                                } else if (message.substring(0, Math.min(message.length(), 11)).equals("MAIL FROM: ")) { //check for mail from command
                                    if((message.length() - 2) > 11){
                                        payload = message.substring(11, message.length() - 2);
                                    }
                                    activeMailInfos.put(clientSocketChannel, new MailInfo(clientSocketChannel));
                                    String sender = message.substring(11, message.length() - 4); // TODO: Ersetzen durch eigentliche Message
                                    activeMailInfos.get(clientSocketChannel).setSender(sender);
                                    response = "250 OK\r\n";
                                } else {
                                    response = "500 Command unrecognized, send \"HELP\"  for more information.\r\n";
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
}