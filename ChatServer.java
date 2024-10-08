import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    
    public static void main(String[] args) {
        int port = 12345;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

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

    public ClientHandler(Socket socket, Set<ClientHandler> clientHandlers) {
        this.socket = socket;
        this.clientHandlers = clientHandlers;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Ask for username
            out.println("Enter your username: ");
            username = in.readLine();
            System.out.println(username + " has joined the chat.");

            // Notify others of the new connection
            broadcastMessage("Server: " + username + " has joined the chat.", this);

            // Send the list of currently connected users
            sendUserList();

            // Handle incoming messages
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("@")) {
                    sendPrivateMessage(message);
                } else if (message.startsWith("#")){
                    sendKey(message);
                }
                else {
                    broadcastMessage(username + ": " + message, this);
                }
            }
        } catch (IOException e) {
            System.out.println(username + " disconnected.");
        } finally {
            cleanup();
        }
    }

    private void broadcastMessage(String message, ClientHandler excludeUser) {
        for (ClientHandler client : clientHandlers) {
            if (client != excludeUser) {
                client.out.println(message);
            }
        }
    }

    private void sendPrivateMessage(String message) {
        String[] messageParts = message.split(":", 2);
        if (messageParts.length < 2) {
            out.println("Invalid private message format. Use @username message.");
            return;
        }

        String targetUsername = messageParts[0].substring(1); // Remove '@' from username
        String privateMessage = messageParts[1];


        boolean foundUser = false;
        for (ClientHandler client : clientHandlers) {
            if (client.username.equalsIgnoreCase(targetUsername)) {
                client.out.println("@Private from " + username + ": " + privateMessage);
                out.println("Private to " + targetUsername + ": " + privateMessage);
                foundUser = true;
                break;
            }
        }
        if (!foundUser) {
            out.println("User " + targetUsername + " not found.");
        }
    }


    private void sendKey(String message) {
        String[] messageParts = message.split(" ", 2);
        if (messageParts.length < 2) {
            out.println("Invalid key format. ");
            return;
        }

        String publickey = messageParts[1];


        for (ClientHandler client : clientHandlers) {

                client.out.println("#: " + username + ": "+  publickey);

        }

    }







    private void sendUserList() {
        out.println("Connected users:");
        for (ClientHandler client : clientHandlers) {
            out.println("- " + client.username);
        }
    }

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
