package org.nampython.creation;

import com.cyecize.ioc.annotations.Bean;
import com.cyecize.ioc.annotations.PostConstruct;
import org.nampython.config.*;
import org.nampython.support.JarFileUnzip;
import org.nampython.support.JarFileUnzipImplement;
import org.nampython.type.ServerComponent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;


@ServerComponent
public class BeanCenter {
    private final static String EXTENSION_JAR = ".jar";
    public static Integer port;
    public static Map<String, Object> configs;
    public static Class<?> mainClass;

    @PostConstruct
    public void init() {
        this.initConfigs();
    }

    /**
     *
     */
    private void initConfigs() {
        //Since there is not app output directory.
        configs.putIfAbsent(ConfigValue.MAIN_APP_JAR_NAME.name(), "");
        //There is no "classes" folder inside the jar file so we set it to empty.
        configs.put(ConfigValue.APP_COMPILE_OUTPUT_DIR_NAME.name(), "");
        //We want to stay on the same level.
        configs.put(ConfigValue.WEB_APPS_DIR_NAME.name(), "./");

    }

    /**
     *
     */
    @Bean
    public ConfigCenter configHandler() throws IOException {
        final ConfigCenter configCenter = new AdditionalConfig(configs);
        int port;
        if (configCenter.getConfigValue(ConfigValue.SERVER_PORT, int.class) == ConstantsPool.EMPTY_PORT) {
            if (BeanCenter.port != null) {
                port = BeanCenter.port;
            } else {
                port = ConstantsPool.DEFAULT_SERVER_PORT;
            }
            configCenter.addConfigParam(ConfigValue.SERVER_PORT, port);
        }
        configCenter.addConfigParam(ConfigValue.JAVACHE_WORKING_DIRECTORY, this.getWorkingDir());
        return configCenter;
    }

    /**
     * Gets the server's working directory.
     * If the app is in a jar file, it will extract it and return the directory to that folder.
     *
     * @return working directory.
     */
    private String getWorkingDir() throws IOException {
        String workingDir;
        try {
            final URI uri = mainClass.getProtectionDomain().getCodeSource().getLocation().toURI();
            workingDir = Path.of(uri).toString();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        if (workingDir.endsWith(".jar")) {
            final JarFileUnzip unzipService = new JarFileUnzipImplement();
            unzipService.unzipJar(new File(workingDir), false, workingDir.replace(".jar", ""));
            workingDir = workingDir.replace(".jar", "");
        }

        System.out.println(String.format("Working Directory: %s", workingDir));

        return workingDir;
    }
}
