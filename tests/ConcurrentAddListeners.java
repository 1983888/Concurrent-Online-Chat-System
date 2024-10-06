import org.junit.jupiter.api.RepeatedTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentAddListeners {

    @RepeatedTest(15)
    void testConcurrentAddListenersUNSAFE() throws InterruptedException, IOException {
        Client client = new Client();
        int numberOfThreads = 1000;
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new Thread(() -> {
                Client.MessageListener listener = new Client.MessageListener() {
                    @Override
                    public void onMessageReceived(String message) {
                    }

                    @Override
                    public void onConnectionStatusChanged(boolean connected) {
                    }
                };
                client.addMessageListener(listener);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertNotEquals(numberOfThreads, client.getListenersCount());
    }


    @RepeatedTest(15)
    void testConcurrentAddListenersSAFE() throws InterruptedException, IOException {
        safeClient client = new safeClient();
        int numberOfThreads = 1000;
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new Thread(() -> {
                safeClient.MessageListener listener = new safeClient.MessageListener() {
                    @Override
                    public void onMessageReceived(String message) {
                    }

                    @Override
                    public void onConnectionStatusChanged(boolean connected) {
                    }
                };
                client.addMessageListener(listener);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(numberOfThreads, client.getListenersCount());
    }
}
