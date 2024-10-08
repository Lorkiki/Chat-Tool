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
    private static HashMap<String, PublicKey> userPublickeys = new HashMap<>();

    public static void storePublicKey(String username, PublicKey publickey){
        userPublickeys.put(username, publickey);
        System.out.println("Public key for user " + username + " stored.");
        System.out.println(username);
        System.out.println(publickey);
    }

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


            // Send the public key to the server with a username
            String publicKeyEncoded = RSA.getPublicKeyString(publicKey);



            // Start a thread to listen for messages from the server
            new Thread(new IncomingMessagesHandler(in,privateKey)).start();

            // Main thread to send user input to the server
            String userInput;
            while ((userInput = reader.readLine()) != null) {

                if (userInput.startsWith("#")){
                    out.println("#: " + publicKeyEncoded);
                }



                else if  (userInput.startsWith("@")){

                    String[] parts = userInput.split(":");
                    String username = parts[0].trim().substring(1);
                    String message = parts[1].trim();




                    PublicKey otherpublicKey = getPublicKeyByUsername(username);






                    String encrypt = RSA.encrypt(message, otherpublicKey);

                    out.println("@" + username+ ": " + encrypt);

                }




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

    public IncomingMessagesHandler(BufferedReader in, PrivateKey privateKey) {
        this.in = in;
        this.privateKey = privateKey;
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = in.readLine()) != null) {

                if(message.startsWith("#")){
                    handlePublicKeyMessage(message);
                }

                else if(message.startsWith("@")){
                    handlePrivateMessage(message);
                }

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


    // Method to handle public key messages
    private void handlePublicKeyMessage(String message) {
        try {

            // Format: PUBLICKEY:<username>:<public_key_base64>
            String[] parts = message.split(":");
            String username = parts[1].trim();
            String publicKeyBase64 = parts[2].trim();

            PublicKey publicKey = RSA.getPublicKeyFromString(publicKeyBase64);

            // Store the public key with the username
            ChatClient.storePublicKey(username, publicKey);



        } catch (Exception e) {
            System.out.println("Error handling public key message: " + e.getMessage());
        }
    }



    private void handlePrivateMessage(String message) throws Exception {
        String[] parts = message.split(":");
        String username = parts[0].trim();
        String privatemessage = parts[1].trim();

        System.out.println(username);


        String decryptmessage = RSA.decrypt(privatemessage, privateKey);
        System.out.println(decryptmessage);










    }
}
