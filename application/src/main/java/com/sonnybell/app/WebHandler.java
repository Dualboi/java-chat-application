package com.sonnybell.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.time.Duration;

/**
 * WebHandler class that implements HttpHandler to handle HTTP requests.
 * It loads files from the resources directory and serves them as HTTP
 * responses.
 */
@SuppressWarnings("restriction")
public class WebHandler implements HttpHandler {
    private Instant serverStartTime;

    public WebHandler(Instant serverStartTime) {
        this.serverStartTime = serverStartTime;
    }

    /**
     * Loads a file from the resources directory.
     *
     * @param fileName The name of the file to load.
     * @return The content of the file as a String, or null if the file is not
     *         found.
     */
    private String loadFile(String fileName) {
        String fileContent = null;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                // Use a ByteArrayOutputStream to read the InputStream into a byte array
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, length);
                }
                // Convert the byte array to a string
                fileContent = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
            }
        } catch (IOException e) {
            e.printStackTrace(); // Log the error if necessary, but avoid printing stack traces to users.
        }
        return fileContent;
    }

    /**
     * Handles HTTP requests.
     * This method is called when a request is received.
     * handles html injection of java variables into the html file.
     * is a robust logic for any other html redirects within the index page.
     *
     * @param exchange The HttpExchange object containing the request and response.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        OutputStream out = exchange.getResponseBody();
        String requestPath = exchange.getRequestURI().getPath();

        // Treat "/" as "/index.html"
        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        // Strip leading slash and treat as file name
        String fileName = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;

        // Try to load the file
        String responseContent = loadFile(fileName);
        if (responseContent == null) {
        } else {
            if (fileName.endsWith(".html")) {
                // Calculate server uptime
                Duration uptime = Duration.between(serverStartTime, Instant.now());
                long hours = uptime.toHours();
                long minutes = uptime.toMinutes() % 60;
                long seconds = uptime.getSeconds() % 60;

                String uptimeMessage = String.format("Server uptime: %02d:%02d:%02d", hours, minutes, seconds);

                // Inject uptime message into HTML content
                responseContent = responseContent.replace("{{SERVER_UPTIME}}", uptimeMessage);

                // Accessing the total number of clients connected 
                String totalClientsMs = String.format("Total clients connected: %d", ClientHandler.clientTotal);
                // Inject total number of clients connected variable into the CurrentClients page
                responseContent = responseContent.replace("{{TOTAL_CLIENTS}}", totalClientsMs);

                // Accessing the list of client names and adding line breaks
                String clientNamesListMs = String.join("<br>", ClientHandler.clientNamesList);
                // Inject all the clients connected names into the CurrentClients page
                responseContent = responseContent.replace("{{CURRENT_USERS}}", clientNamesListMs);
            }
        }

        // If the file is not found, return a 404 error
        byte[] responseBytes = responseContent.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
        out.write(responseBytes);
        out.flush();
        out.close();
    }
}
