package org.nampython.support;

import com.cyecize.ioc.services.DependencyContainer;

public class IocCenter {
    private static DependencyContainer serverDependencyContainer;
    private static DependencyContainer requestHandlersDependencyContainer;

    public static void setServerDependencyContainer(DependencyContainer serverDependencyContainer) {
        if (serverDependencyContainer != null) {
            IocCenter.serverDependencyContainer = serverDependencyContainer;

        }
    }

    public static void setRequestHandlersDependencyContainer(DependencyContainer requestHandlersDependencyContainer) {
        IocCenter.requestHandlersDependencyContainer = requestHandlersDependencyContainer;
    }

    public static DependencyContainer getServerDependencyContainer() {
        return serverDependencyContainer;
    }

    public static DependencyContainer getRequestHandlersDependencyContainer() {
        return requestHandlersDependencyContainer;
    }
}
