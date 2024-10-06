import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class VideoPlayer {
    public static void displayVideo(File videoFile) {
        // Initialize JavaFX
        JFrame frame = new JFrame("Video Player");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Embed a JavaFX panel
        JFXPanel fxPanel = new JFXPanel();
        frame.add(fxPanel, BorderLayout.CENTER);

        // Run JavaFX code on the JavaFX Application Thread
        Platform.runLater(() -> {
            // Create a JavaFX scene
            Group root = new Group();
            Scene scene = new Scene(root, 800, 600);

            // Create a JavaFX media player
            Media media = new Media(videoFile.toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);

            // Add media view to the scene
            root.getChildren().add(mediaView);

            // Play the video
            mediaPlayer.play();

            // Set the JavaFX scene
            fxPanel.setScene(scene);
        });

        // Display the frame
        frame.setVisible(true);
    }

}
