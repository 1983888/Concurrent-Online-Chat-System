import org.bytedeco.javacv.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class VideoEncoder {
    private Socket socket;
    private VideoStreamingView streamingView;
    private boolean streaming;


    public VideoEncoder(Socket socket, VideoStreamingView streamingView) {
        this.socket = socket;
        this.streamingView = streamingView;
        this.streaming = true; // Flag to control streaming
    }

    public void streamVideo(File videoFile) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();

            Java2DFrameConverter converter = new Java2DFrameConverter();
            Frame frame;
            while ((frame = grabber.grab()) != null && streaming) {
                BufferedImage image = converter.convert(frame);
                if (image != null) {
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        ImageIO.write(image, "jpg", outputStream);
                        byte[] imageData = outputStream.toByteArray();

                        // Send image data to the streaming view
                        streamingView.displayVideoFrame(imageData);

                        // Send image data over the network
                        sendFrame(imageData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void streamWebCamVideo() {
        try {
            OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);

            final int captureWidth = 100;
            final int captureHeight = 100;

            grabber.setImageWidth(captureWidth);
            grabber.setImageHeight(captureHeight);

            grabber.start();

            Java2DFrameConverter converter = new Java2DFrameConverter();
            Frame frame;
            while ((frame = grabber.grab()) != null && streaming) {
                BufferedImage image = converter.convert(frame);
                if (image != null) {
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        ImageIO.write(image, "jpg", outputStream);
                        byte[] imageData = outputStream.toByteArray();
                        // Send image data to the streaming view
                        streamingView.displayVideoFrame(imageData);
                        // Send image data over the network
                        sendFrame(imageData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            grabber.stop();
            grabber.close();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFrame(byte[] imageData) throws IOException {
        if (socket.isConnected() && !socket.isClosed()) {
            OutputStream os = socket.getOutputStream();
            os.write(imageData);
            os.flush();
        } else {
            System.out.println("Socket is not connected.");
            streaming = false;
        }
    }

    public void stopStreaming() {
        streaming = false;
    }
}
