package org.nampython.core;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 *
 */
public class ConnectionHandler implements Runnable {
    private final Socket socketClient;
    private final List<RequestHandler> requestHandlers;
    private final List<RequestDestroy> requestDestroys;

    public ConnectionHandler(Socket socketClient, List<RequestHandler> requestHandlers, List<RequestDestroy> requestDestroys) {
        this.socketClient = socketClient;
        this.requestHandlers = requestHandlers;
        this.requestDestroys = requestDestroys;
    }

    /**
     * Override run method from {@link Runnable}
     */
    @Override
    public void run() {
        try {
            this.handlerRequest();
            socketClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Each class that implement {@link RequestHandler} store handleRequest method.
     * @throws IOException -  if an I/O error occurs when creating the input stream, the socket is closed, the socket is not connected,
     * or the socket input has been shutdown using shutdownInput()
     */
    private void handlerRequest() throws IOException {
        RequestHandlerShareData sharedData = new RequestHandlerShareData();
        for (RequestHandler requestHandler : this.requestHandlers) {
            boolean isRequestHandler = requestHandler.handleRequest(this.socketClient.getInputStream(), this.socketClient.getOutputStream(), sharedData);
            if (isRequestHandler) {
                break;
            }
        }
        this.handlerDestroy(sharedData);
    }

    /**
     *
     * @param shareData
     */
    private void handlerDestroy(RequestHandlerShareData shareData) {
        for (RequestDestroy requestDestroy : this.requestDestroys) {
            requestDestroy.destroy(shareData);
        }
    }
}
