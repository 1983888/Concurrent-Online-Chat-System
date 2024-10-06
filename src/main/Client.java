import javax.swing.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client {
    public static String username;
    private static Socket textSocket;
    private static Socket fileSocket;
    private List<Client.MessageListener> listeners = new ArrayList<>();
    private List<ChatBox> openChatBoxes = new ArrayList<>();
    private static Client.FileReceivedListener fileReceivedListener;

    public interface MessageListener {
        void onMessageReceived(String message);
        void onConnectionStatusChanged(boolean connected);
    }

    public interface FileReceivedListener {
        void onFileReceived(String fileName, File file);
    }

    public static void setFileReceivedListener(Client.FileReceivedListener listener) {
        fileReceivedListener = listener;
    }
    public Client() throws IOException {
            textSocket = new Socket();
            fileSocket = new Socket();
    }

    public void addMessageListener(Client.MessageListener listener) {
            listeners.add(listener);
    }

    private void notifyConnectionStatusChanged(boolean connected) {
        for (Client.MessageListener listener : listeners) {
            listener.onConnectionStatusChanged(connected);
        }
    }

    public int getListenersCount() {
        return listeners.size();
    }

    public void connectToServer(String serverHostname, int serverPort) throws IOException {
        if (!textSocket.isConnected()) {
            textSocket.connect(new InetSocketAddress(serverHostname, serverPort));
            System.out.println("[CLIENT] Connected to server for text: " + textSocket.getInetAddress());
            if (!fileSocket.isConnected()) {
                fileSocket.connect(new InetSocketAddress(serverHostname, serverPort));
                System.out.println("[CLIENT] Connected to server for files: " + fileSocket.getInetAddress());
            }
        }
        notifyConnectionStatusChanged(true);
        // Start reading text messages
        new Thread(() -> readAsync(textSocket)).start();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static String getUsername() {
        return username;
    }

    public static void writeToServer(String username, String message) {
        if (!textSocket.isConnected()) {
            throw new IllegalStateException("Client is not connected to the server");
        }
        try {
            OutputStream outputStream = textSocket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);
            writer.println(username + ": " + message);
        } catch (IOException e) {
            System.out.println("Error writing to server: " + e.getMessage());
        }
    }


    private void readAsync(Socket clientSocket) {
        new Thread(() -> {
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line;

                while ((line = reader.readLine()) != null) {
                    notifyMessageReceived(line);
                }
                notifyConnectionStatusChanged(false);
            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
                System.out.println("Error reading from server: " + e.getMessage());
            }
        }).start();
    }

    private void notifyMessageReceived(String message) {
        if (message.startsWith("FILE_TRANSFER")) {
            System.out.println(message);
            String[] parts = message.split(": ", 4);
            System.out.println(Arrays.toString(parts));
            if ((parts.length == 4) && (parts[3].equals(Client.getUsername()))) {
                String fileName = parts[2];
                try {
                    saveReceivedFile(fileName);
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(null, "File received: " + fileName, "File Received", JOptionPane.INFORMATION_MESSAGE));
                } catch (IOException e) {
                    System.out.println("Error receiving file: " + e.getMessage());
                }
            }
        } else {
            for (Client.MessageListener listener : listeners) {
                listener.onMessageReceived(message);
            }
            handleReceivedMessage(message);
        }
    }

    private void saveReceivedFile(String fileName) throws IOException {
        File directory = new File("client_files");
        if (!directory.exists() && !directory.mkdirs()) {
            System.out.println("Failed to create directory: " + "client_files");
            return;
        }
        File file = new File(directory, fileName);
        try (FileOutputStream fos = new FileOutputStream(file);
             DataInputStream dis = new DataInputStream(fileSocket.getInputStream())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("File " + fileName + " received and stored.");
            if (fileReceivedListener != null) {
                fileReceivedListener.onFileReceived(fileName, file);
            }


            if (isImageFile(fileName)) {
                View.displayImage(file);
            } else if (isVideoFile(fileName)) {
                VideoPlayer.displayVideo(file);
            }


        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isImageFile(String fileName) {
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        for (String extension : imageExtensions) {
            if (fileName.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private boolean isVideoFile(String fileName) {
        String[] videoExtensions = {".mp4", ".avi", ".mkv", ".mov"};
        for (String extension : videoExtensions) {
            if (fileName.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static void sendFile(String filePath, String recipientUsername) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        String fileName = file.getName();
        writeToServer(getUsername(), "FILE_TRANSFER: " + fileName + ": " + recipientUsername);

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataOutputStream dos = new DataOutputStream(fileSocket.getOutputStream())) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                dos.flush();
            }
        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("File sent: " + fileName);
    }

    private void handleReceivedMessage(String message) {
        String[] parts = message.split(": ", 2);
        if (parts.length == 2) {
            String senderUsername = parts[0];
            String actualMessage = parts[1];

            for (ChatBox chatBox : openChatBoxes) {
                if (chatBox.getFriendUsername().equals(senderUsername)) {
                    View.displayMessageInChatBox(senderUsername, actualMessage);
                    break;
                }
            }
        }
    }
}

