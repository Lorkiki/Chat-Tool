package Working_Send_Text_file;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;


public class ChatClientGUI extends JFrame implements ActionListener {
    private JTextArea chatArea;
    private JTextField inputField;
    private JTextField filePathField;  // Field to input the file path
    private JButton sendButton;
    private JButton sendFileButton;    // Button to send the file
    private JButton browseButton;      // Button to browse for the file
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;
    private Socket socket;


    public ChatClientGUI() throws IOException {
        createGUI();
        try {
            connectToServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        startReadingFromServer();
    }

    private void createGUI() {
        // Create GUI components
        chatArea = new JTextArea(16, 50);
        chatArea.setEditable(false);
        inputField = new JTextField(50);
        filePathField = new JTextField(40);  // Adjust the size as needed
        sendButton = new JButton("Send");
        sendFileButton = new JButton("Send File");  // Button to send the file
        browseButton = new JButton("Browse...");    // Button to open file chooser

        // Layout
        setLayout(new BorderLayout());
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        filePanel.add(filePathField);
        filePanel.add(browseButton);
        filePanel.add(sendFileButton);

        add(inputPanel, BorderLayout.SOUTH);
        add(filePanel, BorderLayout.NORTH);

        // Listeners
        sendButton.addActionListener(this);
        inputField.addActionListener(this);
        browseButton.addActionListener(this);       // Add listener for the browse button
        sendFileButton.addActionListener(this);     // Add listener for the send file button

        // Final setup
        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private void connectToServer() throws IOException {
        // Prompt for server IP and port
        String ip = JOptionPane.showInputDialog(
                this,
                "What IP address do you want to connect to?",
                "Server IP Address",
                JOptionPane.QUESTION_MESSAGE
        );
        String portString = JOptionPane.showInputDialog(
                this,
                "What port number do you want to use?",
                "Server Port Number",
                JOptionPane.QUESTION_MESSAGE
        );

        if (ip != null && portString != null && !ip.isEmpty() && !portString.isEmpty()) {
            try {
                int port = Integer.parseInt(portString);
                socket = new Socket(ip, port);
                outToServer = new DataOutputStream(socket.getOutputStream());


                inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                chatArea.append("Connected to the server at " + ip + ":" + port + "\n");
            } catch (NumberFormatException nfe) {
                chatArea.append("The port number must be an integer.\n");
            } catch (IOException e) {
                chatArea.append("Could not connect to the server at " + ip + ":" + portString + "\n");
                e.printStackTrace();
            }
        } else {
            chatArea.append("Connection cancelled by user.\n");
        }



    }


    private void startReadingFromServer() throws IOException {
        // Read messages from server
        InputStream input = socket.getInputStream();
        Thread readThread = new Thread(() -> {
            String message;
            try {
                while ((message = inFromServer.readLine()) != null) {

                     if (message.equalsIgnoreCase("Send_File"))
                     {

                     String filePath = inFromServer.readLine();
                     long fileSize = Long.parseLong(inFromServer.readLine());
                     FileOutputStream fileOut = new FileOutputStream("received_file");
                     byte[] new_buffer = new byte[1024];
                     int new_bytesRead;
                     long totalRead = 0;



                     while (totalRead < fileSize && (new_bytesRead = input.read(new_buffer)) != -1) {
                     fileOut.write(new_buffer, 0, new_bytesRead);
                     totalRead += new_bytesRead;
                     }
                     fileOut.close();
                     System.out.println("File received and saved.");




                     }else


                    chatArea.append(message + "\n");


                }
            } catch (IOException e) {
                chatArea.append("Disconnected from the server\n");
                e.printStackTrace();
            }
        });
        readThread.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Send message to server
        if (e.getSource() == sendButton || e.getSource() == inputField) {
            try {
                String message = inputField.getText();
                outToServer.writeBytes(message + "\n");
                chatArea.append("You: " + message + "\n");

                inputField.setText("");

                if ("quit".equalsIgnoreCase(message)) {
                    socket.close();
                    System.exit(0);
                }
            } catch (IOException ex) {
                chatArea.append("Error sending message to the server\n");
                ex.printStackTrace();
            }
        }else if (e.getSource() == browseButton) {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        } else if (e.getSource() == sendFileButton) {
            // Implement the file sending logic here
            // For example, read the file and send it over the socket
            String filePath = filePathField.getText();

            File file = new File(filePath);
            long fileSize = file.length();
            try {

                outToServer.writeBytes("Send_File\n");
                outToServer.writeBytes(filePath + "\n");
                outToServer.writeBytes(fileSize + "\n"); // Send the file size
                FileInputStream fileIn = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    outToServer.write(buffer, 0, bytesRead);
                }

                fileIn.close();
                System.out.println("File sent to the server.");

                // Flush the stream to ensure all data is sent
                outToServer.flush();
                JOptionPane.showMessageDialog(ChatClientGUI.this, "File send", "File send ", JOptionPane.ERROR_MESSAGE);



            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    public static void main(String[] args) throws IOException {
        new ChatClientGUI();
    }
}
