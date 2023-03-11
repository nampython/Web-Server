package org.nampython.core.engine;


import com.cyecize.ioc.annotations.Autowired;
import com.cyecize.ioc.annotations.Service;
import org.nampython.base.*;
import org.nampython.base.api.BaseHttpRequest;
import org.nampython.base.api.BaseHttpResponse;
import org.nampython.base.api.HttpStatus;
import org.nampython.config.ConfigCenter;
import org.nampython.config.ConfigValue;
import org.nampython.core.RequestHandler;
import org.nampython.core.RequestHandlerShareData;
import org.nampython.core.SessionManagement;
import org.nampython.support.IocCenter;
import org.nampython.support.PathUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 */
@Service
public class Dispatcher implements RequestHandler {
    private static final String MISSING_SOLET_ANNOTATION_FORMAT = "Missing solet annotation for class named %s.";
    public static final String CONFIG_ASSETS_DIR;
    public static final String CFG_WORKING_DIR;
    public static final String CONFIG_APP_NAME_PREFIX;
    public static final String CONFIG_SESSION_STORAGE_KEY;
    public static final String CONFIG_SERVER_CONFIG_SERVICE_KEY;
    public static final String CONFIG_DEPENDENCY_CONTAINER_KEY;
    public static final String CONFIG_LOGGER;

    static {
        CONFIG_ASSETS_DIR = "cfg.assets.dir";
        CFG_WORKING_DIR = "cfg.working.dir";
        CONFIG_APP_NAME_PREFIX = "cfg.app.name.prefix";
        CONFIG_SESSION_STORAGE_KEY = "cfg.session.storage";
        CONFIG_SERVER_CONFIG_SERVICE_KEY = "cfg.javache.config";
        CONFIG_DEPENDENCY_CONTAINER_KEY = "cfg.javache.dependency.container";
        CONFIG_LOGGER = "cfg.logger";
    }

    private final String assetsDir;
    private boolean isRootDir;
    private final String webappsDir;
    private final ConfigCenter configCenter;
    private final SessionManagement sessionManagement;
    private DispatcherConfig<String> dispatcherConfig;
    private final boolean trackResources;
    private final String rootAppName;
    private Map<String, HttpHandler> storageControllers;
    private final Map<String, List<Class<HttpHandler>>> controllerClasses = new HashMap<>();
    private List<String> applicationNames;

    @Autowired
    public Dispatcher(ConfigCenter configCenter, SessionManagement sessionManagement) {
        this.configCenter = configCenter;
        this.sessionManagement = sessionManagement;
        this.assetsDir = this.getAssetsDir();
        this.webappsDir = this.getWebappsDir();
        this.rootAppName = configCenter.getConfigValue(ConfigValue.MAIN_APP_JAR_NAME);
        this.controllerClasses.put(this.rootAppName, new ArrayList());
        this.isRootDir = true;
        this.loadLibraries();
        this.trackResources = configCenter.getConfigValue(ConfigValue.BROCCOLINA_TRACK_RESOURCES, boolean.class);
        storageControllers = new HashMap<>();
    }

    @Override
    public boolean handleRequest(InputStream inputStream, OutputStream outputStream, RequestHandlerShareData sharedData) throws IOException {
        final HttpRequest request = new HttpRequestImpl(sharedData.getObject(RequestHandlerShareData.HTTP_REQUEST, BaseHttpRequest.class));
        final HttpResponse response = new HttpResponseImpl(sharedData.getObject(RequestHandlerShareData.HTTP_RESPONSE, BaseHttpResponse.class));
        final HttpHandler solet = this.findSoletCandidate(request);

        this.sessionManagement.initSessionIfExistent(request);
        if (solet == null || request.isResource() && !this.trackResources) {
            return false;
        } else if (!this.runSolet(solet, request, response)) {
            return false;
        } else {
            if (response.getStatusCode() == null) {
                response.setStatusCode(HttpStatus.OK);
            }
            this.sessionManagement.sendSessionIfExistent(request, response);
            this.sessionManagement.clearInvalidSessions();
            outputStream.write(response.getBytes());
            return true;
        }
    }

