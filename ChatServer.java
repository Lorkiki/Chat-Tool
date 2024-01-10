package SendFile;


import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

class ChatServerGUI extends JFrame {
    private JTextArea textArea;

    public ChatServerGUI() {
        textArea = new JTextArea(20, 50);
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        setTitle("Chat Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    public void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> textArea.append(message + "\n"));
    }
}

public class ChatServer {
    public static final ConcurrentHashMap<String, ClientHandler> userSessions = new ConcurrentHashMap<>();
    private static ChatServerGUI serverGUI;

    public static void main(String[] args) throws UnknownHostException {
        serverGUI = new ChatServerGUI();
        String ip = InetAddress.getLocalHost().getHostAddress();
        int port = 6799;
        serverGUI.displayMessage("Chat Server is listening\n" +
                                "IP address: " + ip + "\n" +
                                "Port: " + port);


        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                serverGUI.displayMessage("Client connected from: " + socket.getInetAddress());
                new ClientHandler(socket, serverGUI).start();
            }
        } catch (IOException ex) {
            serverGUI.displayMessage("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void broadcastMessage(String message, ClientHandler sender) {
        userSessions.forEach((username, handler) -> {
            if (handler != sender) {
                handler.sendMessage(message);
            }
        });
    }


    public static void broadcastFile( ClientHandler sender, int dataType, String fileName, byte[] fileData ) {
        userSessions.forEach((username, handler) -> {
            if (handler != sender) {
                try {
                    handler.sendFile(  dataType,  fileName, fileData );
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    // Call this method when a client disconnects to notify the server GUI
    public static void notifyDisconnection(String username) {
        serverGUI.displayMessage(username + " has disconnected.");
    }


}

class ClientHandler extends Thread implements Runnable {
    private Socket socket;
    private String username;
    private DataOutputStream outToClient;
    private ChatServerGUI serverGUI;

    public ClientHandler(Socket socket, ChatServerGUI serverGUI) {
        this.socket = socket;
        this.serverGUI = serverGUI;
    }

    public void run() {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream())))
           {

            outToClient = new DataOutputStream(socket.getOutputStream());

            outToClient.writeInt(1);
            outToClient.writeUTF("Enter username:\n");
            dis.readInt();
            this.username = dis.readUTF().trim();
               if (!ChatServer.userSessions.containsKey(this.username)) {
                   ChatServer.userSessions.put(this.username, this);
                   serverGUI.displayMessage(username + " has joined the chat.");
               } else {
                   // Handle duplicate username case
                   outToClient.writeUTF("Username already taken. Please reconnect with a different username.\n");
                   return; // Exit the thread
               }


               while (true) {
                    int dataType = dis.readInt();
                    if (dataType == 1) {
                        // Text message
                        String message = dis.readUTF();
                        String messageToSend = this.username + ": " + message;
                        System.out.println(messageToSend);
                        ChatServer.broadcastMessage(messageToSend, this);
                    }
                    else if (dataType == 2) {
                        // File
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                            baos.write(buffer, 0, bytesRead);
                            fileSize -= bytesRead;
                        }
                        ChatServer.broadcastFile( this,2,fileName, baos.toByteArray());

                    }

                }


        } catch (IOException ex) {
            serverGUI.displayMessage(username + " has disconnected due to an error.");
        } finally {
            // Notify the server GUI of the disconnection in the finally block
            ChatServer.notifyDisconnection(username);
            ChatServer.userSessions.remove(username);
            try {
                socket.close();
            } catch (IOException e) {
                serverGUI.displayMessage("Error closing the socket for user " + username);
                e.printStackTrace();
            }
        }
    }



    public void sendMessage(String message) {
        try {
            outToClient.writeInt(1);
            outToClient.writeUTF(message + "\n");
        } catch (IOException e) {
            serverGUI.displayMessage("Failed to send message to " + username);
            e.printStackTrace();
        }
    }



    public void sendFile( int dataType, String fileName, byte[] fileData ) throws IOException {
        outToClient.writeInt(dataType);
        outToClient.writeUTF(fileName);
        outToClient.writeLong(fileData.length);
        outToClient.write(fileData);

        }

    }

