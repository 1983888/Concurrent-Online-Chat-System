import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.*;

public class Server implements Runnable {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private List<String> receivedMessages = new ArrayList<>();
    private Lock clientsLock = new ReentrantLock();
    private Lock receivedMessagesLock = new ReentrantLock();

    public void startListening(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        new Thread(this).start();
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("[SERVER] Waiting for incoming connections...");
                Socket clientSocket = serverSocket.accept();
                Socket fileSocket = serverSocket.accept();
                System.out.println("[SERVER] Accepted connection from client: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket, fileSocket, "default_username");
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        clientsLock.lock();
        try {
            Iterator<ClientHandler> iterator = clients.iterator();
            while (iterator.hasNext()) {
                ClientHandler client = iterator.next();
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        } finally {
            clientsLock.unlock();
        }
    }

    public List<String> getReceivedMessages() {
        receivedMessagesLock.lock();
        try {
            return receivedMessages;
        } finally {
            receivedMessagesLock.unlock();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket textSocket;
        private Socket fileSocket;
        private PrintWriter textOut;

        private String username;

        public ClientHandler(Socket textSocket, Socket fileSocket, String username) {
            this.textSocket = textSocket;
            this.fileSocket = fileSocket;
            this.username = username;
            try {
                textOut = new PrintWriter(textSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getUsername() {
            return username;
        }

        public void sendMessage(String message) {
            textOut.println(message);
        }

        private void receiveFile(DataInputStream dataInputStream, String fileName, String recipientUsername) throws IOException {
            String directoryPath = "server_files";
            File directory = new File(directoryPath);

            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Directory created: " + directoryPath);
                } else {
                    System.out.println("Failed to create directory: " + directoryPath);
                    return;
                }
            }

            File file = new File(directory, fileName);

            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                System.out.println("File " + fileName + " received and stored on the client side.");
            } catch (IOException e) {
                System.out.println("Error storing file on the client side: " + e.getMessage());
                e.printStackTrace();
            }

            sendFileToRecipient(fileName, recipientUsername, directoryPath);
        }

        private void sendFileToRecipient(String fileName, String recipientUsername, String directoryPath) {
            try {
                String notificationMessage = "FILE_TRANSFER:" + fileName + ":" + recipientUsername;

                broadcastMessage(notificationMessage, this);

                for (ClientHandler client : clients) {
                    if (client != this) {
                        String filePath = directoryPath + File.separator + fileName;
                        File file = new File(filePath);

                        if (!file.exists()) {
                            System.out.println("File not found at: " + filePath);
                            return;
                        }

                        try (FileInputStream fis = new FileInputStream(file);
                             BufferedInputStream bis = new BufferedInputStream(fis);
                             DataOutputStream dos = new DataOutputStream(client.fileSocket.getOutputStream())) {

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = bis.read(buffer)) != -1) {
                                dos.write(buffer, 0, bytesRead);
                            }
                            dos.flush();
                        } catch (IOException e) {
                            System.out.println("Error sending file to recipient: " + e.getMessage());
                            e.printStackTrace();
                        }

                        System.out.println("File " + fileName + " sent to recipient " + recipientUsername);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        @Override
        public void run() {
            try (BufferedReader textIn = new BufferedReader(new InputStreamReader(textSocket.getInputStream()));
                 DataInputStream fileIn = new DataInputStream(fileSocket.getInputStream())) {
                String message;
                while ((message = textIn.readLine()) != null) {

                    if (message.contains("FILE_TRANSFER")) {
                        System.out.println(message);
                        String[] parts = message.split(":", 3);
                        String fileName = parts[1];
                        String recipientUsername = parts[parts.length-1];
                        receiveFile(fileIn, fileName, recipientUsername);
                    } else {
                        broadcastMessage(message, this);
                        receivedMessages.add(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clientsLock.lock();
                try {
                    clients.remove(this);
                    System.out.println("[SERVER] Client disconnected: " + textSocket.getInetAddress());
                } finally {
                    clientsLock.unlock();
                }
            }
        }

    }

}
