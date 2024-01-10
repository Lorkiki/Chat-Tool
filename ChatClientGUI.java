package SendFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.nio.*;

public class ChatClientGUI extends JFrame implements ActionListener {
    private JTextArea chatArea;
    private JTextField inputField;
    private JTextField filePathField;  // Field to input the file path
    private JButton sendButton;
    private JButton sendFileButton;    // Button to send the file
    private JButton browseButton;      // Button to browse for the file

    private DataOutputStream outToServer;
    private DataInputStream inFromServer;
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
        chatArea = new JTextArea(16, 50);
        chatArea.setEditable(false);
        inputField = new JTextField(50);
        filePathField = new JTextField(40);
        sendButton = new JButton("Send");
        sendFileButton = new JButton("Send File");
        browseButton = new JButton("Browse...");

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

        sendButton.addActionListener(this);
        inputField.addActionListener(this);
        browseButton.addActionListener(this);
        sendFileButton.addActionListener(this);

        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private void connectToServer() throws IOException {
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
                inFromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

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
        Thread readThread = new Thread(() -> {
            try {
                while (true) {
                    int dataType = inFromServer.readInt();
                    if (dataType == 1)
                     {
                         String message = inFromServer.readUTF();
                         chatArea.append(message + "\n");
                         System.out.println("Message from server: " + message);
                     }
                    else if (dataType == 2) {
                        // File
                        String fileName = inFromServer.readUTF();
                        long fileSize = inFromServer.readLong();
                        FileOutputStream fos = new FileOutputStream(fileName);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while (fileSize > 0 && (bytesRead = inFromServer.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            fileSize -= bytesRead;
                        }
                        fos.close();
                        System.out.println("Received file: " + fileName);
                    }
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
        if (e.getSource() == sendButton || e.getSource() == inputField) {
            try {
                String message = inputField.getText();
                outToServer.writeInt(1);
                outToServer.writeUTF(message + "\n");
                chatArea.append("You: " + message + "\n");
                inputField.setText("");
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
            String filePath = filePathField.getText();

            try {
                File file = new File(filePath);
                FileInputStream fis = new FileInputStream(file);
                outToServer.writeInt(2); // Sending a file
                outToServer.writeUTF(file.getName());
                outToServer.writeLong(file.length());
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    outToServer.write(buffer, 0, bytesRead);
                }
                fis.close();
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
