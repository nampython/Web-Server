package org.nampython.base;

import static org.nampython.base.api.HttpStatus.NOT_IMPLEMENTED;


public abstract class BaseHttp implements HttpHandler {
    private boolean isInitialized;
    private boolean hasIntercepted;
    private SoletConfig soletConfig;

    protected BaseHttp() {
        this.isInitialized = false;
        this.setHasIntercepted(true);
    }

    /**
     * Create proper route having the app name in mind.
     *
     * @param route - required route.
     * @return formatted route.
     */
    protected String createRoute(String route) {
        return this.soletConfig.getAttribute(SoletConstants.SOLET_CONFIG_APP_NAME_PREFIX) + route;
    }

    /**
     * The GET method requests a representation of the specified resource. Requests using GET should only retrieve data.
     * @param request {@link HttpRequest}
     * @param response {@link HttpResponse}
     */
    protected void doGet(HttpRequest request, HttpResponse response) {
        this.functionalityNotFound(request, response);
    }

    /**
     * The POST method submits an entity to the specified resource, often causing a change in state or side effects on the server.
     * @param request {@link HttpRequest}
     * @param response {@link HttpResponse}
     */
    protected void doPost(HttpRequest request, HttpResponse response) {
        this.functionalityNotFound(request, response);
    }

    /**
     * The PUT method replaces all current representations of the target resource with the request payload.
     * @param request {@link HttpRequest}
     * @param response {@link HttpResponse}
     */
    protected void doPut(HttpRequest request, HttpResponse response) {
        this.functionalityNotFound(request, response);
    }

    /**
     * The DELETE method deletes the specified resource.
     * @param request {@link HttpRequest}
     * @param response {@link HttpResponse}
     */
    protected void doDelete(HttpRequest request, HttpResponse response) {
        this.functionalityNotFound(request, response);
    }

    protected void setHasIntercepted(boolean hasIntercepted) {
        this.hasIntercepted = hasIntercepted;
    }

    @Override
    public void init(SoletConfig soletConfig) {
        this.soletConfig = soletConfig;
        this.soletConfig.setIfMissing(SoletConstants.SOLET_CONFIG_APP_NAME_PREFIX, "");
        this.isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return this.isInitialized;
    }

    @Override
    public boolean hasIntercepted() {
        return this.hasIntercepted;
    }

    @Override
    public SoletConfig getSoletConfig() {
        return this.soletConfig;
    }

    /**
     *
     * @param request {@link HttpRequest}
     * @param response {@link HttpResponse}
     */
    @Override
    public void service(HttpRequest request, HttpResponse response) {
        switch (request.getMethod().toUpperCase()) {
            case "GET":
                this.doGet(request, response);
                break;
            case "POST":
                this.doPost(request, response);
                break;
            case "PUT":
                this.doPut(request, response);
                break;
            case "DELETE":
                this.doDelete(request, response);
                break;
            default:
                this.functionalityNotFound(request, response);
                break;
        }
    }

    /**
     *
     * @param request {@link HttpRequest}
     * @param response {@link HttpResponse}
     */
    private void functionalityNotFound(HttpRequest request, HttpResponse response) {
        response.setStatusCode(NOT_IMPLEMENTED);
        response.addHeader("Content-Type", "text/html");
        response.setContent((String.format(
                "<h1>[ERROR] %s %s </h1><br/><h3>[MESSAGE] The page or functionality you are looking for is not found.</h3>",
                request.getMethod(),
                request.getRequestURL()))
        );
    }
}