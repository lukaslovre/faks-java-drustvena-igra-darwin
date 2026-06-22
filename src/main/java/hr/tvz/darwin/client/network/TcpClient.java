package hr.tvz.darwin.client.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcpClient {
    private static final Logger LOGGER = Logger.getLogger(TcpClient.class.getName());

    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Object> onMessage;

    // disconnect() writes while the listener virtual thread reads this flag.
    private volatile boolean running;

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        // Match the server's output-before-input order to exchange stream headers.
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
        } catch (Exception _) {
            if (running) {
                LOGGER.warning("TcpClient connection lost unexpectedly.");
            }
        }
    }

    public synchronized void send(Object payload) {
        try {
            out.writeObject(payload);
            out.flush();
            out.reset();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "TcpClient send error", e);
        }
    }

    public void setOnMessage(Consumer<Object> handler) {
        this.onMessage = handler;
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException _) {
            // Closing an already-failed connection needs no recovery.
        }
    }
}
