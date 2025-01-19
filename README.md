# TCP Simulation

This project simulates communication between a __server__ and a __client__ using a TCP-like protocol. Once a connection is established, the server sends a file to the client in a sequence of packets. For the simulation, we 'drop' ~20% of packets sent by the server and rely on the protocol to recover from this so that the complete file is successfully received.

The details of the custom protocol are described [here](https://github.com/avromi-s/TCPSimulation-OperatingSystems/blob/main/protocol-description.md).

### To run the simulation:
-   Download and install the server and client applications (found in the [releases](https://github.com/avromi-s/TCPSimulation-OperatingSystems/releases); alternatively, you can build it from source)
    -   they can be installed on the same or separate computers
-   On the server application, select a local file and check off the 'Send file once connection is established' checkbox
    -   currently, only plain-text files are supported (e.g., `.txt`, `.csv`, `.json`, `.html`)
-   On the client application, enter the IP address where the server application is running:
    -   if you are running both applications on the same computer, use `127.0.0.1`
    -   if the applications are running on different networks, port forwarding will need to be configured on the *server* application's network to forward all incoming traffic for port `30121` to the local machine that is running the server application
-   Click the 'Receive File' button on the client application
-   After a file is finished sending, additional files can be sent in the same manner

|    Server application                                                                                                                      |            Client application                        |
|--------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------|
| <img width="282" alt="server-application-running" src="https://github.com/user-attachments/assets/7ac56625-6a1b-4aa8-9099-7ec91760388e" /> | <img width="282" alt="client-application-running" src="https://github.com/user-attachments/assets/c369754b-88d8-447f-a96f-5a530e9b7de9" /> |