    /**
     *
     */
    @Override
    public void init() {
        try {
            this.storageControllers = this.loadApplications(this.createDispatcherConfig());
            this.applicationNames = this.getApplicationNames();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param dispatcherConfig
     * @return
     * @throws IOException
     */
    public Map<String, HttpHandler> loadApplications(DispatcherConfig<String> dispatcherConfig) throws ClassNotFoundException, RuntimeException {
        try {
            this.dispatcherConfig = dispatcherConfig;
            final Map<String, List<Class<HttpHandler>>> controllerClasses = this.findControllerClasses();

            for (Map.Entry<String, List<Class<HttpHandler>>> entry : controllerClasses.entrySet()) {
                final String applicationName = entry.getKey();
                this.makeAppAssetDir(PathUtil.appendPath(this.assetsDir, applicationName));
                for (Class<HttpHandler> controllerClass : entry.getValue()) {
                    this.loadSolet(controllerClass, applicationName);
                }
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
        return this.storageControllers;
    }

    /**
     * @return
     * @throws ClassNotFoundException
     */
    private Map<String, List<Class<HttpHandler>>> findControllerClasses() throws ClassNotFoundException {
        File file = new File((String) this.configCenter.getConfigValue(ConfigValue.JAVACHE_WORKING_DIRECTORY));
        String packageName = "";
        this.loadClass(file, packageName);
        return this.controllerClasses;
    }

    /**
     * Recursive method for loading classes, starts with empty packageName.
     * If the file is directory, iterate all files inside and call loadClass with the current file name
     * appended to the packageName.
     * <p>
     * If the file is file and the file name ends with .class, load it and check if the class
     * is assignable from {@link HttpHandler}. If it is, add it to the map of solet classes.
     */
    @SuppressWarnings("unchecked")
    private void loadClass(File currentFile, String packageName) throws ClassNotFoundException {
        if (currentFile.isDirectory()) {
            //If the folder is the root dir, do not append package name since the name is outside the java packages.
            boolean appendPackage = !this.isRootDir;
            //Since the root dir is reached only once, set it to false.
            this.isRootDir = false;
            for (File childFile : Objects.requireNonNull(currentFile.listFiles())) {
                if (appendPackage) {
                    this.loadClass(childFile, (packageName + currentFile.getName() + "."));
                } else {
                    this.loadClass(childFile, (packageName));
                }
            }
        } else {
            if (!currentFile.getName().endsWith(".class")) {
                return;
            }
            final String className = packageName + currentFile.getName().replace(".class", "").replace("/", ".");
            final Class<?> currentClassFile = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            if (BaseHttp.class.isAssignableFrom(currentClassFile)) {
                this.controllerClasses.get(this.rootAppName).add(((Class<HttpHandler>) currentClassFile));
            }
        }
    }


    /**
     * @return
     */
    public List<String> getApplicationNames() {
        return Collections.singletonList(this.rootAppName);
    }

    private boolean runSolet(HttpHandler solet, HttpRequest request, HttpResponse response) {
        try {
            solet.service(request, response);
            return solet.hasIntercepted();
        } catch (Exception ex) {
//            this.loggingService.printStackTrace(ex);
        }
        return true;
    }


    /**
     *
     * @param request
     * @return
     */
    public HttpHandler findSoletCandidate(HttpRequest request) {
        request.setContextPath(this.resolveCurrentRequestAppName(request));
        final String requestUrl = request.getRequestURL();
        final Pattern applicationRouteMatchPattern = Pattern.compile(Pattern.quote(request.getContextPath() + "\\/[a-zA-Z0-9]+\\/"));
        final Matcher applicationRouteMatcher = applicationRouteMatchPattern.matcher(requestUrl);

        if (this.storageControllers.containsKey(requestUrl)) {
            return this.storageControllers.get(requestUrl);
        } else {
            if (applicationRouteMatcher.find()) {
                String applicationRoute = applicationRouteMatcher.group(0) + "*";
                if (this.storageControllers.containsKey(applicationRoute)) {
                    return this.storageControllers.get(applicationRoute);
                }
            }
        }
        if (this.storageControllers.containsKey(request.getContextPath() + "/*")) {
            return this.storageControllers.get(request.getContextPath() + "/*");
        }
        return null;
    }

    private String resolveCurrentRequestAppName(HttpRequest request) {
//        for (String applicationName : this.applicationNames) {
//            if (request.getRequestURL().startsWith(applicationName) && !applicationName.equals(this.rootAppName)) {
//                return applicationName;
//            }
//        }
//
//        return "";
        Iterator<String> iterator = this.applicationNames.iterator();
        String applicationName;
        do {
            if (!iterator.hasNext()) {
                return "";
            }
            applicationName = (String)iterator.next();
        } while(!request.getRequestURL().startsWith(applicationName) || applicationName.equals(this.rootAppName));
        return applicationName;
    }

    @Override
    public int order() {
        return this.configCenter.getConfigValue(ConfigValue.DISPATCHER_ORDER.name(), int.class);
    }

    /**
     * Creates an instance of the solet.
     * If the application name is different than the javache specified main jar name (ROOT.jar by default),
     * add the appName to the route.
     * Put the solet in a solet map with a key being the soletRoute.
     */
    private void loadSolet(Class<HttpHandler> soletClass, String applicationName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final HttpHandler controllerInstance = soletClass.getDeclaredConstructor().newInstance();
        final Controller controllerAnnotation = this.getSoletAnnotation(controllerInstance.getClass());
        if (controllerAnnotation == null) {
            throw new IllegalArgumentException(String.format(MISSING_SOLET_ANNOTATION_FORMAT, soletClass.getName()));
        } else {
            String soletRoute = controllerAnnotation.value();
            if (!applicationName.equals(this.rootAppName)) {
                soletRoute = "/" + applicationName + soletRoute;
            }
            final SoletConfig soletConfigCopy = this.copySoletConfig();
            soletConfigCopy.setAttribute(CONFIG_ASSETS_DIR, PathUtil.appendPath(this.assetsDir, applicationName));
            soletConfigCopy.setAttribute(CFG_WORKING_DIR, this.getSoletWorkingDir(applicationName));
//        soletConfigCopy.setAttribute(CONFIG_LOGGER, new SoletLoggerImpl(
//                this.loggingService,
//                applicationName
//        ));
            if (!applicationName.equals("") && !applicationName.equals(this.rootAppName)) {
                soletConfigCopy.setAttribute(CONFIG_APP_NAME_PREFIX, "/" + applicationName);
            }
            if (!controllerInstance.isInitialized()) {
                controllerInstance.init(soletConfigCopy);
            }
            this.storageControllers.put(soletRoute, controllerInstance);
        }
    }

    private String getSoletWorkingDir(String appName) {
        final String appDir = PathUtil.appendPath(this.webappsDir, appName);
        final String appWorkingDir = PathUtil.appendPath(
                appDir,
                this.configCenter.getConfigValue(ConfigValue.APP_COMPILE_OUTPUT_DIR_NAME)
        );

        return PathUtil.appendPath(appWorkingDir, File.separator);
    }

    /**
     * Create SoletConfig instance and add objects.
     * This Solet Config will be used for initializing every solet.
     */
    private SoletConfig copySoletConfig() {
        final SoletConfig soletConfig = new SoletConfigImpl();
        for (Map.Entry<String, Object> entry : this.dispatcherConfig.getAllAttributes().entrySet()) {
            soletConfig.setAttribute(entry.getKey(), entry.getValue());
        }
        return soletConfig;
    }

    /**
     * Recursive method for getting {@link Controller} annotation from a given class.
     * Recursion is required since only parent class could have {@link Controller} annotation
     * and not the child.
     */
    private Controller getSoletAnnotation(Class<?> soletClass) {
        final Controller controller = soletClass.getAnnotation(Controller.class);
        if (controller == null && soletClass.getSuperclass() != null) {
            return this.getSoletAnnotation(soletClass.getSuperclass());
        }
        return controller;
    }

    /**
     * Creates asset directory for the current app in javache's assets directory.
     */
    private void makeAppAssetDir(String dir) {
        final File file = new File(dir);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    private String getAssetsDir() {
        return PathUtil.appendPath(
                this.configCenter.getConfigValue(ConfigValue.JAVACHE_WORKING_DIRECTORY),
                this.configCenter.getConfigValue(ConfigValue.ASSETS_DIR_NAME)
        );
    }

    private String getWebappsDir() {
        return PathUtil.appendPath(
                this.configCenter.getConfigValue(ConfigValue.JAVACHE_WORKING_DIRECTORY),
                this.configCenter.getConfigValue(ConfigValue.WEB_APPS_DIR_NAME)
        );
    }


    /**
     * Checks if there is folder that matches the folder name in the config file (lib by default)
     * Iterates all elements and adds the .jar files to the system's classpath.
     */
    private void loadLibraries() {
        String workingDir = this.configCenter.getConfigValue(ConfigValue.JAVACHE_WORKING_DIRECTORY);
        if (!workingDir.endsWith("/") && !workingDir.endsWith("\\")) {
            workingDir += "/";
        }

        final File libFolder = new File(workingDir + this.configCenter
                .getConfigValue(ConfigValue.APPLICATION_DEPENDENCIES_FOLDER_NAME));

        if (!libFolder.exists()) {
            return;
        }

        for (File file : libFolder.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                //TODO: add libs
//                try {
//                    ReflectionUtils.addJarFileToClassPath(file.getCanonicalPath());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        }
    }

    /**
     * Create SoletConfig instance and add objects.
     * This Solet Config will be used for initializing every solet.
     */
    private DispatcherConfig<String> createDispatcherConfig() {
        final DispatcherConfig<String> soletConfig = new DispatcherConfig<>();
        soletConfig.setAttribute(Dispatcher.CONFIG_SESSION_STORAGE_KEY, this.sessionManagement.getSessionStorage());
        soletConfig.setAttribute(Dispatcher.CONFIG_SERVER_CONFIG_SERVICE_KEY, this.configCenter);
        soletConfig.setAttribute(Dispatcher.CONFIG_DEPENDENCY_CONTAINER_KEY, IocCenter.getRequestHandlersDependencyContainer());
        return soletConfig;
    }

    /**
     * @param <T>
     */
    public static class DispatcherConfig<T> {
        private final Map<T, Object> attributes;

        public DispatcherConfig() {
            this.attributes = new HashMap<>();
        }

        public void setAttribute(T name, Object attribute) {
            this.attributes.put(name, attribute);
        }

        public void setIfMissing(T name, Object attribute) {
            this.attributes.putIfAbsent(name, attribute);
        }

        public void deleteAttribute(T name) {
            this.attributes.remove(name);
        }

        public boolean hasAttribute(T name) {
            return this.attributes.containsKey(name);
        }

        public Object getAttribute(T name) {
            return this.attributes.get(name);
        }

        public Map<T, Object> getAllAttributes() {
            return Collections.unmodifiableMap(this.attributes);
        }
    }
}
