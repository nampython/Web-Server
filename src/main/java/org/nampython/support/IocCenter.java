package org.nampython.support;


import org.ioc.contex.ApplicationContext;

public class IocCenter {
    private static ApplicationContext serverDependencyContainer;
    private static ApplicationContext requestHandlersDependencyContainer;

    public static void setServerDependencyContainer(ApplicationContext serverDependencyContainer) {
        if (serverDependencyContainer != null) {
            IocCenter.serverDependencyContainer = serverDependencyContainer;

        }
    }

    public static void setRequestHandlersDependencyContainer(ApplicationContext requestHandlersDependencyContainer) {
        IocCenter.requestHandlersDependencyContainer = requestHandlersDependencyContainer;
    }

    public static ApplicationContext getServerDependencyContainer() {
        return serverDependencyContainer;
    }

    public static ApplicationContext getRequestHandlersDependencyContainer() {
        return requestHandlersDependencyContainer;
    }
}
