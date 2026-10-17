package com.wimn.practicals;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class LocalTcpChatClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = false;
    private final Consumer<String> messageReceiver;

    public LocalTcpChatClient(Consumer<String> messageReceiver) {
        this.messageReceiver = messageReceiver;
    }

    public synchronized void connect(String host, int port, String clientName) throws IOException {
        if (connected) return;
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        connected = true;

        if (clientName != null && !clientName.isEmpty()) {
            writer.println("NAME " + clientName);
        }

        Thread.ofVirtual().start(() -> {
            try {
                String line;
                while (connected && (line = reader.readLine()) != null) {
                    if (messageReceiver != null) {
                        messageReceiver.accept(line);
                    }
                }
            } catch (IOException e) {
                if (connected && messageReceiver != null) {
                    messageReceiver.accept("[SYSTEM]: Connection closed by server.");
                }
            } finally {
                disconnect();
            }
        });
    }

    public void sendMessage(String msg) {
        if (writer != null && connected) {
            writer.println(msg);
        }
    }

    public synchronized void disconnect() {
        connected = false;
        try {
            if (writer != null) {
                writer.println("QUIT");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return connected;
    }
}
