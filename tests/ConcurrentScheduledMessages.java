import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ConcurrentScheduledMessages {

    @Test
    public void testUnsafeSchedulingConcurrency() throws IOException, InterruptedException {
        Server server = new Server();
        server.startListening(8082);
        safeClient client = new safeClient();
        client.connectToServer("localhost", 8082);
        client.setUsername("laurenknox");
        View view = new View();

        Thread thread1 = new Thread(() -> view.scheduleMessageUnsafe("Friend1", "Message1", 1));
        Thread thread2 = new Thread(() -> view.scheduleMessageUnsafe("Friend1", "Message2", 1));
        Thread thread3 = new Thread(() -> view.scheduleMessageUnsafe("Friend1", "Message3", 1));
        Thread thread4 = new Thread(() -> view.scheduleMessageUnsafe("Friend1", "Message4", 1));

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        Thread.sleep(10000);
    }


    @Test
    public void testSafeScheduling() throws IOException {
        Server server = new Server();
        server.startListening(8082);
        safeClient client = new safeClient();
        client.connectToServer("localhost", 8082);
        client.setUsername("laurenknox");
        View view = new View();

        view.scheduleMessage("Friend1", "Message1", 1);
        view.scheduleMessage("Friend1", "Message2", 1);
        view.scheduleMessage("Friend1", "Message3", 1);
        view.scheduleMessage("Friend1", "Message4", 1);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

