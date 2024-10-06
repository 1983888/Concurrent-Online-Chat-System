import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ConcurrentSendMessages {

    private Server server;

    @BeforeEach
    public void setUp() throws IOException {
        server = new Server();
        server.startListening(8082);
    }

    @Test
    public void testConcurrentWriteToServer() {
        int numberOfClients = 10;
        int messagesPerClient = 100;
        CountDownLatch latch = new CountDownLatch(numberOfClients);

        try {
            for (int i = 0; i < numberOfClients; i++) {
                final int clientId = i;
                new Thread(() -> {
                    try {
                        latch.countDown();
                        latch.await();

                        Client client = new Client();
//                        safeClient client = new safeClient();
                        client.connectToServer("localhost", 8082);

                        for (int j = 0; j < messagesPerClient; j++) {
                            String username = Thread.currentThread().getName() + "-Client" + clientId;
                            String message = "Hello from " + username + ", Message: " + j;

                            client.writeToServer( username, message);
                        }
                    } catch (InterruptedException | IllegalStateException | IOException e) {
                        System.err.println("Error: " + e.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("Error during writeToServer");
                    }
                }).start();
            }

            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Error during test");
        }
        List<String> receivedMessages = server.getReceivedMessages();
        System.out.println("ACTUAL: " + numberOfClients * messagesPerClient + " EXPECTED : " + receivedMessages.size());
        assertEquals(numberOfClients * messagesPerClient, receivedMessages.size());
    }

}
