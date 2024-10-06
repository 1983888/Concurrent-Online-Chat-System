//import org.junit.jupiter.api.Test;
//import java.io.IOException;
//class ConcurrentSaveMessages {
//
//    @Test
//
//    public void SendSimultaneousMessage() throws IOException {
//        // Set the current user to andrewcrossan
//        safeClient c = new safeClient();
//        c.setUsername("andrewcrossan");
//        ChatBox chatBox = new ChatBox("evablues", new View());
//
//        new Thread(() -> {
//            chatBox.saveMessage("evablues: Hello", true);
//        }).start();
//        new Thread(() -> {
//            chatBox.saveMessage("evablues: How are you?", true);
//        }).start();
//        new Thread(() -> {
//            chatBox.saveMessage("You: I am good", true);
//        }).start();
//        new Thread(() -> {
//            chatBox.saveMessage("evablues: Thats good!", true);
//        }).start();
//        new Thread(() -> {
//            chatBox.saveMessage("You: Yes, it is.", true);
//        }).start();
//    }
//
//    @Test
//    public void SendLockedSimMessage() throws IOException {
//        // Set the current user to andrewcrossan
//        safeClient c = new safeClient();
//        c.setUsername("andrewcrossan");
//        ChatBox chatBox = new ChatBox("evablues", new View());
//
//        new Thread(() -> {
//            try {
//                chatBox.LockedSaveFile("evablues: Hello", true);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
//        new Thread(() -> {
//            try {
//                chatBox.LockedSaveFile("evablues: How are you?", true);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
//        new Thread(() -> {
//            try {
//                chatBox.LockedSaveFile("You: I am good", true);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
//        new Thread(() -> {
//            try {
//                chatBox.LockedSaveFile("evablues: Thats good!", true);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
//        new Thread(() -> {
//            try {
//                chatBox.LockedSaveFile("You: Yes, it is.", true);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }
//
//}
