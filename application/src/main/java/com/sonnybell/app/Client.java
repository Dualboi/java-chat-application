package com.sonnybell.app;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Client class to handle sending and receiving messages from the server.
 * It connects to the server, sends messages, and listens for incoming messages.
 */
public class Client {
    private static final int SERVER_PORT = 1234;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;

    /**
     * Constructor to initialize the client with a socket and username.
     *
     * @param socket   The socket connected to the server.
     * @param username The username of the client.
     */
    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;

            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * Method to send messages to the server.
     * It reads user input from the console and sends it to the server.
     * It also handles the "quit" command to exit the chat.
     * The quit command is sent directly to the server without waiting for a new
     * line.
     * This allows the client to exit gracefully.
     * the quit command works with the same logic in the ClientHandler class to act
     * as handshake
     * and close the connection.
     */
    public void sendMessage() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();

                if (messageToSend.isEmpty()) {
                    continue; // skip empty messages
                }

                if (messageToSend.equalsIgnoreCase("quit")) {
                    writer.write("quit"); // send quit directly
                    writer.newLine();
                    writer.flush();
                    break; // exit the loop
                }
                writer.write(username + ": " + messageToSend);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * Method to listen for incoming messages from the server.
     * It runs in a separate thread to continuously read messages.
     */
    public void listenForMessages() {
        new Thread(() -> {
            String msgFromServer;
            try {
                while ((msgFromServer = reader.readLine()) != null) {
                    System.out.println(msgFromServer);
                }
            } catch (IOException e) {
                closeEverything();
            }
        }).start();
    }

    /**
     * Method to close all resources when done.
     * It closes the socket, reader, and writer.
     */
    public void closeEverything() {
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
     * Main method to start the client.
     * It connects to the server and starts listening for messages.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Socket socket = new Socket("localhost", SERVER_PORT);
            BufferedWriter tempWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader tempReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String serverResponse;
            while (true) {
                System.out.println("Enter server password:");
                String clientInputPassword = scanner.nextLine();

                // exit client if no password is entered
                // this is to prevent the client from hanging if the server is not
                if (clientInputPassword == null || clientInputPassword.trim().isEmpty()) {
                    System.out.println("No password entered. Exiting.");
                    socket.close();
                    return;
                }

                // Send the password to the server
                tempWriter.write(clientInputPassword);
                tempWriter.newLine();
                tempWriter.flush();

                // Wait for server response
                serverResponse = tempReader.readLine();

                if ("OK".equals(serverResponse)) {
                    break; // Password is correct, exit the loop
                } else {
                    System.out.println("Incorrect password. Please try again.");
                }
            }

            // Password is valid, proceed to get username
            System.out.print("Enter your username: ");
            String username = scanner.nextLine();
            System.out.println("Welcome to the chat application!");

            // Send username to the server
            tempWriter.write(username);
            tempWriter.newLine();
            tempWriter.flush();

            Client client = new Client(socket, username);
            client.listenForMessages();
            client.sendMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
