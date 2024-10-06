import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                View view = new View();
                safeClient model = new safeClient();
                Controller controller = new Controller(view, model);

                controller.promptForAndValidateUsername();

                model.connectToServer("localhost", 8082);

                view.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to initialize the client/server: " + e.getMessage());
            }
        });
    }
}
