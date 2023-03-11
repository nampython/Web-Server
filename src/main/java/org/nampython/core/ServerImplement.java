package org.nampython.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 *
 */
public class ServerImplement extends BaseServer {
    private static final int SOCKET_TIMEOUT_MILLISECONDS;
    private static final String LISTENING_MESSAGE_FORMAT;

    static {
        SOCKET_TIMEOUT_MILLISECONDS = 60000;
        LISTENING_MESSAGE_FORMAT = "http://localhost:%d";
    }

    private final InitLoadingRequest initLoadingRequest;
    private final int port;

    public ServerImplement(InitLoadingRequest initLoadingRequest, int port) {
        this.initLoadingRequest = initLoadingRequest;
        this.port = port;
    }

    /**
     * Socket is opening for listening the connections from the client and with each the new request need to open
     * a new thread where the connection is handled.
     * @throws IOException
     */
    @Override
    public void run() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(this.port);
        serverSocket.setSoTimeout(SOCKET_TIMEOUT_MILLISECONDS);
        System.out.println(String.format(LISTENING_MESSAGE_FORMAT, this.port));
        while (true) {
            while (true) {
                try {
                    final Socket client = serverSocket.accept();
                    client.setSoTimeout(SOCKET_TIMEOUT_MILLISECONDS);
                    final Thread thread = new Thread(new ConnectionHandler(
                            client,
                            this.initLoadingRequest.getRequestHandlers(),
                            this.initLoadingRequest.getRequestDestroyHandlers()
                    ));
                    thread.start();
                } catch (SocketTimeoutException ignored) {
                }
            }
        }
    }
}
