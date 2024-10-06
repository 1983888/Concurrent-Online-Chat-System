import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class View implements safeClient.FileReceivedListener {
    private JFrame display;
    private JPanel userPanel;
    private JPanel friendPanel;
    private JPanel schedulePanel;
    private JPanel fileTransferPanel;
    private static JLabel usernameLabel;
    private Map<String, JButton> friendButtons;
    private JButton scheduleButton;
    private ActionListener addFriendButtonListener;
    private static Map<String, ChatBox> openChatBoxes = new HashMap<>();
    private JButton sendFileButton;
    private JLabel selectedFileLabel;
    private JFileChooser fileChooser;
    private Map<String, File> receivedFiles = new HashMap<>();
    private JPanel fileListPanel;
    private final Lock lock = new ReentrantLock();


    public View() {
        display = new JFrame("Chat");
        display.setBounds(100, 100, 320, 550);
        display.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        usernameLabel = new JLabel("Username: Not set");
        display.add(usernameLabel, BorderLayout.NORTH);

        friendButtons = new HashMap<>();

        userPanel = new JPanel();
        userPanel.setLayout(new GridLayout(0, 1));

        friendPanel = new JPanel();
        friendPanel.setLayout(new GridLayout(0, 1));

        schedulePanel = new JPanel();
        schedulePanel.setLayout(new FlowLayout());

        initializeFileTransferPanel();

        fileChooser = new JFileChooser();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Friends", friendPanel);
        tabbedPane.addTab("Add Users", userPanel);
        tabbedPane.addTab("Message Scheduling", schedulePanel);
        tabbedPane.addTab("File Sharing", fileTransferPanel);

        display.add(tabbedPane);
        addScheduleButton();
        loadExistingFiles();

        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<JLabel> friendLabels = getFriendLabels();
                String selectedFriend = showFriendSelectionDialog(friendLabels);
                if (selectedFriend != null) {
                    openFileChooserAndSendFile(selectedFriend);
                } else {
                    JOptionPane.showMessageDialog(display, "No friend selected.", "Send File", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        safeClient.setFileReceivedListener(this);
    }

    @Override
    public void onFileReceived(String fileName, File file) {
        SwingUtilities.invokeLater(() -> {
            addReceivedFile(fileName, file);
        });
    }

    public void addReceivedFile(String fileName, File file) {
        receivedFiles.put(fileName, file);
        refreshFileListPanel();
    }

    private void initializeFileTransferPanel() {
        fileTransferPanel = new JPanel();
        fileTransferPanel.setLayout(new BorderLayout());

        // Sub panel for sending files
        JPanel sendFileSubPanel = new JPanel();
        sendFileSubPanel.setLayout(new FlowLayout());
        sendFileButton = new JButton("Send File");
        selectedFileLabel = new JLabel("No file selected");
        sendFileSubPanel.add(sendFileButton);
        sendFileSubPanel.add(selectedFileLabel);
        fileTransferPanel.add(sendFileSubPanel, BorderLayout.NORTH);

        // Sub panel for listing received files
        JPanel fileListSubPanel = new JPanel();
        fileListSubPanel.setLayout(new BorderLayout());

        JLabel fileListHeadingLabel = new JLabel("Files shared with you");
        fileListHeadingLabel.setFont(new Font("Arial", Font.BOLD, 14));
        fileListSubPanel.add(fileListHeadingLabel, BorderLayout.NORTH);

        fileListPanel = new JPanel();
        fileListPanel.setLayout(new BoxLayout(fileListPanel, BoxLayout.Y_AXIS));
        JScrollPane fileListScrollPane = new JScrollPane(fileListPanel);
        fileListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        fileListSubPanel.add(fileListScrollPane, BorderLayout.CENTER);

        fileTransferPanel.add(fileListSubPanel, BorderLayout.CENTER);

        // to display the initial file list
        refreshFileListPanel();
    }

    private void refreshFileListPanel() {
        fileListPanel.removeAll();
        fileListPanel.revalidate();
        fileListPanel.repaint();

        for (Map.Entry<String, File> entry : receivedFiles.entrySet()) {
            String fileName = entry.getKey();
            File file = entry.getValue();

            JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            filePanel.add(new JLabel(fileName));

            JButton viewButton = new JButton("View File");
            viewButton.addActionListener(e -> viewFile(file));
            filePanel.add(viewButton);

            fileListPanel.add(filePanel);
        }
        fileListPanel.revalidate();
        fileListPanel.repaint();
    }

    private void loadExistingFiles() {
        File directory = new File("client_files");
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    receivedFiles.put(file.getName(), file);
                }
            }
        }
        refreshFileListPanel();
    }

    private void viewFile(File file) {
        String fileName = file.getName().toLowerCase();

        // Check if the file is an image
        if (isImageFile(fileName)) {
            displayImage(file);
        }
        // Check if the file is a video
        else if (isVideoFile(fileName)) {
            VideoPlayer.displayVideo(file);
        }
        // Handling other file types (e.g., text, PDF)
        else {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    JOptionPane.showMessageDialog(display, "File type not supported for viewing.",
                            "View File", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(display, "Error opening file: " + e.getMessage(),
                        "File Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private boolean isImageFile(String fileName) {
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        return Arrays.stream(imageExtensions).anyMatch(fileName::endsWith);
    }

    private boolean isVideoFile(String fileName) {
        String[] videoExtensions = {".mp4", ".avi", ".mkv", ".mov"};
        return Arrays.stream(videoExtensions).anyMatch(fileName::endsWith);
    }

    private String showFriendSelectionDialog(List<JLabel> friendLabels) {
        Object[] possibleValues = friendLabels.stream().map(JLabel::getText).toArray();
        Object selectedValue = JOptionPane.showInputDialog(display, "Choose a friend to send the file:",
                "Select Friend", JOptionPane.INFORMATION_MESSAGE,
                null, possibleValues, possibleValues[0]);
        return selectedValue != null ? selectedValue.toString() : null;
    }

    private void openFileChooserAndSendFile(String selectedFriend) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(display);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            selectedFileLabel.setText(selectedFile.getName());
            try {
                safeClient.sendFile(selectedFile.getAbsolutePath(), selectedFriend);
            } catch (IOException ioException) {
                JOptionPane.showMessageDialog(display, "Error sending file: " + ioException.getMessage(),
                        "File Transfer Error", JOptionPane.ERROR_MESSAGE);
                ioException.printStackTrace();
            }
        }
    }

    public static void playVideo(File file) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.open(file);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error playing video: " + e.getMessage(),
                        "Video Playback Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Desktop is not supported, cannot play video.",
                    "Video Playback Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    public static void setUsername(String username) {
        usernameLabel.setText("Username: " + username);
    }

    public void displayUser(String username) {
        JButton friendButton = new JButton("Add Friend");
        friendButtons.put(username, friendButton);

        friendButton.setActionCommand(username);

        friendButton.addActionListener(e ->
                addFriendButtonListener.actionPerformed
                        (new ActionEvent(friendButton, ActionEvent.ACTION_PERFORMED, username)));

        userPanel.add(new JLabel(username));
        userPanel.add(friendButton);
    }

    public void displayFriends(List<String> friends) {
        friendPanel.removeAll();

        for (String friend : friends) {
            JButton privateMessageButton = new JButton("Private Message");
            privateMessageButton.setActionCommand(friend);

            privateMessageButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String friendUsername = e.getActionCommand();
                    openChatBox(friendUsername);
                }
            });

            friendPanel.add(new JLabel(friend));
            friendPanel.add(privateMessageButton);
        }
    }

    public void removeUser(String username) {
        JButton friendButton = friendButtons.get(username);
        if (friendButton != null) {
            Component[] components = userPanel.getComponents();
            for (Component component : components) {
                if (component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    if (label.getText().equals(username)) {
                        userPanel.remove(label);
                        break;
                    }
                }
            }
            userPanel.remove(friendButton);
            friendButtons.remove(username);
        }
    }

    public void setAddFriendButtonListener(ActionListener listener) {
        addFriendButtonListener = listener;
    }

    public void displayMessage(String message) {
        JOptionPane.showMessageDialog(null, message, "Alert", JOptionPane.INFORMATION_MESSAGE);
    }

    public void setVisible(boolean visible) {
        display.setVisible(visible);
    }

    public void openChatBox(String friendUsername) {
        String chatBoxId = friendUsername;
        if (!openChatBoxes.containsKey(chatBoxId)) {
            ChatBox chatBox = new ChatBox(friendUsername, this);
            openChatBoxes.put(chatBoxId, chatBox);
        }
    }

    public static void displayMessageInChatBox(String senderUsername, String message) {
        ChatBox chatBox = openChatBoxes.get(senderUsername);
        if (chatBox != null) {
            chatBox.appendMessage(senderUsername + ": " +  message);
        }
        // Save the message to the chat history
        try {
            assert chatBox != null;
            chatBox.LockedSaveFile(senderUsername + ": " + message, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeChatBox(String friendUsername) {
        openChatBoxes.remove(friendUsername);
    }

    private void addScheduleButton() {
        scheduleButton = new JButton("Schedule Message");
        schedulePanel.add(scheduleButton);
        scheduleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSchedulePanel(); // Method to handle opening the scheduling panel
            }
        });
    }

    private void openSchedulePanel() {
        int delayInMinutes;
        JDialog dialog = new JDialog(display, "Schedule Message", true);
        dialog.setLayout(new BorderLayout());

        JPanel scheduleOptionsPanel = new JPanel();
        scheduleOptionsPanel.setLayout(new GridLayout(0, 2));

        JSpinner delaySpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        JComboBox<String> timeUnitComboBox = new JComboBox<>(new String[]{"Seconds", "Minutes", "Hours"});

        scheduleOptionsPanel.add(new JLabel("Delay:"));
        scheduleOptionsPanel.add(delaySpinner);

        scheduleOptionsPanel.add(new JLabel("Time Unit:"));
        scheduleOptionsPanel.add(timeUnitComboBox);

        JLabel friendLabel = new JLabel("Select Friend:");
        scheduleOptionsPanel.add(friendLabel);
        scheduleOptionsPanel.add(new JLabel()); // Placeholder for alignment

        List<JLabel> friendLabels = getFriendLabels();

        // Add radio buttons for each friend
        ButtonGroup friendGroup = new ButtonGroup();
        for (JLabel friend : friendLabels) {
            JRadioButton radioButton = new JRadioButton(friend.getText());
            friendGroup.add(radioButton);
            scheduleOptionsPanel.add(radioButton);
            scheduleOptionsPanel.add(new JLabel()); // Placeholder for alignment
        }

        JTextField messageField = new JTextField(20);
        JButton scheduleButton = new JButton("Schedule");
        scheduleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedFriend = getSelectedFriend(scheduleOptionsPanel);
                if (selectedFriend != null) {
                    String message = messageField.getText();
                    int delayValue = (int) delaySpinner.getValue();
                    String timeUnit = (String) timeUnitComboBox.getSelectedItem();

                    long delayInSeconds;
                    switch (timeUnit) {
                        case "Seconds":
                            delayInSeconds = delayValue;
                            break;
                        case "Minutes":
                            delayInSeconds = delayValue * 60;
                            break;
                        case "Hours":
                            delayInSeconds = delayValue * 3600;
                            break;
                        default:
                            delayInSeconds = 0;
                    }

                    scheduleMessage(selectedFriend, message, delayInSeconds);
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(null, "Please select a friend.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Add components to the panel
        scheduleOptionsPanel.add(new JLabel("Enter Message:"));
        scheduleOptionsPanel.add(messageField);
        scheduleOptionsPanel.add(scheduleButton);

        dialog.add(scheduleOptionsPanel, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(display);
        dialog.setVisible(true);
    }

    private List<JLabel> getFriendLabels() {
        Component[] components = friendPanel.getComponents();
        List<JLabel> friendLabels = new ArrayList<>();
        for (Component component : components) {
            if (component instanceof JLabel) {
                friendLabels.add((JLabel) component);
            }
        }
        return friendLabels;
    }

    private String getSelectedFriend(JPanel scheduleOptionsPanel) {
        Component[] components = scheduleOptionsPanel.getComponents();
        for (Component component : components) {
            if (component instanceof JRadioButton) {
                JRadioButton radioButton = (JRadioButton) component;
                if (radioButton.isSelected()) {
                    return radioButton.getText();
                }
            }
        }
        return null;
    }

    public void scheduleMessage(String friend, String message, long delayInSeconds){
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                openChatBoxWithMessage(friend, message);
            }
        };
        scheduler.schedule(task, delayInSeconds, TimeUnit.SECONDS);
        scheduler.shutdown();
    }

    public void scheduleMessageUnsafe(String friend, String message, long delayInSeconds) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                openChatBoxWithMessageUnsafe(friend, message);
            }
        };
        scheduler.schedule(task, delayInSeconds, TimeUnit.SECONDS);
        scheduler.shutdown();
    }

    public synchronized void openChatBoxWithMessage(String friend, String message) {
        if (!openChatBoxes.containsKey(friend)) {
            ChatBox chatBox = new ChatBox(friend, this);
            openChatBoxes.put(friend, chatBox);
        }
        ChatBox chatBox = openChatBoxes.get(friend);
        String newMessage = message + ": " + friend;
        safeClient.writeToServer(safeClient.getUsername(), newMessage);
        chatBox.appendMessage("Scheduled message: " + message);
    }

    public void openChatBoxWithMessageUnsafe(String friend, String message) {
        // Simulate a delay before opening the chat box
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!openChatBoxes.containsKey(friend)) {
            ChatBox chatBox = new ChatBox(friend, this);
            openChatBoxes.put(friend, chatBox);
        }
        ChatBox chatBox = openChatBoxes.get(friend);
        safeClient.writeToServer(safeClient.getUsername(), message);
        chatBox.appendMessage("Scheduled message: " + message);
    }

    /**
     static void displayImage(File imageFile) {
     try {
     BufferedImage img = ImageIO.read(imageFile);
     if (img != null) {
     JFrame frame = new JFrame("Received Image");
     JLabel label = new JLabel(new ImageIcon(img));
     frame.getContentPane().add(label, BorderLayout.CENTER);
     frame.pack();
     frame.setLocationRelativeTo(null);
     frame.setVisible(true);
     } else {
     System.out.println("Failed to read image file: " + imageFile.getName());
     }
     } catch (IOException e) {
     System.out.println("Error displaying image: " + e.getMessage());
     e.printStackTrace();
     }
     }
     **/

    static void displayImage(File imageFile) {
        try {
            BufferedImage img = ImageIO.read(imageFile);
            if (img != null) {
                // Scale the image to fit the frame
                int maxWidth = 600; // Maximum width of the frame
                int maxHeight = 400; // Maximum height of the frame
                int imgWidth = img.getWidth();
                int imgHeight = img.getHeight();

                // Scale the image if it exceeds the maximum width or height
                if (imgWidth > maxWidth || imgHeight > maxHeight) {
                    double scale = Math.min((double) maxWidth / imgWidth, (double) maxHeight / imgHeight);
                    int scaledWidth = (int) (imgWidth * scale);
                    int scaledHeight = (int) (imgHeight * scale);
                    Image scaledImg = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                    img = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();
                    g2d.drawImage(scaledImg, 0, 0, null);
                    g2d.dispose();
                }

                // Display the image in a GUI window
                JFrame frame = new JFrame("Received Image");
                JLabel label = new JLabel(new ImageIcon(img));
                frame.getContentPane().add(label, BorderLayout.CENTER);
                frame.pack();

                // Set the frame size to fit the scaled image
                int frameWidth = Math.min(img.getWidth(), maxWidth);
                int frameHeight = Math.min(img.getHeight(), maxHeight);
                frame.setSize(frameWidth, frameHeight);

                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } else {
                System.out.println("Failed to read image file: " + imageFile.getName());
            }
        } catch (IOException e) {
            System.out.println("Error displaying image: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public String promptUserForFriend(String sender, String message) {
        List<JLabel> friendLabels = getFriendLabels();
        String[] possibleValues = friendLabels.stream().map(JLabel::getText).toArray(String[]::new);
        String selectedValue = (String) JOptionPane.showInputDialog(
                display,
                "Choose a friend to forward the message to:",
                "Forward Message",
                JOptionPane.PLAIN_MESSAGE,
                null,
                possibleValues,
                null);

        // Check if a friend was selected
        if (selectedValue != null) {
            String newMessage = message + ": " + selectedValue;
            safeClient.writeToServer(sender, newMessage);
        }

        return selectedValue;
    }
}
