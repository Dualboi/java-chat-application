package com.sonnybell.app;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Utility class for moderation-related functionalities.
 * Used for removing users from the chat server,
 * used by admin only.
 */
public interface Moderation {

    /**
     * Delay in milliseconds to allow the client to process the quit message.
     */
    int CLIENT_QUIT_PROCESSING_DELAY_MS = 100;

    /**
     * Removes a user directly from the server's client list.
     * This method is intended to be used by an admin.
     *
     * @param usernameToRemove The username of the user to be removed.
     * @return true if the user was found and removal was initiated, false
     *         otherwise.
     */
    static boolean removeUserDirectly(String usernameToRemove) {
        if (usernameToRemove == null || usernameToRemove.trim().isEmpty()) {
            System.err.println("Moderation: Username to remove cannot be null or empty.");
            return false;
        }

        // First, check if it's a socket client
        ClientHandler handlerToRemove = null;
        for (ClientHandler handler : ClientHandler.getClientList()) {
            if (handler.getUsername().equals(usernameToRemove)) {
                handlerToRemove = handler;
                break;
            }
        }

        if (handlerToRemove != null) {
            // Handle socket client removal
            try {
                Socket clientSocket = handlerToRemove.getSocket();
                BufferedWriter tempWriter = new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()));

                tempWriter.write("quit");
                tempWriter.newLine();
                tempWriter.flush();

                // Allow some time for the client to process the quit message
                Thread.sleep(CLIENT_QUIT_PROCESSING_DELAY_MS);
                tempWriter.close();

                String message = "SERVER: " + usernameToRemove + " has been removed by an admin.";
                String tag = "Moderation";

                // Broadcast removal message to all socket clients
                for (ClientHandler client : ClientHandler.getClientList()) {
                    if (!client.getUsername().equals(usernameToRemove)) {
                        try {
                            client.getBufferedWriter().write(message);
                            client.getBufferedWriter().newLine();
                            client.getBufferedWriter().flush();
                        } catch (java.io.IOException e) {
                        }
                    }
                }

                // Log the removal message
                handlerToRemove.logMessage(message, tag);
                System.out.println(message);
                handlerToRemove.closeEverything();

                // Remove the handler from the client list
                return true;
            } catch (java.io.IOException | InterruptedException e) {
                System.err.println("Moderation: Error during socket client removal: " + e.getMessage());
                return false;
            }
        } else {
            // Check if it's a web client by looking at the centralized username list
            if (ClientHandler.getClientNamesList().contains(usernameToRemove)) {
                // It's a web client, remove it using the web client removal method
                ClientHandler.removeWebClient(usernameToRemove);

                // Also remove from the WebChat WEB_USERS set
                WebChat.removeFromWebUsers(usernameToRemove);

                String message = "SERVER: " + usernameToRemove + " has been removed by an admin.";

                // Broadcast removal message to all socket clients
                for (ClientHandler client : ClientHandler.getClientList()) {
                    try {
                        client.getBufferedWriter().write(message);
                        client.getBufferedWriter().newLine();
                        client.getBufferedWriter().flush();
                    } catch (java.io.IOException e) {
                    }
                }

                System.out.println(message);
                return true;
            } else {
                System.out.println("Moderation: User " + usernameToRemove + " not found.");
                return false;
            }
        }
    }
}
