package com.sonnybell.app;

/**
 * Main class to start the application.
 * It determines whether to run the server or client based on command line arguments.
 * Usage: java -jar your-app.jar <server|client>
 * @author Sonny Bell
 */
public class Main {
    public static void main(String[] args) {
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
