package org.nampython.base;

public interface HttpHandler {

    void init(SoletConfig soletConfig);

    void service(HttpRequest request, HttpResponse response) throws Exception;

    boolean isInitialized();

    boolean hasIntercepted();

    SoletConfig getSoletConfig();
}
