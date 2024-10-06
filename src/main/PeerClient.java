import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PeerClient {
    public Socket socket;

    public PeerClient() throws IOException {
        socket = new Socket();
    }
    public void connectToPeer(String peerHostname, int peerPort) throws IOException {
        socket.connect(new InetSocketAddress(peerHostname, peerPort));
        System.out.println("[CLIENT] Connected to peer: " + socket.getInetAddress());
    }
    public void listenForConnections(int listenPort) throws IOException {
        ServerSocket serverSocket = new ServerSocket(listenPort);
        // Begin listening for incoming connections on a new thread
        new Thread(() -> {
            while (true) {
                System.out.println("[CLIENT] Listening for incoming connections...");
                try {
                    // Accept incoming connections
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[CLIENT] Accepted connection from client: " + clientSocket.getInetAddress());

                    // Start a new thread for reading incoming data from the client
                    new Thread(() -> {
                        while (clientSocket.isConnected()) {
                            readAsync(clientSocket);
                            try {
                                Thread.sleep(6000);
                            } catch (InterruptedException e) {
                                System.out.println("InterruptedException: " + e.getMessage());
                            }
                        }
                    }).start();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();
    }
    public void readAsync(Socket peerSocket) {
        // Start a new thread to handle incoming data
        new Thread(() -> {
            try {
                InputStream inputStream = peerSocket.getInputStream();
                byte[] buffer = new byte[1638400]; // Adjust buffer size as needed

                while (true) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        // End of stream
                        break;
                    }

                    if (bytesRead < 4) {
                        String message = new String(buffer, 0, bytesRead);
                        System.out.println(message);
                    } else {
                        byte[] imageData = Arrays.copyOf(buffer, bytesRead);
                        VideoStreamingView.displayVideoFrame(imageData);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading from peer: " + e.getMessage());
            }
        }).start();
    }

//    public void readAsync(Socket clientSocket) {
//        new Thread(() -> {
//            try {
//                InputStreamReader inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
//                BufferedReader reader = new BufferedReader(inputStreamReader);
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    System.out.println(line);
//                }
//            } catch (IOException e) {
//
//                System.out.println("Error reading from server: " + e.getMessage());
//            }
//        }).start();
//    }

    public Socket getSocket() {
        return this.socket;
    }
}
