package com.sonnybell.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

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

    /**
     * Constructor to initialize the client-side GUI.
     * It sets up the JavaFX application and creates the UI components.
     */
    @Override
    public void start(Stage primaryStage) {

        // Create a VBox layout for the main window
        VBox root = new VBox(10);

        // Create a text area for displaying messages
        messageArea = new TextArea();

        // Set the text area to be non-editable
        messageArea.setEditable(false);

        // Set the text area to be non-resizable
        messageArea.setWrapText(true);

        // Create a scroll pane for the message area
        inputField = new TextField();

        // Set the prompt text for the input field
        inputField.setPromptText("Type a message...");

        // Create a button to send messages
        sendButton = new Button("Send");

        // Set the button to be disabled initially
        sendButton.setDisable(true);

        HBox inputBox = new HBox(10, inputField, sendButton);
        root.getChildren().addAll(new ScrollPane(messageArea), inputBox);
        Scene scene = new Scene(root, 400, 300);

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
        try {
            Socket socket = new Socket("localhost", 1234);
            BufferedWriter tempWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader tempReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Prompt for password
            String serverResponse;
            while (true) {
                String password = promptDialog("Enter server password:");
                if (password == null) {
                    // User cancelled the dialog, exit the app
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

            // Prompt for username
            String username = promptDialog("Enter your username:");
            if (username == null || username.trim().isEmpty()) {
                Platform.runLater(() -> {
                    Platform.exit();
                    new Thread(() -> {
                        try {
                            Thread.sleep(200);
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

        } catch (

        IOException e) {
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

            // Append your message locally like CLI does
            messageArea.appendText(msg + "\n");

            client.sendMessage(msg); // GUI uses this
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
        try {
            if (client != null) {
                // Send quit message to server to quit gracefully.
                client.sendMessage("quit");
                client.closeEverything();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
