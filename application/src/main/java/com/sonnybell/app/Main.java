package com.sonnybell.app;

/**
 * Main class to start the application.
 * It serves as the entry point for the application.
 * It initializes the server or client based on the command line argument.
 * Usage: java -jar your-app.jar <server|client>
 * @author Sonny Bell
 */
public interface Main {

    /**
     * Main method to start the application.
     * It checks the command line argument to determine whether to start the server or client.
     * If the argument is missing or incorrect, it prints usage instructions and exits.
     *
     * @param args Command line arguments, expected to be either "server" or "client".
     */
    static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar your-app.jar <server|client>");
            System.exit(1); // Exit the program if the argument is missing or incorrect
        }

        String mode = args[0];

        if ("server".equalsIgnoreCase(mode)) {
            System.out.println("Starting the server...");
            // Start server logic
            Server.main(args); // Call server main method
        } else if ("client".equalsIgnoreCase(mode)) {
            System.out.println("Starting the client...");
            // Start client logic
            Client.main(args); // Call client main method
        } else {
            System.out.println("Invalid argument. Use 'server' or 'client'.");
            System.exit(1);
        }
    }
}
