import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Controller {
    private View view;
    private safeClient model;
    private Boolean isConnected = false;
    private FriendListUpdater friendListUpdater;
    private Thread friendListUpdateThread;

    public Controller(View view, safeClient model) {
        this.view = view;
        this.model = model;

        view.setAddFriendButtonListener(e -> {
            JButton sourceButton = (JButton) e.getSource();
            String friendUsername = sourceButton.getActionCommand();
            sendFriendRequest(friendUsername);
        });

        model.addMessageListener(new safeClient.MessageListener() {
            @Override
            public void onMessageReceived(String message) {
                System.out.println(message);
                SwingUtilities.invokeLater(() -> {
                    if (message.contains("FILE_TRANSFER")) {
                        System.out.println(message + " for finding the structure");
                        String[] parts = message.split(" ", 4);
                        String senderUsername = parts[1];
                        String fileName = parts[2];
                        String fileType = parts[parts.length-1];
                        view.displayMessage("File received from " + senderUsername + ": " + fileName);
                    } else {
                        String[] parts = message.split(": ", 3);
                        System.out.println(Arrays.toString(parts));
                        if (parts.length == 3) {
                            String senderUsername = parts[0];
                            String actualMessage = parts[1];
                            String recipientUsername = parts[parts.length-1];
                            if (safeClient.getUsername().equals(recipientUsername)) {
                                view.openChatBox(senderUsername);
                                View.displayMessageInChatBox(senderUsername, actualMessage);
                            }
                        }
                        else {
                            view.displayMessage(message);
                        }
                    }
                });
            }
            @Override
            public void onConnectionStatusChanged(boolean connected) {
                isConnected = connected;
                SwingUtilities.invokeLater(() -> {
                    if (!connected) {
                        view.displayMessage("Disconnected");
                    }
                });
            }
        });
        friendListUpdater = new FriendListUpdater(this);
        friendListUpdateThread = new Thread(friendListUpdater);
    }

    public void promptForAndValidateUsername() {
        String username = null;

        while (username == null || username.trim().isEmpty()) {
            username = JOptionPane.showInputDialog("Enter your username:");
            if (username == null) {
                System.exit(0);
            } else {
                try {
                    if (!userExists(username)) {
                        addUserToDatabase(username);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    view.displayMessage("Error validating username: " + e.getMessage());
                }
            }
        }
        model.setUsername(username);
        friendListUpdateThread.start();
        displayAllUsers();
    }

    private boolean userExists(String username) throws SQLException {
        Connection connection = dbConnect.getMysqlConnection();
        String query = "SELECT * FROM Users WHERE username = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();  // Returns true if the user exists, false otherwise
            }
        }
    }

    private void addUserToDatabase(String username) {
        try {
            Connection connection = dbConnect.getMysqlConnection();
            String insertQuery = "INSERT INTO Users (username) VALUES (?)";

            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                insertStatement.setString(1, username);
                insertStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            view.displayMessage("Error adding user to the database: " + e.getMessage());
        }
    }

    private void displayAllUsers() {
        try {
            Connection connection = dbConnect.getMysqlConnection();
            String query = "SELECT * FROM Users WHERE username != ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, model.getUsername());

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String userName = resultSet.getString("username");
                        if (!isFriend(userName, model.getUsername())) {
                            view.displayUser(userName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.displayMessage("Failed to fetch users from the database: " + e.getMessage());
        }
    }

    private void sendFriendRequest(String friendUsername) {
        try {
            Connection connection = dbConnect.getMysqlConnection();
            String checkQuery = "SELECT * FROM Friendships WHERE (user_one_ID = ? AND user_two_ID = ?) OR (user_one_ID = ? AND user_two_ID = ?)";
            try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
                checkStatement.setInt(1, getUserId(model.getUsername()));
                checkStatement.setInt(2, getUserId(friendUsername));
                checkStatement.setInt(3, getUserId(friendUsername));
                checkStatement.setInt(4, getUserId(model.getUsername()));

                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        String insertQuery = "INSERT INTO Friendships (user_one_ID, user_two_ID) VALUES (?, ?)";
                        try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                            insertStatement.setInt(1, getUserId(model.getUsername()));
                            insertStatement.setInt(2, getUserId(friendUsername));
                            insertStatement.executeUpdate();
                            view.removeUser(friendUsername);
                            view.displayMessage("Friend request sent to " + friendUsername);
                        }
                    } else {
                        view.displayMessage("Friendship already exists with " + friendUsername);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.displayMessage("Error sending friend request: " + e.getMessage());
        }
    }

    private boolean isFriend(String user1, String user2) throws Exception {
        Connection connection = dbConnect.getMysqlConnection();
        String query = "SELECT * FROM Friendships WHERE " +
                "(user_one_ID = ? AND user_two_ID = ?) OR " +
                "(user_one_ID = ? AND user_two_ID = ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, getUserId(user1));
            preparedStatement.setInt(2, getUserId(user2));
            preparedStatement.setInt(3, getUserId(user2));
            preparedStatement.setInt(4, getUserId(user1));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();  // Returns true if they are already friends
            }
        }
    }

    private int getUserId(String username) throws Exception {
        Connection connection = dbConnect.getMysqlConnection();
        String query = "SELECT user_ID FROM Users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("user_ID");
                } else {
                    throw new Exception("User not found in the database");
                }
            }
        }
    }

    public List<String> getAllFriends() {
        List<String> friends = new ArrayList<>();
        try {
            Connection connection = dbConnect.getMysqlConnection();
            String query = "SELECT CASE " +
                    "WHEN Friendships.user_one_ID = ? THEN Users2.username " +
                    "WHEN Friendships.user_two_ID = ? THEN Users1.username " +
                    "END AS friend_username " +
                    "FROM Friendships " +
                    "INNER JOIN Users AS Users1 ON Friendships.user_one_ID = Users1.user_ID " +
                    "INNER JOIN Users AS Users2 ON Friendships.user_two_ID = Users2.user_ID " +
                    "WHERE Friendships.user_one_ID = ? OR Friendships.user_two_ID = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                int userId = getUserId(model.getUsername());
                preparedStatement.setInt(1, userId);
                preparedStatement.setInt(2, userId);
                preparedStatement.setInt(3, userId);
                preparedStatement.setInt(4, userId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String friendUsername = resultSet.getString("friend_username");
                        friends.add(friendUsername);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return friends;
    }

    private static class FriendListUpdater implements Runnable {
        private final Controller controller;
        private volatile boolean running = true;
        private List<String> previousFriends;

        public FriendListUpdater(Controller controller) {
            this.controller = controller;
            this.previousFriends = new ArrayList<>();
        }

        @Override
        public void run() {
            while (running) {
                try {
                    List<String> currentFriends = controller.getAllFriends();
                    controller.view.setUsername(safeClient.getUsername());

                    if (!previousFriends.equals(currentFriends)) {
                        previousFriends = currentFriends;
                        SwingUtilities.invokeLater(() -> {
                            controller.view.displayFriends(currentFriends);
                        });
                    }

                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        public void stop() {
            running = false;
        }
    }
}

