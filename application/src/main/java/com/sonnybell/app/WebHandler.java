package com.sonnybell.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * WebHandler class that implements HttpHandler to handle HTTP requests.
 * It loads files from the resources directory and serves them as HTTP responses.
 */
@SuppressWarnings("restriction")
public class WebHandler implements HttpHandler {

    /**
     * Loads a file from the resources directory.
     *
     * @param fileName The name of the file to load.
     * @return The content of the file as a String, or null if the file is not found.
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
        e.printStackTrace();  // Log the error if necessary, but avoid printing stack traces to users.
    }
    return fileContent;
}


    /**
     * Handles HTTP requests.
     * This method is called when a request is received.
     *
     * @param exchange The HttpExchange object containing the request and response.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        OutputStream out = exchange.getResponseBody();

        // Load the file content
        String fileContent = loadFile("index.html");

        // If file not found, return a 404
        if (fileContent == null) {
            String notFoundMessage = "File not found!";
            exchange.sendResponseHeaders(404, notFoundMessage.getBytes().length);
            out.write(notFoundMessage.getBytes());
        } else {
            // If file is found, return the content
            byte[] responseBytes = fileContent.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            out.write(responseBytes);
        }

        out.flush();
        out.close();
    }
}
