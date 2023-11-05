# OperatingSystems-TCPSimulation

This program simulates communication between a Server and Client using a TCP-like protocol. 
-   The Server application allows selection of a .csv or .txt file by the user. Once a file is selected and the "Send file once connection is established" checkbox is selected, the Server application launches a background task to send the file selected to a Client
-   The Client application allows the user to specify an IPv4 address to connect to, and once the user clicks the "Receive File" button, attempts to connect and receive a file from a Server application at that IP
-   After a file is finished sending (successfully or not), additional files can be sent in the same manner

The Server application and Client application are intended to be ran independently of each other. 
***
The protocol for communication is described below:

-   Protocol Description
    -   The protocol allows packets to arrive out of order and recover from lost packets as follows:
        1.  The message is broken down into 100-character packets (excluding headers) with the number of packets depending on the size of the message
        2.  The server sends all of the packets once to the client, finishing with the last packet which indicates that it is such
        3.  Upon receival of the last packet, the client sends a message back to the server either indicating that all packets were successfully received, or that it is missing some packets
            -  If packets are missing, the client indicates the number of packets missing and their sequence numbers
            -  The server then sends (only) the indicated packets once, again indicating the last packet when it is sent
            -  The protocol continues from step 3 again
-   Packet Structure
    -   Syntax
        -   All packets start with a length indicator that gives the length of the packet contents (args and message), enclosed in parentheses. The length of the indicator itself is not included
        -   Headers contain key-value pairs as arguments
        -   Arguments are encoded with the following structure:
            -   `KEY:VALUE`
            -   Arguments are not case sensitive, but should be expressed in capital letters
        -   Multiple arguments are separated by commas
        -   Arguments can be given in any order
        -   A newline character ends the header
        -   Any characters following the newline character start the message, if applicable
            -   This includes any other newline characters
    -   The server packet message (excluding headers) is limited to a size of 100 characters
        -   The arguments are:
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
    -   The client sends 2 types of packets: the initial request packet and the follow-up packets, when it needs to indicate if it has completed receiving all packets
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
