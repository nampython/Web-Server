package org.nampython;

import com.cyecize.ioc.MagicInjector;
import com.cyecize.ioc.config.MagicConfiguration;
import com.cyecize.ioc.services.DependencyContainer;
import org.nampython.config.ConfigCenter;
import org.nampython.config.ConfigValue;
import org.nampython.core.BaseServer;
import org.nampython.core.InitLoadingRequest;
import org.nampython.core.ServerImplement;
import org.nampython.creation.BeanCenter;
import org.nampython.support.IocCenter;
import org.nampython.support.Logger;
import org.nampython.type.ServerComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StartServer {

    /**
     * Run the application by calling this function.
     * @param port A port number is the logical address of each application or process that uses a network or the Internet to communicate.
     * @param serverInitializationClass the entry class for the application
     */
    public static void run(Integer port, Class<?> serverInitializationClass) {
        run(port, new HashMap<>(), serverInitializationClass);
    }

    /**
     * @param port the port number
     * @param config the configuration for the application
     * @param serverInitializationClass the entry class for the application
     */
    public static void run(Integer port, Map<String, Object> config, Class<?> serverInitializationClass) {
        run(port, config, serverInitializationClass, null);
    }

    /**
     * TODO
     * @param port port
     * @param config config
     * @param serverInitializationClass the entry point class
     * @param onServerLoadedEvent Runnable
     */
    private static void run(Integer port, Map<String, Object> config, Class<?> serverInitializationClass, Runnable onServerLoadedEvent) {
        Logger loggingService = null;

        try {
            MagicConfiguration magicConfiguration = new MagicConfiguration()
                    .scanning()
                    .addCustomServiceAnnotation(ServerComponent.class)
                    .and()
                    .build();
            BeanCenter.port = 8080;
            BeanCenter.mainClass = serverInitializationClass;
            BeanCenter.configs = new HashMap<>();
            final DependencyContainer dependencyContainer = MagicInjector.run(StartServer.class, magicConfiguration);
            IocCenter.setServerDependencyContainer(dependencyContainer);
            IocCenter.setRequestHandlersDependencyContainer(dependencyContainer);
            loadRequestHandlers(dependencyContainer.getService(InitLoadingRequest.class));
            loggingService = dependencyContainer.getService(Logger.class);
            final BaseServer server = new ServerImplement(
                    dependencyContainer.getService(InitLoadingRequest.class),
                    dependencyContainer.getService(ConfigCenter.class).getConfigValue(ConfigValue.SERVER_PORT, int.class)
            );
            if (onServerLoadedEvent != null) {
                onServerLoadedEvent.run();
            }
            server.run();
        } catch (Exception ex) {
            if (loggingService != null) {
                loggingService.printStackTrace(ex);
            } else {
                ex.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     *
     * @param dependencyContainer {@link DependencyContainer} Contains all Services
     * @param <T>
     */
    private static <T> void loadRequestHandlers(T dependencyContainer) {
        InitLoadingRequest initLoadingRequest = (InitLoadingRequest) dependencyContainer;
        initLoadingRequest.loadRequestHandlers(new ArrayList<>(), null, null);
    }
}
