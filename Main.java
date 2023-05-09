package vs_uebung_2_gruppe_31;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

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

    public static int generateMessageID(LinkedList<Integer> idList) {
        Random rand = new Random();
        int messageID = rand.nextInt(9999); //generate random natural number up to 9999
        while (idList.contains(messageID)) { //if number is already used as an id
            messageID = rand.nextInt(9999); //generate a new number
        }
        idList.add(messageID); //store number as a used id
        return messageID; //return the id
    }

    public static void main(String[] args) throws IOException {
        //String hostname = java.net.InetAddress.getLocalHost().getHostName();
        String hostname = "localhost";
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(hostname, 2525));


        serverSocketChannel.configureBlocking(false);


        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        HashMap<SocketChannel, MailInfo> activeMailInfos = new HashMap<>(); //HashMap of MailInfo Objects, which stores all the information about mails
        LinkedList<Integer> messageIDs = new LinkedList<>(); //list of all used id's for messages


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

                    //trying to create US ASCII charset for messages
                    try {
                        Charset messageCharset = StandardCharsets.US_ASCII; //#TODO Never used, maybe remove??
                    } catch (UnsupportedCharsetException uce) {
                        System.err.println("Cannot create charset for this application. Exiting...");
                        System.exit(1);
                    }

                    //server is ready to communicate with clients
                    String response = "220 " + hostname + " Simple Mail Transfer Service Ready\r\n";
                    clientSocketChannel.write(ByteBuffer.wrap(response.getBytes()));
                } else if (key.isReadable()) {
                    // handle the incoming data from the client
                    SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);

                    if (!readCommandLine(clientSocketChannel, buffer))
                        continue;

                    //get bytes from buffer and construct a string representing a received message
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    String message = new String(bytes);
                    System.out.println("Received message: " + message);
                    String response = "500\r\n";

                    String payload;
                    //checks which command is sent by the client
                    if (activeMailInfos.containsKey(clientSocketChannel)&& activeMailInfos.get(clientSocketChannel).getIsWriting()) { //client is currently transmitting data
                        response = "";

                        if (message.length()>=5 && message.substring(message.length() - 5).equals("\r\n.\r\n")) { //client wants to finish data transmission
                            activeMailInfos.get(clientSocketChannel).setIsWriting(false); //toggle isWriting back to false
                            message = message.substring(0, message.length() - 5); //cut the signal for transmission stop from the actual message
                        }
                        activeMailInfos.get(clientSocketChannel).appendData(message);// transmitted data is appended to the mail
                        if (!activeMailInfos.get(clientSocketChannel).isWriting) { // if data transfer is finished
                            activeMailInfos.get(clientSocketChannel).storeMail(generateMessageID(messageIDs)); // save mail and remove it form the data structure
                            activeMailInfos.remove(clientSocketChannel);
                            response = "250 OK\r\n";
                        }

                    } else {
                        switch (message.toUpperCase().substring(0, Math.min(message.length(), 4))) { //check commands with len 4 (math.min prevents an out of bounds error
                            case "HELO":
                                response = "250 " + hostname + " \r\n"; //answer according to a received HELO message
                                break;
                            case "DATA":
                                activeMailInfos.get(clientSocketChannel).setIsWriting(true); //toggle isWriting to work with the next input as data for an e-mail
                                System.out.println("Handling Data Packet"); //server side note of what is handled
                                response = "354 Start mail input; end with <CRLF>.<CRLF>\r\n"; //inform client, that data is read until marking its end
                                break;
                            case "HELP":
                                payload = message.substring(4, message.length() - 2);
                                String code = "214 ";

                                //if the client requests HELP for a specific command, the information for that command only is returned, otherwise a list of all commands is returned
                                if (payload.equals(" HELO")) {
                                    response = code + "HELO - The HELO command initiates the SMTP session conversation. The client greets the server and introduces itself. As a rule, HELO is attributed with an argument that specifies the domain name or IP address of the SMTP client.\r\n";
                                } else if (payload.equals(" MAIL FROM")) {
                                    response = code + "MAIL FROM - The MAIL FROM command initiates a mail transfer. As an argument, MAIL FROM includes a sender mailbox (reverse-path).\r\n";
                                } else if (payload.equals(" RCPT TO")) {
                                    response = code + "RCPT TO - The RCPT TO command specifies the recipient. As an argument, RCPT TO includes a destination mailbox (forward-path). In case of multiple recipients, RCPT TO will be used to specify each recipient separately.\r\n";
                                } else if (payload.equals(" DATA")) {
                                    response = code + "DATA - With the DATA command, the client asks the server for permission to transfer the mail data. The response code 354 grants permission, and the client launches the delivery of the email contents line by line. This includes the date, from header, subject line, to header, attachments, and body text.";
                                } else if (payload.equals(" QUIT")) {
                                    response = code + "QUIT - The QUIT command send the request to terminate the SMTP session. Once the server responses with 221, the client closes the SMTP connection.";
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
                                            
                                            """; //help for all supported commands is returned to the client
                                }
                                break;
                            case "QUIT":
                                response = "221 " + hostname + "\r\n"; //answer according to a received QUIT message
                                //#TODO maybe kick client from selectors
                                break;
                            default: //command doesn't match any len 4 command
                                if (message.toUpperCase().substring(0, Math.min(message.length(), 9)).equals("RCPT TO: ")) { //check for rcpt to command
                                    payload = message.substring(9, message.length() - 2); //get client name appended to the RCPT TO command
                                    String rcpt = payload;
                                    activeMailInfos.get(clientSocketChannel).addRCPT(rcpt); //add it to the list of known recipients
                                    response = "250 OK\r\n";
                                } else if (message.toUpperCase().substring(0, Math.min(message.length(), 11)).equals("MAIL FROM: ")) { //check for mail from command
                                    payload = message.substring(11, message.length() - 2); //get client name appended to the MAIL FROM command
                                    activeMailInfos.put(clientSocketChannel, new MailInfo(clientSocketChannel));
                                    String sender = payload;
                                    activeMailInfos.get(clientSocketChannel).setSender(sender); //add the sender to the list of known senders
                                    response = "250 OK\r\n";
                                } else {
                                    response = "500 Command unrecognized, send \"HELP\"  for more information.\r\n"; //command not known, reminder to ask for help
                                }
                        }
                    }
                    clientSocketChannel.write(ByteBuffer.wrap(response.getBytes()));
                }
                selector.selectedKeys().clear();
            }
        }
    }
}