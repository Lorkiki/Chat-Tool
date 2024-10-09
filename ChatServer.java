import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {

    // Set to hold all connected client
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    
    public static void main(String[] args) {
        // Server port
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port: " + port);

            // Consistently accept new client connections
            while (true) {
                // Accept a new client connection
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                // Create a new client handler for the connected client
                ClientHandler clientHandler = new ClientHandler(socket, clientHandlers);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Set<ClientHandler> clientHandlers;
    private String username;

    // Constructor for initializing the client handler
    public ClientHandler(Socket socket, Set<ClientHandler> clientHandlers) {
        this.socket = socket;
        this.clientHandlers = clientHandlers;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Request the username from the client
            out.println("Enter your username: ");
            username = in.readLine();
            System.out.println(username + " has joined the chat.");

            // Notify all other clients about the new user joining
            broadcastMessage("Server: " + username + " has joined the chat.", this);

            // Send the list of currently connected users
            sendUserList();

            // Listen for incoming messages from the client
            String message;
            while ((message = in.readLine()) != null) {
                // Check if the message is a private message (starts with '@')
                if (message.startsWith("@")) {
                    sendPrivateMessage(message);
                }

                // Check if the message is a public key (starts with '#')
                else if (message.startsWith("#")){
                    sendKey(message);
                }

                // Otherwise, broadcast the message to all clients
                else {
                    broadcastMessage(username + ": " + message, this);
                }
            }
        } catch (IOException e) {
            System.out.println(username + " disconnected.");
        } finally {
            cleanup(); // Clean up when the client disconnects
        }
    }

    // Method to broadcast a message to all connected clients except the sender
    private void broadcastMessage(String message, ClientHandler excludeUser) {
        for (ClientHandler client : clientHandlers) {
            if (client != excludeUser) {
                client.out.println(message);
            }
        }
    }

    // Method to send a private message to a specific client
    private void sendPrivateMessage(String message) {
        // Split the message into username and message by ":"
        String[] messageParts = message.split(":", 2);
        if (messageParts.length < 2) {
            out.println("Invalid private message format. Use @username: message.");
            return;
        }
        String targetUsername = messageParts[0].substring(1); // Remove '@' from username
        String privateMessage = messageParts[1];

        // Flag to check if the target user is found
        boolean foundUser = false;
        for (ClientHandler client : clientHandlers) {
            // Check if the recipient's username matches
            if (client.username.equalsIgnoreCase(targetUsername)) {
                client.out.println("@Private from " + username + ": " + privateMessage);
                out.println("Private to " + targetUsername + ": " + privateMessage);
                foundUser = true;
                break;
            }
        }

        // If the target user was not found
        if (!foundUser) {
            out.println("User " + targetUsername + " not found.");
        }
    }


    // Method to send a public key to all connected clients
    private void sendKey(String message) {
        String[] messageParts = message.split(" ", 2);
        if (messageParts.length < 2) {
            out.println("Invalid key format. ");
            return;
        }

        // Extract the public key
        String publickey = messageParts[1];

        // Broadcast the public key to all connected clients
        for (ClientHandler client : clientHandlers) {
                client.out.println("#: " + username + ": "+  publickey);
        }
    }






    // Method to send the list of currently connected users to the client
    private void sendUserList() {
        out.println("Connected users:");
        for (ClientHandler client : clientHandlers) {
            out.println("- " + client.username);
        }
    }


    // Method to clean up resources when a client disconnects
    private void cleanup() {
        try {
            clientHandlers.remove(this);
            socket.close();
            System.out.println(username + " has left the chat.");
            broadcastMessage("Server: " + username + " has left the chat.", this);
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }
}
