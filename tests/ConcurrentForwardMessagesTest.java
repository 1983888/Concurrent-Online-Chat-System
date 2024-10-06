import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ConcurrentForwardMessagesTest {
    @Test
    public void testSafeForwardMessageThreadSafety() throws InterruptedException, IOException {
        Server server = new Server();
        server.startListening(8082);
        safeClient client = new safeClient();
        client.connectToServer("localhost", 8082);
        client.setUsername("laurenknox");

        View mockedView = Mockito.mock(View.class);
        Mockito.when(mockedView.promptUserForFriend(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("evablues"); // Simulated user input

        ChatBox chatBox = new ChatBox("Friend", mockedView);
        int numThreads = 10;

        int messagesPerThread = 10;

        CountDownLatch startLatch = new CountDownLatch(1);

        CountDownLatch endLatch = new CountDownLatch(numThreads);

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        // Spawn threads
        for (int i = 0; i < numThreads; i++) {
            executorService.execute(() -> {
                try {
                    // Wait for all threads to start concurrently
                    startLatch.await();
                    // Forward messages
                    for (int j = 0; j < messagesPerThread; j++) {
                        chatBox.forwardMessageSafe("Test message");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // Count down the latch to signal thread completion
                    endLatch.countDown();
                }
            });
        }

        // Release all threads to start concurrently
        startLatch.countDown();

        // Wait for all threads to complete
        endLatch.await();

        // Shut down the ExecutorService
        executorService.shutdown();

        // Get the message history
        var messageHistory = chatBox.getMessageHistory();

        // Calculate the expected number of messages
        int expectedMessages = numThreads * messagesPerThread;

        // Assert that the message history size matches the expected number of messages
        assertEquals(expectedMessages, messageHistory.size());
    }
}


