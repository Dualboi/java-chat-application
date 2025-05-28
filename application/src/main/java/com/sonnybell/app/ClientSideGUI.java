package com.sonnybell.app;

import java.io.*;
import java.net.Socket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * ClientSideGUI class to create a graphical user interface for the client.
 * It allows users to send and receive messages from the server.
 * It uses JavaFX for the GUI components.
 * It includes a text area for displaying messages,
 * a text field for user input,
 * and a button to send messages.
 * It also handles user authentication with the server.
 */
public class ClientSideGUI extends Application {
    private TextArea messageArea;
    private TextField inputField;
    private Button sendButton;
    private Client client;
    private int serverPort = 6666;
    private int setWidth1 = 600;

    public int getSetWidth1() {
        return setWidth1;
    }

    /**
     * Constructor to initialize the client-side GUI.
     * It sets up the JavaFX application and creates the UI components.
     */
    @Override
    public void start(Stage primaryStage) {
        final int setHeight = 50;

        final int setVbox = 10;
        // Create a VBox layout for the main window
        VBox root = new VBox(setVbox);

        // Create a text area for displaying messages
        messageArea = new TextArea();

        // Set the text area to be non-editable
        messageArea.setEditable(false);

        final int setHeightMessageArea = 500;
        // Set the text area to be non-resizable
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(setHeightMessageArea);
        messageArea.setPrefWidth(setWidth1);

        final int set = 10;
        final int setLeft = 30;
        // Add left padding to the message area
        root.setPadding(new Insets(set, set, set, setLeft));

        // Create a scroll pane for the message area
        inputField = new TextField();
        inputField.setPrefWidth(setWidth1);
        inputField.setPrefHeight(setHeight);

        // Set the prompt text for the input field
        inputField.setPromptText("Type a message...");

        final int setWidth2 = 100;
        // Create a button to send messages
        sendButton = new Button("Send");
        sendButton.setPrefWidth(setWidth2);
        sendButton.setPrefHeight(setHeight);

        // Set the button to be disabled initially
        sendButton.setDisable(true);

        final int inputFieldHeight = 10;
        HBox inputBox = new HBox(inputFieldHeight, inputField, sendButton);
        root.getChildren().addAll(new ScrollPane(messageArea), inputBox);

        final int sizeWidth = 800;
        final int sizeHeight = 600;
        Scene scene = new Scene(root, sizeWidth, sizeHeight);

        // Set the scene to the primary stage
        primaryStage.setTitle("Client Chat");
        primaryStage.setScene(scene);

        // Set the primary stage to be resizable
        primaryStage.show();

        setupClient();
        setupEventHandlers();

        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
        });
    }

    /**
     * Method to set up the client connection to the server.
     * It handles user authentication and message listening.
     */
    private void setupClient() {
        Socket socket = null;
        try {
            // prompt user for port number
            String portInput = promptDialog("Enter server port (default is 6666):");
            final int defaultPort = 6666;

            if (portInput != null && !portInput.trim().isEmpty()) {
                try {
                    serverPort = Integer.parseInt(portInput);
                } catch (NumberFormatException e) {
                    showAlert("Invalid port number. Using default port " + defaultPort);
                    serverPort = defaultPort;
                }
            }
            socket = new Socket("localhost", serverPort);
            BufferedWriter tempWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader tempReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Prompt for password
            String serverResponse;
            while (true) {
                String password = promptDialog("Enter server password:");
                if (password == null) {
                    // User cancelled the dialog, exit the app
                    if (socket != null && !socket.isClosed()) {
                        try {
                            socket.close();
                        } catch (IOException ignored) {
                        }
                    }
                    Platform.exit();
                    return;
                }
                if (password.trim().isEmpty()) {
                    showAlert("Password cannot be blank. Please try again.");
                    continue;
                }
                tempWriter.write(password);
                tempWriter.newLine();
                tempWriter.flush();

                serverResponse = tempReader.readLine();
                if ("OK".equals(serverResponse)) {
                    break;
                } else {
                    showAlert("Incorrect password. Please try again.");
                }
            }

            final int sleepTime = 200;
            // Prompt for username
            String username = promptDialog("Enter your username:");
            if (username == null || username.trim().isEmpty()) {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
                Platform.runLater(() -> {
                    Platform.exit();
                    new Thread(() -> {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ignored) {
                        }
                        System.exit(0);
                    }).start();
                });
                return;
            }

            // Send username
            tempWriter.write(username);
            tempWriter.newLine();
            tempWriter.flush();
            client = new Client(socket, username);
            client.setMessageListener(msg -> {
                Platform.runLater(() -> messageArea.appendText(msg + "\n"));
            });
            // Read initial history before listening for new messages
            client.readInitialHistory();
            client.listenForMessages();

            // Enable the send button after successful authentication
            Platform.runLater(() -> sendButton.setDisable(false));

        } catch (IOException e) {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
            showAlert("Error connecting to server: " + e.getMessage());
            Platform.exit();
        }
    }

    /**
     * Method to set up event handlers for the GUI components.
     * It handles button clicks and text field actions.
     */
    private void setupEventHandlers() {
        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());
    }

    /**
     * Method to send a message to the server.
     * It retrieves the text from the input field and sends it through the client.
     */
    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            messageArea.appendText(msg + "\n");
            client.sendMessage(msg);

            // Close the GUI if user typed "quit"
            if ("quit".equalsIgnoreCase(msg)) {
                Platform.exit();
            }

            inputField.clear();
        }
    }

    /**
     * Method to prompt the user for input using a dialog.
     * It displays a text input dialog and returns the user's input.
     *
     * @param message The message to display in the dialog.
     * @return The user's input as a string.
     */
    private String promptDialog(String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Authentication");
        dialog.setHeaderText(message);
        return dialog.showAndWait().orElse(null);
    }

    /**
     * Method to show an alert dialog with a warning message.
     * It displays a warning alert with the specified message.
     *
     * @param message The warning message to display.
     */
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
            alert.showAndWait();
        });
    }

    /**
     * Method to stop the client and close the connection.
     * It sends a "quit" message to the server and closes the socket.
     */
    @Override
    public void stop() {
        if (client != null) {
            client.sendMessage("quit");
            client.closeEverything();
        }
    }

    /**
     * Main method to launch the JavaFX application.
     * It initializes the JavaFX runtime and starts the application.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        launch(args);
    }
}
