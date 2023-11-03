// Avromi Schneierson - 11/3/2023
package src;

import src.InternetProtocolHandling.MultiPacketDecoder;
import src.InternetProtocolHandling.PacketDecoder;
import src.InternetProtocolHandling.PacketEncoder;
import src.InternetProtocolHandling.enums.PacketArgKey;
import javafx.concurrent.Task;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class is responsible for receiving a message from a Server. The class is instantiated with the required arguments
 * that the run function needs.
 */
public class MessageReceiver extends Task<String> {
    private final String ip;
    private final int portNumber;

    public MessageReceiver(String ip, int portNumber) {
        this.ip = ip;
        this.portNumber = portNumber;
    }

    /**
     * Connect to the server and receive a message. Upon fully receiving the message, this method returns and sets this
     * Task's value to the received message.
     * <p>
     * This method does the following:
     *     <ul>
     *         <li>creates a socket and waits for the server to connect</li>
     *         <li>upon connecting to the server, sends the initial message request packet</li>
     *         <li>waits for and receives the packets containing the message</li>
     *         <li>when the server indicates that it is done sending all packets, this method sends a packet to the server
     *         indicating which packets it has still not received (that were 'dropped')</li>
     *         <li>this repeats until this method has received all packets, at which point this method sends a
     *         final packet indicating success to the server and terminates</li>
     *     </ul>
     * </p>
     *
     * @return the message received from the server, or null if a message wasn't received or an error occurred
     */
    @Override
    protected String call() {
        PacketEncoder packetEncoder = new PacketEncoder();
        MultiPacketDecoder allPacketsDecoder = new MultiPacketDecoder();
        try (
                Socket clientSocket = new Socket(ip, portNumber);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            updateMessage("Connected to server, requesting message");
            log("server connected");

            // Request a message to receive from the server
            HashMap<PacketArgKey, String> reqArgs = new HashMap<>();
            reqArgs.put(PacketArgKey.REQUEST_TYPE, "MESSAGE");
            PacketEncoder requestPacket = new PacketEncoder(reqArgs, new HashMap<>());
            out.print(requestPacket.getPacketString());
            out.flush();  // flush is required to ensure packet get sent

            // Wait for the server's response with the message packets
            // Wait until the first char is received from the server. Once it is, read one more character at a time until
            // the packet is complete. The packet will be complete based on the packet's indicated length, so reading one
            // character at a time allows us to receive and read packets that contain newline characters.
            int characterVal;
            while ((characterVal = in.read()) != -1 && !isCancelled()) {
                PacketDecoder packet = new PacketDecoder(String.valueOf((char) characterVal));
                while (!packet.packetLengthMatchesIndicator()) {
                    packet.appendToPacketString(String.valueOf((char) in.read()));
                }
                allPacketsDecoder.addPacket(packet);
                log("RECEIVED: '" + packet.getPacketString() + "'");

                updateMessage("Receiving message: " + String.format("%,.2f", allPacketsDecoder.getPercentComplete()) + "% complete...");
                updateProgress(allPacketsDecoder.getNumReceivedPackets(), allPacketsDecoder.getNumTotalPackets());

                // Continue to receive all the packets until the server is finished, or we received all the packets from
                // the message.
                if (allPacketsDecoder.receivedAllPackets()) {
                    // Send packet indicating that receipt is complete and terminate
                    HashMap<PacketArgKey, String> regArgs = new HashMap<>();
                    regArgs.put(PacketArgKey.COMPLETED, "T");
                    PacketEncoder completedPacket = new PacketEncoder(regArgs);
                    out.print(completedPacket.getPacketString());
                    out.flush();
                    log("sent packet '" + completedPacket.getPacketString() + "'");
                    System.out.println("Message received:\n******* BEGIN *******\n" +
                            allPacketsDecoder.getFullMessage(true) + "\n******** END ********");
                    updateMessage("");
                    return allPacketsDecoder.getFullMessage(true);
                } else if (allPacketsDecoder.containsArg(PacketArgKey.COMPLETED) && allPacketsDecoder.getArg(PacketArgKey.COMPLETED).equals("T")) {
                    // Send a packet indicating the missing packets and wait for more packets
                    HashMap<PacketArgKey, String> regArgs = new HashMap<>();
                    HashMap<PacketArgKey, Object[]> arrayArgs = new HashMap<>();
                    HashSet<Integer> missingPackets = (HashSet<Integer>) allPacketsDecoder.getMissingPacketNumbers();
                    regArgs.put(PacketArgKey.COMPLETED, "F");
                    regArgs.put(PacketArgKey.TOTAL_PACKETS_MISSING, String.valueOf(missingPackets.size()));
                    arrayArgs.put(PacketArgKey.MISSING_PACKET_NUMS, missingPackets.toArray(new Integer[0]));
                    packetEncoder.setArgs(regArgs, arrayArgs, true);

                    out.print(packetEncoder.getPacketString());
                    out.flush();
                    log("sent packet '" + packetEncoder.getPacketString() + "'");
                }
            }
            if (isCancelled()) {
                updateMessage("Task cancelled - message not received");
                log("task cancelled - message not received");
            } else {
                // If the input stream is closed that means we stopped receiving messages from the client
                updateMessage("Lost connection to the server - message not received");
                log("lost connection to the server - message not received");
            }
            return null;
        } catch (IOException e) {
            updateMessage("Connection error");
            log("EXCEPTION: exception while listening on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            return null;
        }
    }

    private void log(String message) {
        System.out.println("CLIENT - " + message);
    }
}
