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

        ClientHandler handlerToRemove = null;
        for (ClientHandler handler : ClientHandler.getClientList()) {
            if (handler.getUsername().equals(usernameToRemove)) {
                handlerToRemove = handler;
                break;
            }
        }

        if (handlerToRemove != null) {
            try {
                // Get the existing connected socket from the client handler
                Socket clientSocket = handlerToRemove.getSocket();

                // Create a temporary writer to send quit message to the client
                BufferedWriter tempWriter = new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()));

                // Directly tell the user's client to quit via a quit message sent to the server
                // using the Client.java method to catch the word quit
                tempWriter.write("quit");
                tempWriter.newLine();
                tempWriter.flush();

                // Give the client a moment to process the quit message
                Thread.sleep(CLIENT_QUIT_PROCESSING_DELAY_MS);

                // Close the temporary writer
                tempWriter.close();

                // Create the admin removal message
                String message = "SERVER: " + usernameToRemove + " has been removed by an admin.";
                String tag = "Moderation";

                // Broadcast the admin removal message to other clients
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

                // Log the admin removal message
                handlerToRemove.logMessage(message, tag);
                System.out.println(message);

                // Then close the server-side resources for that user (this will trigger "left
                // the chat" message)
                handlerToRemove.closeEverything();

                return true;
            } catch (java.io.IOException e) {
                System.err.println("Moderation: IOException during removal for user "
                        + usernameToRemove + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            } catch (InterruptedException e) {
                System.err.println("Moderation: InterruptedException during removal process for user "
                        + usernameToRemove + ": " + e.getMessage());
                Thread.currentThread().interrupt(); // Restore the interrupted status
                return false;
            }
        } else {
            System.out.println("Moderation: User " + usernameToRemove + " not found in local client list.");
            return false;
        }
    }
}
