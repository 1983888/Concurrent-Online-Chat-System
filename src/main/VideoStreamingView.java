import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class VideoStreamingView {
    private List<PeerClient> connectedClients; // List of connected clients
    private JFrame display;
    private JPanel streamingPanel;
    private JButton startStreamButton;
    private JButton stopStreamButton;
    private JButton startVideoStreamButton;
    private JFileChooser fileChooser;
    private static JLabel videoLabel; // JLabel to display the video frames


    public VideoStreamingView() {
        // Initialize components
        initializeComponents();

        // Add action listeners
        startStreamButton.addActionListener(this::startStreaming);
        stopStreamButton.addActionListener(this::stopStreaming);
        startVideoStreamButton.addActionListener(this::startVideoStream);
    }

    private void initializeComponents() {
        display = new JFrame("Video Streaming");
        display.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        streamingPanel = new JPanel();
        streamingPanel.setLayout(new BorderLayout()); // Use BorderLayout for better resizing

        startStreamButton = new JButton("Start Streaming");
        stopStreamButton = new JButton("Stop Streaming");
        startVideoStreamButton = new JButton("Start Video Stream");
        stopStreamButton.setEnabled(false); // Disable stop button initially

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startStreamButton);
        buttonPanel.add(stopStreamButton);
        buttonPanel.add(startVideoStreamButton);

        streamingPanel.add(buttonPanel, BorderLayout.NORTH); // Place buttons at the top

        // Create file chooser and set it to the instance variable
        fileChooser = new JFileChooser();

        // Create video label with preferred size
        videoLabel = new JLabel();
        videoLabel.setPreferredSize(new Dimension(800, 600)); // Set preferred size
        streamingPanel.add(videoLabel, BorderLayout.CENTER); // Center video label

        // Add streaming panel to the content pane of the JFrame
        display.getContentPane().add(streamingPanel);

        display.pack(); // Resize the frame to fit its contents
        display.setLocationRelativeTo(null); // Center the frame on the screen
        display.setVisible(true);
    }

    private void startStreaming(ActionEvent event) {
        int returnValue = fileChooser.showOpenDialog(display);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Selected file for streaming: " + selectedFile.getAbsolutePath());
            this.connectedClients = PeerToPeerApp.getConnectedClients();
            for (PeerClient client : connectedClients) {
                Socket socket = client.getSocket();
                new Thread(() -> {
                    VideoEncoder videoStreamer = new VideoEncoder(socket, this);
                    videoStreamer.streamVideo(selectedFile);
                }).start();
            }
            startStreamButton.setEnabled(false); // Disable start button
            stopStreamButton.setEnabled(true);   // Enable stop button
        }
    }

    private void startVideoStream(ActionEvent event) {
        this.connectedClients = PeerToPeerApp.getConnectedClients();
        for (PeerClient client : connectedClients) {
            Socket socket = client.getSocket();
            new Thread(() -> {
                VideoEncoder videoStreamer = new VideoEncoder(socket, this);
                videoStreamer.streamWebCamVideo();
            }).start();
        }
    }

    private void stopStreaming(ActionEvent event) {
        // TODO: Implement the logic to stop streaming
        System.out.println("Streaming stopped.");

        stopStreamButton.setEnabled(false);    // Disable stop button
        startStreamButton.setEnabled(true);    // Enable start button
    }

    public static void displayVideoFrame(byte[] imageData) {
        if (imageData != null && imageData.length > 0) {
            SwingUtilities.invokeLater(() -> {
                try {
                    // Convert byte array to BufferedImage
                    ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
                    BufferedImage image = ImageIO.read(bis);

                    if (image != null) {
                        // Update the videoLabel with the new image
                        videoLabel.setIcon(new ImageIcon(image));
                    } else {
                        System.err.println("Failed to load image: ImageIO.read returned null.");
                    }

                } catch (IOException e) {
                    System.err.println("Error reading image from byte array: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception ex) {
                    System.err.println("Unexpected error occurred: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        } else {
            System.err.println("Invalid image data: Byte array is null or empty.");
        }
    }

}
