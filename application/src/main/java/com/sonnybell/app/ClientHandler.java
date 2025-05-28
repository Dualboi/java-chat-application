package com.sonnybell.app;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ClientHandler class to manage individual client connections.
 * It handles sending and receiving messages for each connected client.
 */
public class ClientHandler implements Runnable {
    // List to keep track of all connected clients
    private static final List<ClientHandler> CLIENT = new CopyOnWriteArrayList<>();

    private static int clientTotal;

    /**
     * List to keep track of client usernames.
     * This is a synchronized list to ensure thread safety when multiple clients
     * are connected.
     */
    private static List<String> clientNamesList = Collections.synchronizedList(new ArrayList<>());
    // Socket connected to the client
    private Socket socket;
    // BufferedReader to read messages from the client
    private BufferedReader reader;
    // BufferedWriter to send messages to the client
    private BufferedWriter writer;
    // Username of the client
    private String username;
    // Append mode for the log file
    private boolean append;
    // Pattern for the log file path
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

            // Only read username AFTER password is validated
            this.username = reader.readLine();
            if (username == null) {
                socket.close();
                return;
            }

            // Getting chat history from the ChatHistory class
            for (String msg : ChatHistory.getMessageHistory()) {
                writer.write(msg);
                writer.newLine();
            }
            writer.write("---END_HISTORY---");
            writer.newLine();
            writer.flush();

            System.out.println("A new user has connected!");

            // Adds the client to the total count of clients
            clientTotal++;

            // Adds the client to the list of usernames
            clientNamesList.add(username);

            // Add this client to the list of connected clients
            CLIENT.add(this);

            String message = "SERVER: " + username + " has joined the chat!";
            broadcastMessage(message);
        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * Method to get the total number of connected clients.
     *
     * @return The total number of connected clients.
     */
    public static int getClientTotal() {
        return clientTotal;
    }

    public static List<String> getClientNamesList() {
        return clientNamesList;
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
                if ("quit".equalsIgnoreCase(message)) {
                    break;
                } else if (message.trim().isEmpty()) {
                    continue;
                } else {
                    broadcastMessage(message);
                }
            }
        } catch (IOException e) {
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
        // Decide on a tag to label this message
        String tag;
        if (message.contains("has joined the chat!")) {
            tag = "HelloUser";
        } else if (message.contains("has left the chat.")) {
            tag = "GoodbyeUser";
        } else {
            tag = "UserChats";
        }

        ChatHistory.addMessageToHistory(message);
        logMessage(message, tag);

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
        // Removes the client from the server
        CLIENT.remove(this);
        // Removes the client from the total count of clients
        clientTotal--;
        // Removes the client from the list of usernames
        clientNamesList.remove(username);
        String message = "SERVER: " + username + " has left the chat.";
        broadcastMessage(message);
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
    public void logMessage(String message, String tag) {
        String projectDir = System.getProperty("user.dir");
        if (projectDir == null) {
            System.err.println("Could not resolve project directory.");
            return;
        }
        String filePath = pattern.replace("%h", projectDir);
        File file = new File(filePath);

        if (!file.exists()) {
            try {
                boolean created = file.createNewFile();
                if (created) {
                    System.out.println("Log file created.");
                }
            } catch (IOException e) {
                System.err.println("Failed to create log file.");
                e.printStackTrace();
                return;
            }
        }

        try (FileWriter fw = new FileWriter(filePath, append)) {
            String timestamped = "[" + new java.util.Date() + "] [" + tag + "] " + message;
            fw.write(timestamped + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Failed to write to log file.");
            e.printStackTrace();
        }
    }
}
