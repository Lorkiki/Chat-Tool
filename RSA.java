import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSA {
    
    // Method to generate RSA KeyPair (Public and Private keys)
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);  // Key size, e.g., 2048 bits
        return keyGen.generateKeyPair();
    }

    // Method to encrypt data using the public key
    public static String encrypt(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes); // Encode encrypted data as base64 for readability
    }

    // Method to decrypt data using the private key
    public static String decrypt(String encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }

    // Method to share public key as a Base64 encoded string
    public static String getPublicKeyString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    // Method to reconstruct a public key from a Base64 encoded string
    public static PublicKey getPublicKeyFromString(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    public static void main(String[] args) {
        try {
            // Generate RSA Key Pair
            KeyPair keyPair = generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // Print the generated public key
            System.out.println("Public Key: " + getPublicKeyString(publicKey));

            // Example of encrypting and decrypting a message
            String originalMessage = "Hello, this is a secret message!";
            System.out.println("Original Message: " + originalMessage);

            // Encrypt the message using the public key
            String encryptedMessage = encrypt(originalMessage, publicKey);
            System.out.println("Encrypted Message: " + encryptedMessage);

            // Decrypt the message using the private key
            String decryptedMessage = decrypt(encryptedMessage, privateKey);
            System.out.println("Decrypted Message: " + decryptedMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
