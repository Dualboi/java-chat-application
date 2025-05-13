package com.sonnybell.app;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server class to handle incoming client connections.
 * It accepts client connections and starts a new thread for each client.
 */
public class Server {
    private static final int SERVER_PORT = 1234;
    private static final int WEB_PORT = 8080;
    private ServerSocket serverSocket;

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
                System.out.println("A new user has connected!");
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();
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
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Server server = new Server(serverSocket);
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
