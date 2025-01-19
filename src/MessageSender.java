// Avromi Schneierson - 11/3/2023
package src;

import src.InternetProtocolHandling.MultiPacketEncoder;
import src.InternetProtocolHandling.PacketDecoder;
import src.InternetProtocolHandling.PacketEncoder;
import src.InternetProtocolHandling.enums.PacketArgKey;
import javafx.concurrent.Task;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

/**
 * This Task is responsible for sending a full message to a Client. The class is instantiated with the required
 * arguments needed to do so.
 */
public class MessageSender extends Task<Boolean> {
    private final boolean SIMULATE_DROPPED_PACKETS = true;
    private final float PACKET_DROP_PROBABILITY = 0.2f;
    private final int maxWaitBeforeSocketTimeout = 60000;
    private final int portNumber;
    private final String messageContent;

    public MessageSender(String messageContent, int portNumber) {
        this.messageContent = messageContent;
        this.portNumber = portNumber;
    }

    /**
     * Connect to the client and send a message. Upon returning, this method returns and sets this
     * Task's value to a boolean indicating if the message was fully sent.
     * <p>
     * This method does the following:
     *      <ul>
     *          <li>creates a socket and waits for a client to connect</li>
     *          <li>upon client connection, sends all packets to the client once (minus any 'dropped' packets)</li>
     *          <li>when completed sending all packets, waits for client response as to whether all packets were received</li>
     *          <li>if the client indicates that it is still missing some packets, this method then sends those missing packets again</li>
     *          <li>when the client indicates that all packets were received, this method terminates with a value of <code>true</code></li>
     *      </ul>
     * </p>
     *
     * @return <code>true</code> if the message was successfully sent, <code>false</code> if it was not
     */
    @Override
    protected Boolean call() {
        MultiPacketEncoder allPacketsEncoder = new MultiPacketEncoder(new HashMap<>(), new HashMap<>(), messageContent);
        ArrayList<PacketEncoder> messagePackets = allPacketsEncoder.getPackets();
        int packetsSent = 0;
        int droppedPackets = 0;
        updateMessage("Waiting for client to connect...");
        log("waiting for client to connect...");
        try (
                ServerSocket serverSocket = new ServerSocket(portNumber);
        ) {
            serverSocket.setSoTimeout(maxWaitBeforeSocketTimeout);
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
                updateMessage("Client connected");
                log("client connected");
            } catch (SocketTimeoutException e) {
                updateMessage("Server timed out while waiting for client connection for " + maxWaitBeforeSocketTimeout + "ms");
                log("Server timed out while waiting for client connection for " + maxWaitBeforeSocketTimeout + "ms");
                return false;
            }

            try (PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
                updateMessage("Client connected");
                log("client connected");
                int characterVal;
                // Wait for and then process the client's packet with either a request or confirmation of message receipt
                while ((characterVal = clientIn.read()) != -1 && !isCancelled()) {
                    PacketDecoder packet = new PacketDecoder(String.valueOf((char) characterVal));
                    log("receiving packet...");
                    while (!packet.isComplete()) {
                        packet.appendToPacketString(String.valueOf((char) clientIn.read()));
                    }
                    log("RECEIVED: '" + packet.getPacketString() + "'");

                    // After receiving the client packet, check what the client wants and reply accordingly:
                    boolean isFirstRequest = (packet.containsArg(PacketArgKey.REQUEST_TYPE) && packet.getArg(PacketArgKey.REQUEST_TYPE).equals("MESSAGE"));
                    boolean clientIsMissingPackets = (packet.containsArg(PacketArgKey.COMPLETED) && packet.getArg(PacketArgKey.COMPLETED).equals("F"));
                    boolean sendPackets = isFirstRequest || clientIsMissingPackets;
                    if (sendPackets) {
                        ArrayList<Integer> packetNumsToSend = new ArrayList<>();
                        if (isFirstRequest) {
                            for (int i = 0; i < messagePackets.size(); i++) {
                                packetNumsToSend.add(i);
                            }
                        } else {
                            int[] missingNums = packet.getIntArrayArg(PacketArgKey.MISSING_PACKET_NUMS);
                            for (int num : missingNums) {
                                packetNumsToSend.add(num);
                            }
                            if (packetNumsToSend.size() == 0) {
                                log("ERROR: unable to retrieve " + PacketArgKey.MISSING_PACKET_NUMS + " from packet");
                                continue;
                            }
                        }

                        // Send the packets...
                        if (SIMULATE_DROPPED_PACKETS) Collections.shuffle(packetNumsToSend);
                        for (int i = 0; i < packetNumsToSend.size() - 1; i++) {
                            if (!SIMULATE_DROPPED_PACKETS || Math.random() >= PACKET_DROP_PROBABILITY) {
                                clientOut.print(messagePackets.get(packetNumsToSend.get(i)));
                                clientOut.flush();  // flush is required to ensure packet get sent
                                log("sent packet '" + messagePackets.get(packetNumsToSend.get(i)) + "'");
                            } else {
                                droppedPackets++;
                            }
                            packetsSent++;
                            updateMessageAndProgress(packetsSent, (allPacketsEncoder.getNumTotalPackets() - packetNumsToSend.size()), allPacketsEncoder.getNumTotalPackets());
                        }

                        // (the last packet is never dropped)
                        PacketEncoder lastPacket = messagePackets.get(packetNumsToSend.get(packetNumsToSend.size() - 1));
                        lastPacket.setArg(PacketArgKey.COMPLETED, "T");
                        clientOut.print(lastPacket);
                        clientOut.flush();
                        log("sent packet '" + lastPacket + "'");
                        packetsSent++;
                        updateMessageAndProgress(packetsSent, (allPacketsEncoder.getNumTotalPackets() - packetNumsToSend.size()), allPacketsEncoder.getNumTotalPackets());
                    } else {
                        updateMessageAndProgress(packetsSent, allPacketsEncoder.getNumTotalPackets(), allPacketsEncoder.getNumTotalPackets());
                        log("Message successfully sent.");
                        log("total packets sent: " + packetsSent + "\npackets 'dropped': " + droppedPackets + "\npackets not dropped: " + (packetsSent - droppedPackets));
                        return true;
                    }
                }
                // If the input stream is closed that means we stopped receiving messages from the client
                if (isCancelled()) {
                    updateMessage("Task cancelled - message not sent");
                    log("task cancelled - message not sent");
                } else {
                    updateMessage("Lost connection to the client - message not sent");
                    log("lost connection to the client - message not sent");
                }
                return false;
            }
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            updateMessage("Connection error");
            log("EXCEPTION: exception while listening on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
    }

    private void updateMessageAndProgress(int packetsSent, int packetsReceived, int totalPackets) {
        updateProgress(packetsReceived, totalPackets);
        updateMessage("Packets sent: " + packetsSent + " - Packets received: " + packetsReceived + " out of " +
                totalPackets + " total packets...\nPacket retransmissions: " +
                String.format("%.0f", ((packetsSent - packetsReceived) * 100) / (float) packetsSent) + "%");
    }

    private void log(String message) {
        System.out.println("SERVER - " + message);
    }
}
