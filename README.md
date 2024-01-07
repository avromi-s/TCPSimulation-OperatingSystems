# TCPSimulation

#### This project simulates communication between a Server and Client using a TCP-like protocol. 

### To run the simulation:
-   Download and install the Server and Client application .exe's (found in the releases) onto the same or separate computers
-   On the Server application, select a local file and check off the 'Send file once connection is established' checkbox
-   On the Client application, enter the IP address where the Server application is running:
    -   If you are running both applications on the same computer, use `127.0.0.1`
    -   If the applications are running on different networks, port forwarding will need to be configured on the *Server* application's network to forward all incoming traffic for port `30121` to the local machine that is running the Server application
-   Click the 'Receive File' button on the Client Application
-   After a file is finished sending, additional files can be sent in the same manner

***

### Protocol Description
The protocol allows packets to arrive out of order and recover from lost packets as follows:
1.  The message is broken down into 100-character-length packets (excluding headers) with the number of packets depending on the size of the message
2.  The server sends all of the packets once to the client, finishing with the last packet which indicates that it is such in the header
3.  Upon receival of the last packet, the client sends a message back to the server either indicating that all packets were successfully received, or that it is missing some packets
    -  If packets are missing, the client indicates the number of packets missing and their sequence numbers
    -  The server then sends again (only) the indicated packets, again indicating the last packet when it is sent
    -  The protocol continues from step 3 again
    -  For the purposes of this simulation, we guarantee that the last packet send from the server is always received by the client

#### Packet Structure
-   Syntax
    -   Packets contain a header and a message section. The header contains key-value pairs as arguments; the message section contains the actual content being transmitted, which is a portion of the larger file/message being transmitted
    -   All packets start with a length indicator that gives the length of the packet contents (including the headers) enclosed in parentheses. The length of the indicator itself is not included in the calculated length
    -   Header structure:
        -   Arguments are encoded with the following structure:
            -   `KEY:VALUE`
            -   Arguments are not case sensitive, but should be expressed in capital letters
        -   Multiple arguments are separated by commas
        -   Arguments can be given in any order
    -   Message structure:
        -   A newline character ends the header and starts the message - any remaining characters belong to the message
        -   The message can be empty
        -   Additional newline characters in the message do not terminate the message section, they are considered part of the message
-   Server packet arguments:
    -   `COMPLETED` - whether the sending of packets is completed or not
        -   Set to `T` or `F`
    -   `TOTAL_PACKETS` - the total number of packets
        -   Set to a number
    -   `SEQUENCE_NUM` - the sequence number of the current packet, starting from 0
        -   Set to a number
    -   Examples:
        -   `(92)COMPLETED:F,TOTAL_PACKETS:10,SEQUENCE_NUM:1\nHello world!\nThis is a packet sent from a server`
        -   `(93)COMPLETED:T,TOTAL_PACKETS:10,SEQUENCE_NUM:10\nHello world!\nThis is a packet sent from a server`
        -   `(92)COMPLETED:T,TOTAL_PACKETS:10,SEQUENCE_NUM:4\nHello world!\nThis is a packet sent from a server`
-   Client packet:
    -   The client sends 2 types of packets: the initial request packet and the follow-up packets, when it needs to indicate whether it has completed receiving all packets
        -   The initial request packet contains the following argument:
            -   `REQUEST_TYPE` – the type of request the client is making to the server
                -   Set to `MESSAGE` to receive a message from the server
                -   Other values can be used based on specific use-cases
        -   The follow-up packet uses the following arguments:
            -   `COMPLETED` – whether or not the client has received all packets
                -   Set to `T` or `F`
            -   `TOTAL_PACKETS_MISSING` – the number of packets missing
                -   Set to a number
                -   only included if `COMPLETED` is set to `F`
            -   `MISSING_PACKET_NUMS` – the list of the sequence numbers of the missing packets
                -   Set to the missing sequence numbers enclosed in square brackets `[]`, separated by commas, in any order
                -   only included if `COMPLETED` is set to `F`
            -   This is followed by a newline character to indicate the end of the header and packet
    -   Examples:
        -   `(12)COMPLETED:T\n`
        -   `(66)COMPLETED:F,TOTAL_PACKETS_MISSING:5,MISSING_PACKET_NUMS:[3,2,6,2]\n`
