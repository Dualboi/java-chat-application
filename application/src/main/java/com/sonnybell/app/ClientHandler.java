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
            this.username = reader.readLine(); // First message is username
            CLIENT.add(this);
            broadcastMessage("SERVER: " + username + " has joined the chat!");
        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * Method to send messages to all connected clients.
     * It runs in a separate thread to continuously read messages.
     */
    @Override
    public void run() {
        String message;
        try {
            while (socket.isConnected() && (message = reader.readLine()) != null) {
                broadcastMessage(message);
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
     */
    public void removeClientHandler() {
        CLIENT.remove(this);
        broadcastMessage("SERVER: " + username + " has left the chat.");
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
}
