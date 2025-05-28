package com.sonnybell.app;

/**
 * Interface to listen for incoming messages.
 * It can be implemented by other classes to handle messages.
 */
public interface MessageListener {
    void onMessageReceived(String message);
}
