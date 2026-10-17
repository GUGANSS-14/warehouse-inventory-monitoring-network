package com.wimn.practicals;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalTcpChatServer {
    private static final Logger LOGGER = Logger.getLogger(LocalTcpChatServer.class.getName());
    private ServerSocket serverSocket;
    private final int port;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private volatile boolean running = false;
    private Consumer<String> logListener;

    public LocalTcpChatServer(int port, Consumer<String> logListener) {
        this.port = port;
        this.logListener = logListener;
    }

    public synchronized void startServer() throws IOException {
        if (running) return;
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"));
        running = true;
        log("TCP Server listening on 127.0.0.1:" + port);

        Thread.ofVirtual().start(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket);
                    clients.add(handler);
                    clientPool.submit(handler);
                    log("Accepted connection from " + socket.getRemoteSocketAddress());
                } catch (IOException e) {
                    if (running) {
                        log("Server accept exception: " + e.getMessage());
                    }
                }
            }
        });
    }

    public synchronized void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler ch : clients) {
                ch.close();
            }
            clients.clear();
            log("TCP Server stopped.");
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Error closing server socket", e);
        }
    }

    public void broadcast(String sender, String message) {
        log("[" + sender + "]: " + message);
        for (ClientHandler c : clients) {
            c.sendMessage("[" + sender + "]: " + message);
        }
    }

    private void log(String msg) {
        if (logListener != null) {
            logListener.accept(msg);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getClientCount() {
        return clients.size();
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String clientName = "Client";

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                writer.println("CONNECTED to WIMN TCP Chat Server on 127.0.0.1:" + port);
                writer.println("Send 'NAME <your_name>' to set alias, or just type messages.");

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("NAME ")) {
                        this.clientName = line.substring(5).trim();
                        writer.println("Alias updated to: " + clientName);
                        log("Client identified as: " + clientName);
                    } else if (line.equalsIgnoreCase("QUIT")) {
                        break;
                    } else {
                        broadcast(clientName, line);
                    }
                }
            } catch (IOException e) {
                log("Connection lost with " + clientName);
            } finally {
                close();
                clients.remove(this);
            }
        }

        public void sendMessage(String msg) {
            if (writer != null) {
                writer.println(msg);
            }
        }

        public void close() {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}
