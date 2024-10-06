import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;

public class ChatBox {
    private JFrame chatFrame;
    private JTextArea chatTextArea;
    private JTextField messageTextField;
    private JButton sendButton;
    private String friendUsername;
    private View view;
    private String chatBoxId;
    private final ArrayList<String> messageHistory;

    private final Lock lock = new ReentrantLock();


    public ChatBox(String friendUsername, View view) {
        this.friendUsername = friendUsername;
        this.view = view;
        messageHistory = new ArrayList<>();


        this.chatBoxId = friendUsername;

        chatFrame = new JFrame("Chat with " + friendUsername);
        chatFrame.setBounds(100, 100, 400, 300);
        chatFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        chatFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                view.removeChatBox(friendUsername);
            }
        });

        chatTextArea = new JTextArea();
        chatTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatTextArea);

        messageTextField = new JTextField();
        sendButton = new JButton("Send");


        JButton forwardButton = new JButton("Forward");
        forwardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                promptAndForwardMessage();
            }
        });
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = messageTextField.getText();
                if (!message.isEmpty()) {
                    String newMessage = message + ": " + friendUsername;
                    safeClient.writeToServer(safeClient.getUsername(), newMessage);
                    appendMessage("You: " + message);
                    messageTextField.setText("");
                }

            }
        });

        chatFrame.setLayout(new BorderLayout());
        chatFrame.add(scrollPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(messageTextField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(forwardButton);
        chatFrame.add(buttonPanel, BorderLayout.NORTH);
        chatFrame.add(bottomPanel, BorderLayout.SOUTH);

        chatFrame.setVisible(true);
    }

    public List<String> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }

    public synchronized void appendMessage(String message) {
        chatTextArea.append(message + "\n");
        int colonIndex = message.indexOf(":");
        if (colonIndex != -1 && colonIndex < message.length() - 1) {
            String messageContent = message.substring(colonIndex + 2);
            messageHistory.add(messageContent);
        } else {
            messageHistory.add(message);
        }
    }


    public void appendMessageUnsafe(String message) {
        chatTextArea.append(message + "\n");
        int colonIndex = message.indexOf(":");
        if (colonIndex != -1 && colonIndex < message.length() - 1) {
            String messageContent = message.substring(colonIndex + 2);
            messageHistory.add(messageContent);
        } else {
            messageHistory.add(message);
        }
    }


    public void promptAndForwardMessage() {
        synchronized (messageHistory) {
            Object[] messageOptions = messageHistory.toArray();
            String selectedMessage = (String) JOptionPane.showInputDialog(chatFrame,
                    "Choose a message to forward:", "Forward Message", JOptionPane.PLAIN_MESSAGE,
                    null, messageOptions, messageOptions[0]);

            if (selectedMessage != null && !selectedMessage.isEmpty()) {
                forwardMessageSafe(selectedMessage);
            } else {
                // Handle case where user cancels or provides an empty message
                JOptionPane.showMessageDialog(chatFrame, "Invalid message to forward.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    public void promptAndForwardMessageUnsafe() {
        Object[] messageOptions = messageHistory.toArray();
        String selectedMessage = (String) JOptionPane.showInputDialog(chatFrame,
                "Choose a message to forward:", "Forward Message", JOptionPane.PLAIN_MESSAGE,
                null, messageOptions, messageOptions[0]);

        if (selectedMessage != null && !selectedMessage.isEmpty()) {
            forwardMessageUnsafe(selectedMessage);
        } else {
            JOptionPane.showMessageDialog(chatFrame, "Invalid message to forward.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    public void forwardMessageSafe(String message) {
        String recipient = view.promptUserForFriend(safeClient.getUsername(), message);
        if (recipient != null) {
            synchronized (messageHistory) { // Synchronize access to the message history list
                appendMessage("Forwarded: " + message);
            }
        }
    }


    public void forwardMessageUnsafe(String message) {
        String recipient = view.promptUserForFriend(safeClient.getUsername(), message);
        if (recipient != null) {
            new Thread(() -> appendMessageUnsafe("Forwarded: " + message)).start();
        }
    }



    public boolean saveMessage(String message, boolean test) {
        // Message files are saved in the format of "myname-friendname.json"
        // If the file does not exist, create it.
        String fileName;
        if (test) {
            fileName = safeClient.getUsername() + "-" + friendUsername + "PMTest.json";
        } else {
            fileName = safeClient.getUsername() + "-" + friendUsername + "PM.json";
        }
        File file = new File("messages/" + fileName);
        System.out.println("Current path: " + file.getAbsolutePath());

        if (!file.exists()) {
            boolean created;
            try {
                created = file.createNewFile();
            } catch (Exception ex) {
                System.out.println("Error creating file: " + ex.getMessage());
                created = false;
            }
            if (!created) {
                System.out.println("Error creating file: " + fileName);
                return false;
            }
        }

        // Write the message to the file
        try (FileWriter fileWriter = new FileWriter("messages/" + fileName, true)) {
            fileWriter.write(message + "\n");
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean LockedSaveFile(String message, boolean test) throws IOException {
        FileLock fileLock;
        // Message files are saved in the format of "myname-friendname.json"
        // If the file does not exist, create it.
        String fileName;
        if (test) {
            fileName =  safeClient.getUsername() + "-" + friendUsername + "PMTest.json";
        } else {
            fileName = safeClient.getUsername() + "-" + friendUsername + "PM.json";
        }

        File file = new File("messages/" + fileName);
        System.out.println("Current path: " + file.getAbsolutePath());

        if (!file.exists()) {
            boolean created;
            try {
                created = file.createNewFile();
            } catch (Exception ex) {
                System.out.println("Error creating file: " + ex.getMessage());
                created = false;
            }
            if (!created) {
                System.out.println("Error creating file: " + fileName);
                return false;
            }
        }

        // Restrict access to the file with this thread is performing writew
        try (FileChannel fileChannel = new RandomAccessFile(file, "rw").getChannel()) {
            fileLock = fileChannel.lock();
            // Write the message to the file
            try (FileWriter fileWriter = new FileWriter("messages/" + fileName, true)) {
                fileWriter.write(message + "\n");
            } catch (IOException e) {
                System.out.println("Error writing to file: " + e.getMessage());
                return false;
            }
        } catch (OverlappingFileLockException e) {
            System.out.println("File is already locked in this thread or virtual machine");
            return false;
        } catch (IOException e) {
            System.out.println("Error locking file: " + e.getMessage());
            return false;
        }
        return true;
    }

    public String getFriendUsername() {
        return friendUsername;
    }
}
