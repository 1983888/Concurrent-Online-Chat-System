import java.io.IOException;
public class ServerApp {
    public static void main(String[] args) {
        try {
            Server server = new Server();
            int serverPort = 8082;
            server.startListening(serverPort);
            System.out.println("Server started on port " + serverPort);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to start the server: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }
}