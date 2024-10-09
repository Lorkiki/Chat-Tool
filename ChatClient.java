import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;


public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    //HashMap to store public keys of users
    private static HashMap<String, PublicKey> userPublickeys = new HashMap<>();

    //Method to store a public key for a specific user
    public static void storePublicKey(String username, PublicKey publickey){
        userPublickeys.put(username, publickey);
        System.out.println("Public key for user " + username + " stored.");
        System.out.println(username);
        System.out.println(publickey);
    }

    // Method to retrieve a public key by username
    public static PublicKey getPublicKeyByUsername(String username) {
        PublicKey publicKey = userPublickeys.get(username);
        if (publicKey == null) {
            System.out.println("No public key found for user " + username);
        }
        return publicKey;
    }


    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to the server!");

            //generate keypair
            KeyPair keyPair = RSA.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();


            // Convert public key to a string to send to other users
            String publicKeyEncoded = RSA.getPublicKeyString(publicKey);



            // Start a thread to listen for messages from the server
            new Thread(new IncomingMessagesHandler(in,privateKey)).start();

            // Main thread to send user input to the server
            String userInput;
            while ((userInput = reader.readLine()) != null) {

                // If user send "#", send public key to other users
                // input should only be "#"
                if (userInput.startsWith("#")){
                    out.println("#: " + publicKeyEncoded);
                }

                // If user input starts with "@", send encrypt message to other users
                // Format of input should be @username: message
                else if  (userInput.startsWith("@")){
                    String[] parts = userInput.split(":");
                    String username = parts[0].trim().substring(1);
                    String message = parts[1].trim();

                    // Get public key of the recipient
                    PublicKey otherpublicKey = getPublicKeyByUsername(username);

                    // Encrypt message using recipient's public key
                    String encrypt = RSA.encrypt(message, otherpublicKey);

                    //send the encrypted message
                    out.println("@" + username+ ": " + encrypt);

                }

                // Otherwise, send a broadcast message
                else{
                    out.println(userInput);}
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class IncomingMessagesHandler implements Runnable {
    private BufferedReader in;
    private PrivateKey privateKey;

    // Constructor to initialize the input reader and the user's private key
    public IncomingMessagesHandler(BufferedReader in, PrivateKey privateKey) {
        this.in = in;
        this.privateKey = privateKey;
    }

    @Override
    public void run() {
        String message;
        try {
            // Continuously listen for incoming messages from the server
            while ((message = in.readLine()) != null) {

                // If the message contains a public key, store it
                if(message.startsWith("#")){
                    handlePublicKeyMessage(message);
                }

                // If the message is an encrypted private message
                // Decrypt it by using private key
                else if(message.startsWith("@")){
                    handlePrivateMessage(message);
                }

                //Otherwise, print the message
                else{
                    System.out.println(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading message: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Method to handle public keys from other users
    private void handlePublicKeyMessage(String message) {
        try {

            // Spilt the message to half, username and public key
            String[] parts = message.split(":");
            String username = parts[1].trim();
            String publicKeyBase64 = parts[2].trim();

            // Convert Base64 public key back to PublicKey object
            PublicKey publicKey = RSA.getPublicKeyFromString(publicKeyBase64);

            // Store the public key with the username
            ChatClient.storePublicKey(username, publicKey);

        } catch (Exception e) {
            System.out.println("Error handling public key message: " + e.getMessage());
        }
    }



    // Method to handle encrypted private messages
    private void handlePrivateMessage(String message) throws Exception {

        // Spilt the message to half, username and encrypt message
        String[] parts = message.split(":");
        String username = parts[0].trim();
        String privatemessage = parts[1].trim();

        // Decrypt the message using the user's private key
        System.out.println(username);

        // Display the decrypted message
        String decryptmessage = RSA.decrypt(privatemessage, privateKey);
        System.out.println(decryptmessage);

    }
}
