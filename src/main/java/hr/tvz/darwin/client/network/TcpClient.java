package hr.tvz.darwin.client.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class TcpClient {

    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Object> onMessage;
    private volatile boolean running;

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        running = true;
        Thread.ofVirtual().start(this::listenLoop);
    }

    private void listenLoop() {
        try {
            while (running) {
                Object payload = in.readObject();
                if (onMessage != null) {
                    onMessage.accept(payload);
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("TcpClient listener error: " + e.getMessage());
            }
        }
    }

    public synchronized void send(Object payload) {
        try {
            out.writeObject(payload);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("TcpClient send error: " + e.getMessage());
        }
    }

    public void setOnMessage(Consumer<Object> handler) {
        this.onMessage = handler;
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
