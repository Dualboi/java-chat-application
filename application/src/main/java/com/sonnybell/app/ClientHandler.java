package com.sonnybell.app;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * ClientHandler class to manage individual client connections.
 * It handles sending and receiving messages for each connected client.
 */
public class ClientHandler implements Runnable {
    // List to keep track of all connected clients (both socket and web clients)
    private static final List<ClientHandler> CLIENT = new CopyOnWriteArrayList<>();

    // Maintain a static set of all connected handlers
    private static final Set<ClientHandler> HANDLERS = new CopyOnWriteArraySet<>();

    // Make these static since they're used in static methods
    private static final String LOG_PATTERN = "%h/MessageLog.log";
    private static final boolean APPEND_MODE = true;

    /**
     * Static variable to keep track of the total number of connected clients.
     * It is incremented when a new client connects and decremented when a client
     * disconnects. This includes both socket clients and web clients.
     */
    private static int clientTotal;

    /**
     * List to keep track of client usernames.
     * This is a synchronized list to ensure thread safety when multiple clients
     * are connected. This includes both socket clients and web clients.
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

    /**
     * Constructor to initialize the client handler with a socket.
     *
     * @param socket The socket connected to the client.
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;

        try {
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Reading username after password is validated
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

            // Use centralized tracking for socket clients
            synchronized (ClientHandler.class) {
                // Adds the client to the total count of clients
                clientTotal++;

                // Adds the client to the list of usernames
                clientNamesList.add(username);
            }

            // Add this client to the list of connected clients
            CLIENT.add(this);
            HANDLERS.add(this);

            String message = "SERVER: " + username + " has joined the chat!";
            broadcastMessage(message);
        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * Method to get the BufferedWriter for sending messages to the client.
     *
     * @return The BufferedWriter for sending messages to the client.
     */
    public BufferedWriter getBufferedWriter() {
        return writer;
    }

    /**
     * Method to get the socket connected to the client.
     *
     * @return The socket connected to the client.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Static method to add a web client to tracking.
     * Should be called when a web client connects.
     *
     * @param username The username of the connecting web client.
     */
    public static synchronized void addWebClient(String username) {
        // Adds the web client to the total count of clients
        clientTotal++;

        // Adds the web client to the list of usernames
        clientNamesList.add(username);

        System.out.println("Web user " + username + " has connected!");
    }

    /**
     * Static method to remove a web client from tracking.
     * Should be called when a web client disconnects.
     *
     * @param username The username of the disconnecting web client.
     */
    public static synchronized void removeWebClient(String username) {
        // Removes the web client from the total count of clients
        clientTotal--;

        // Removes the web client from the list of usernames
        clientNamesList.remove(username);

        System.out.println("Web user " + username + " has disconnected!");
    }

    /**
     * Static method to get the total number of connected clients.
     * This includes both socket clients and web clients.
     *
     * @return The total number of connected clients.
     */
    public static List<ClientHandler> getClientList() {
        return CLIENT;
    }

    /**
     * Default constructor for the ClientHandler class.
     * It initializes the client handler with a socket.
     */
    public String getUsername() {
        return username;
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
        HANDLERS.remove(this);

        // Use centralized tracking for socket clients
        synchronized (ClientHandler.class) {
            // Removes the client from the total count of clients
            clientTotal--;
            // Removes the client from the list of usernames
            clientNamesList.remove(username);
        }

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
     * Static method to log messages to a file.
     *
     * @param message The message to be logged.
     * @param tag     The tag to associate with the message.
     */
    public static void logMessage(String message, String tag) {
        String projectDir = System.getProperty("user.dir");
        if (projectDir == null) {
            System.err.println("Could not resolve project directory.");
            return;
        }

        String filePath = LOG_PATTERN.replace("%h", projectDir);
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

        try (FileWriter fw = new FileWriter(filePath, APPEND_MODE)) {
            String timestamped = "[" + new java.util.Date() + "] [" + tag + "] " + message;
            fw.write(timestamped + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Failed to write to log file.");
            e.printStackTrace();
        }
    }

    /**
     *
     * @param message
     */
    public static void broadcastMessageToAll(String message) {
        for (ClientHandler handler : HANDLERS) {
            handler.sendMessage(message);
        }
    }

    /**
     *
     * @param message
     */
    public void sendMessage(String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }
}
