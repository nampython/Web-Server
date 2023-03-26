package org.nampython.core;



import org.ioc.engine.ComponentModel;
import org.ioc.stereotype.Autowired;
import org.ioc.stereotype.Service;
import org.nampython.base.BaseHttp;
import org.nampython.base.Controller;
import org.nampython.core.container.ServletContainer;
import org.nampython.support.IocCenter;


import java.io.File;
import java.net.URL;
import java.util.*;

/**
 *
 */
@Service
public class LoadingRequestHandler implements InitLoadingRequest {
    /**
     * {@link LinkedList<RequestHandler>} Store all the classes that implement {@link RequestHandler}
     * It includes{@code} {@link ServletContainer}, {@link org.nampython.core.engine.RequestProcessor}, {@link org.nampython.core.engine.ResourceHandler}, {@link org.nampython.core.engine.FallbackHandler}
     */
    private final LinkedList<RequestHandler> requestHandlers;
    /**
     * {@link List<RequestDestroy>}  Store all the classes that implement {@link RequestDestroy}
     * It only for {@link RequestDestroyHandler}
     */
    private final List<RequestDestroy> destroyHandlers;

    @Autowired
    public LoadingRequestHandler() {
        this.requestHandlers = new LinkedList<>();
        this.destroyHandlers = new ArrayList<>();
    }

    /**
     * Include
     * @param requestHandlerFileNames requestHandlerFileNames
     * @param libURLs libURLs
     * @param apiURLs apiURLs
     */
    @Override
    public void loadRequestHandlers(List<String> requestHandlerFileNames, Map<File, URL> libURLs, Map<File, URL> apiURLs) {
        this.handlerRequestHandlers();
        this.handlerRequestDestroyHandlers();
    }

    /**
     *
     */
    private void handlerRequestDestroyHandlers() {
        for (ComponentModel implementation : IocCenter.getServerDependencyContainer().getImplementations(RequestDestroy.class)) {
            this.destroyHandlers.add((RequestDestroy) implementation.getInstance());
        }
    }

    /**
     * Get all instances that implement  {@link RequestHandler} and call the init method
     */
    private void handlerRequestHandlers() {
        Collection<ComponentModel> implementationOfRequestHandlers =  IocCenter.getServerDependencyContainer().getImplementations(RequestHandler.class);
        for (ComponentModel next : implementationOfRequestHandlers) {
            RequestHandler instance = (RequestHandler) next.getInstance();
            this.requestHandlers.add(instance);
        }
        requestHandlers.sort(Comparator.comparingInt(RequestHandler::order));
        this.callInitRequestHandler(requestHandlers);
    }

    /**
     *  Need to call the init method of each implement {@link RequestHandler}. But the specified init method is just called
     * in the {@link ServletContainer} and {@link org.nampython.core.engine.ResourceHandler}
     * At {@link ServletContainer} we will find all class that having {@link Controller} annotation and
     * extends {@link BaseHttp}. Besides, We need also to decide the application's name.
     * @param requestHandlers List of the requestHandlers
     */
    private void callInitRequestHandler(List<RequestHandler> requestHandlers) {
        for (RequestHandler requestHandler : this.requestHandlers) {
            requestHandler.init();
        }
    }

    @Override
    public List<RequestHandler> getRequestHandlers() {
        return this.requestHandlers;
    }

    @Override
    public List<RequestDestroy> getRequestDestroyHandlers() {
        return this.destroyHandlers;
    }
}
