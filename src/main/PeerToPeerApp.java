import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeerToPeerApp {
    private PeerClient client2;
    private static List<PeerClient> connectedClients;

    private VideoStreamingView streamingView2;
    public PeerToPeerApp() {
        // Initialize the peer clients
        try {

            client2 = new PeerClient();
        } catch (IOException e) {
            System.out.println("An error occurred while creating the clients: " + e.getMessage());
            return;
        }

        // Initialize the shared list of connected clients
        connectedClients = new ArrayList<>();

        connectedClients.add(client2);

        // Initialize the video streaming views

        streamingView2 = new VideoStreamingView(); // Pass the shared list to the view

        // Start listening for connections
        startListening();
    }

    public static List<PeerClient> getConnectedClients() {
        return connectedClients;
    }

    private void startListening() {
        // Start listening for incoming connections on separate threads

        new Thread(() -> {
            try {
                client2.listenForConnections(8082); // Client2 listens on port 8082
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }


    public static void main(String[] args) throws IOException {
        // Create an instance of the application
        PeerToPeerApp app = new PeerToPeerApp();

        app.client2.connectToPeer("10.12.1.61", 8081);
        // Connect client2 to client1 on port 8081
        System.out.println("Client2 connected to Client1");
    }
}

//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class PeerToPeerApp {
//    private PeerClient client1;
//    private PeerClient client2;
//    private static List<PeerClient> connectedClients;
//    private VideoStreamingView streamingView1;
//    private VideoStreamingView streamingView2;
//    public PeerToPeerApp() {
//        // Initialize the peer clients
//        try {
//            client1 = new PeerClient();
//            client2 = new PeerClient();
//        } catch (IOException e) {
//            System.out.println("An error occurred while creating the clients: " + e.getMessage());
//            return;
//        }
//
//        // Initialize the shared list of connected clients
//        connectedClients = new ArrayList<>();
//        connectedClients.add(client1);
//        connectedClients.add(client2);
//
//        // Initialize the video streaming views
//        streamingView1 = new VideoStreamingView(); // Pass the shared list to the view
//        streamingView2 = new VideoStreamingView(); // Pass the shared list to the view
//
//        // Start listening for connections
//        startListening();
//    }
//
//    public static List<PeerClient> getConnectedClients() {
//        return connectedClients;
//    }
//
//    private void startListening() {
//        // Start listening for incoming connections on separate threads
//        new Thread(() -> {
//            try {
//                client1.listenForConnections(8081); // Client1 listens on port 8081
//            } catch (IOException e) {
//                System.out.println("An error occurred while listening for incoming connections for client1: " + e.getMessage());
//            }
//        }).start();
//
//        new Thread(() -> {
//            try {
//                client2.listenForConnections(8082); // Client2 listens on port 8082
//            } catch (IOException e) {
//                System.out.println("An error occurred while listening for incoming connections for client2: " + e.getMessage());
//            }
//        }).start();
//    }
//
//
//    public static void main(String[] args) {
//        // Create an instance of the application
//        PeerToPeerApp app = new PeerToPeerApp();
//
//        // Connect client1 to client2
//        try {
//            app.client1.connectToPeer("localhost", 8082); // Connect client1 to client2 on port 8082
//            System.out.println("Client1 connected to Client2");
//        } catch (IOException e) {
//            System.out.println("An error occurred while connecting client1 to client2: " + e.getMessage());
//        }
//
////         Connect client2 to client1
//        try {
//            app.client2.connectToPeer("localhost", 8081); // Connect client2 to client1 on port 8081
//            System.out.println("Client2 connected to Client1");
//        } catch (IOException e) {
//            System.out.println("An error occurred while connecting client2 to client1: " + e.getMessage());
//        }
//    }
//}
