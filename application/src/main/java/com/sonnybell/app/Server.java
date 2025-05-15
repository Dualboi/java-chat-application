package com.sonnybell.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Server class to handle incoming client connections.
 * It accepts client connections and starts a new thread for each client.
 */
public class Server {
    private static final int SERVER_PORT = 1234;
    private static final int WEB_PORT = 8080;
    private ServerSocket serverSocket;
    public static String serverPass;

    /**
     * Constructor to initialize the server with a ServerSocket.
     *
     * @param serverSocket The ServerSocket to accept client connections.
     */
    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Method to start the server and accept client connections.
     * It runs in a loop to continuously accept new clients.
     */
    public void startServer() {
        WebServer webServer = new WebServer(WEB_PORT);
        webServer.run();

        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                // Temporary input/output streams for password check
                BufferedReader tempReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter tempWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                String receivedPassword;

                while (true) {
                    receivedPassword = tempReader.readLine();

                    if (receivedPassword == null) {
                        System.out.println("Client disconnected before entering a password.");
                        socket.close();
                        break;
                    }

                    if (receivedPassword.equals(serverPass)) {
                        tempWriter.write("OK");
                        tempWriter.newLine();
                        tempWriter.flush();

                        ClientHandler clientHandler = new ClientHandler(socket);
                        Thread thread = new Thread(clientHandler);
                        thread.start();
                        break;
                    } else {
                        tempWriter.write("Incorrect password. Please try again.");
                        tempWriter.newLine();
                        tempWriter.flush();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Method to close the server socket.
     * It ensures that the server socket is closed properly.
     */
    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method to start the server.
     * It creates a new ServerSocket and starts the server.
     *
     * @param args Command line arguments (not used).
     * @throws IOException If an I/O error occurs when creating the server socket.
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Create a password to secure the server:");
            String serverPass = scanner.nextLine();
            Server.serverPass = serverPass;
            System.out.println("Server password set to: " + serverPass);
            System.out.println("Server is starting...");

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Server server = new Server(serverSocket);
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
