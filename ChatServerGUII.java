package Working_Send_Text_file;


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

public class ChatServerGUII {
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


    public static void broadcastFile( ClientHandler sender) {
        userSessions.forEach((username, handler) -> {
            if (handler != sender) {
                handler.sendFile();
            }
        });
    }











    // Call this method when a client disconnects to notify the server GUI
    public static void notifyDisconnection(String username) {
        serverGUI.displayMessage(username + " has disconnected.");
    }


}

class ClientHandler extends Thread {
    private Socket socket;
    private String username;
    private DataOutputStream outToClient;
    private DataInputStream dataInputStream;
    private ChatServerGUI serverGUI;

    public ClientHandler(Socket socket, ChatServerGUI serverGUI) {
        this.socket = socket;
        this.serverGUI = serverGUI;
    }

    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            outToClient = new DataOutputStream(socket.getOutputStream());

            outToClient.writeBytes("Enter username:\n");
            this.username = reader.readLine();
            outToClient.writeBytes("Enter password:\n");
            String password = reader.readLine();

            if (authenticateUser(username, password)) {
                ChatServerGUII.userSessions.put(username, this);
                serverGUI.displayMessage(username + " has joined the chat.");
                outToClient.writeBytes("Authentication successful. You can start chatting!\n");

                String clientMessage;
                while (!(clientMessage = reader.readLine()).equalsIgnoreCase("quit")) {

                    if (clientMessage.equalsIgnoreCase("Send_File")) {
                        String filePath = reader.readLine();
                        long fileSize = Long.parseLong(reader.readLine());
                        FileOutputStream fileOut = new FileOutputStream("received_file");
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        long totalRead = 0;

                        while (totalRead < fileSize && (bytesRead = input.read(buffer)) != -1) {
                            fileOut.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                        }
                        fileOut.close();
                        System.out.println("File received and saved.");





                        ChatServerGUII.broadcastFile(this);




                    } else {


                        String messageToSend = this.username + ": " + clientMessage;
                        ChatServerGUII.broadcastMessage(messageToSend, this);
                    }
                }

                // When the client sends 'quit', notify the server GUI
                serverGUI.displayMessage(username + " has left the chat.");

            } else {
                outToClient.writeBytes("Authentication failed. Connection will be closed.\n");
            }
        } catch (IOException ex) {
            serverGUI.displayMessage(username + " has disconnected due to an error.");
        } finally {
            // Notify the server GUI of the disconnection in the finally block
            ChatServerGUII.notifyDisconnection(username);
            ChatServerGUII.userSessions.remove(username);
            try {
                socket.close();
            } catch (IOException e) {
                serverGUI.displayMessage("Error closing the socket for user " + username);
                e.printStackTrace();
            }
        }
    }

    private boolean authenticateUser(String username, String password) {
        return ("Lin".equals(username) || "Jin".equals(username)) && "123".equals(password);
    }

    public void sendMessage(String message) {
        try {
            outToClient.writeBytes(message + "\n");
        } catch (IOException e) {
            serverGUI.displayMessage("Failed to send message to " + username);
            e.printStackTrace();
        }
    }

    public void sendFile() {
        String workingDir = System.getProperty("user.dir");
        String filePath = workingDir + "/received_file";
        File file = new File(filePath);
        long fileSize = file.length();
        try {

            outToClient.writeBytes("Send_File\n");
            outToClient.writeBytes(filePath + "\n");
            outToClient.writeBytes(fileSize + "\n"); // Send the file size
            FileInputStream fileIn = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) != -1) {
                outToClient.write(buffer, 0, bytesRead);
            }

            fileIn.close();
            System.out.println("File sent to the server.");

            System.out.println(filePath);
            // Flush the stream to ensure all data is sent
            outToClient.flush();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
