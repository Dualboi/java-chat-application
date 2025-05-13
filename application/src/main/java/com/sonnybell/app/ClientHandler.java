package com.sonnybell.app;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ClientHandler class to manage individual client connections.
 * It handles sending and receiving messages for each connected client.
 */
public class ClientHandler implements Runnable {
    private static final List<ClientHandler> CLIENT = new CopyOnWriteArrayList<>();
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;
    private boolean append;
    private String pattern;

    /**
     * Constructor to initialize the client handler with a socket.
     *
     * @param socket The socket connected to the client.
     */
    public ClientHandler(Socket socket) {
        this.append("%h/MessageLog.log", true); // Sets the log file path and append mode
        this.socket = socket;

        try {
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = reader.readLine(); // First message is username
            String message = "SERVER: " + username + " has joined the chat!";
            broadcastMessage(message);
            logMessage(message);
        } catch (IOException e) {
            closeEverything();
        }
        CLIENT.add(this); // Add this client to the list of connected clients
    }

    /**
     * Method to send messages to all connected clients.
     * It runs in a separate thread to continuously read messages.
     * Contains an if statement to check if the message is "quit" to exit the loop.
     * It also handles IOException when the client disconnects.
     */
    @Override
    public void run() {
        String message;
        try {
        while (socket.isConnected() && (message = reader.readLine()) != null) {
            if (message.equalsIgnoreCase("quit")) {
                break; // Exit the loop
            } else {
                String fullMessage = message;
                broadcastMessage(fullMessage);
            }
        }

        } catch (IOException e) {
            // Client likely disconnected
        } finally {
            closeEverything();
        }
    }

    /**
     * Method to broadcast a message to all connected clients except the sender.
     *
     * @param message The message to be sent.
     */
    public void broadcastMessage(String message) {
        for (ClientHandler client : CLIENT) {
            try {
                if (!client.username.equals(this.username)) {
                    client.writer.write(message);
                    client.writer.newLine();
                    client.writer.flush();
                }
            } catch (IOException e) {
                client.closeEverything();
            }
        }
    }

    /**
     * Method to remove the client handler from the list of connected clients.
     * and broadcast a message indicating the client has left.
     */
    public void removeClientHandler() {
        CLIENT.remove(this);
        String message = "SERVER: " + username + " has left the chat.";
        broadcastMessage(message);
        logMessage(message);

    }

    /**
     * Method to close all resources associated with the client.
     */
    public void closeEverything() {
        removeClientHandler();
        try {
            if (reader != null) {
                reader.close();
            }

            if (writer != null) {
                writer.close();
            }

            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to append a message to a log file.
     *
     * @param filePattern The pattern for the log file.
     * @param toAppend    Whether to append to the file or not.
     */
    public void append(String filePattern, boolean toAppend) {
        this.pattern = filePattern;
        this.append = toAppend;
    }

    /**
     * Method to log messages to a file.
     *
     * @param message The message to be logged.
     */
    public void logMessage(String message) {
        // Get the path of the current working directory (your project folder)
        String projectDir = System.getProperty("user.dir"); // This will give the path to your project folder
        if (projectDir == null) {
            System.err.println("Could not resolve project directory.");
            return;
        }

        // Replace %h with the project directory path
        String filePath = pattern.replace("%h", projectDir);
        // System.out.println("Resolved log file path: " + filePath); // Print the file
        // path for debugging

        // Create a new File object with the resolved path
        File file = new File(filePath);

        // Check if the file exists, if not create it
        if (!file.exists()) {
            try {
                boolean created = file.createNewFile(); // Create the file if it doesn't exist
                if (created) {
                    System.out.println("Log file did not exist, created a new one.");
                } else {
                    System.err.println("Failed to create the log file.");
                    return;
                }
            } catch (IOException e) {
                System.err.println("Failed to create the log file.");
                e.printStackTrace();
                return;
            }
        }

        // Write to the log file
        try (FileWriter writer = new FileWriter(filePath, append)) {
            String timestamped = "[" + new java.util.Date() + "] " + message;
            writer.write(timestamped + System.lineSeparator());
            // System.out.println("Successfully wrote to log.");
        } catch (IOException e) {
            System.err.println("Failed to write to log file.");
            e.printStackTrace();
        }
    }
}
