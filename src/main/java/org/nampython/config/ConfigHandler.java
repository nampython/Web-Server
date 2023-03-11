package org.nampython.config;

import org.nampython.support.PrimitiveTypeDataResolver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.nampython.config.ConstantsPool.WORKING_DIRECTORY;

public class ConfigHandler implements ConfigCenter {

    private static final String CONFIG_FOLDER_PATH = WORKING_DIRECTORY + "config/";

    private static final String REQUEST_HANDLER_PRIORITY_FILE = CONFIG_FOLDER_PATH + "request-handlers.ini";

    private static final String CONFIG_FILE_PATH = CONFIG_FOLDER_PATH + "config.ini";

    private static final String REQUEST_HANDLER_PRIORITY_FILE_NOT_FOUND_FORMAT =
            "Request Handler priority configuration file does not exist for \"%s\".";

    private List<String> requestHandlers;
    protected Map<String, Object> configParameters;
    private final PrimitiveTypeDataResolver dataResolver;

    public ConfigHandler() {
        this.dataResolver = new PrimitiveTypeDataResolver();
        this.init();
    }

    /**
     *
     */
    private void init() {
        try {
            this.loadRequestHandlerConfig();
            this.initDefaultConfigParams();
            this.initConfigParams();
            this.applyEnvironmentVariables();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     */
    private void initDefaultConfigParams() {
        this.configParameters = new HashMap<>();
        this.configParameters.put(ConfigValue.MAX_REQUEST_SIZE.name(), Integer.MAX_VALUE);
        this.configParameters.put(ConfigValue.SHOW_REQUEST_LOG.name(), false);
        this.configParameters.put(ConfigValue.ASSETS_DIR_NAME.name(), "assets/");
        this.configParameters.put(ConfigValue.WEB_APPS_DIR_NAME.name(), "webapps/");
        this.configParameters.put(ConfigValue.APP_COMPILE_OUTPUT_DIR_NAME.name(), "classes");
        this.configParameters.put(ConfigValue.APP_RESOURCES_DIR_NAME.name(), "webapp");
        this.configParameters.put(ConfigValue.MAIN_APP_JAR_NAME.name(), "ROOT");
        this.configParameters.put(ConfigValue.APPLICATION_DEPENDENCIES_FOLDER_NAME.name(), "lib");
        this.configParameters.put(ConfigValue.BROCOLLINA_SKIP_EXTRACTING_IF_FOLDER_EXISTS.name(), false);
        this.configParameters.put(ConfigValue.BROCCOLINA_FORCE_OVERWRITE_FILES.name(), true);
        this.configParameters.put(ConfigValue.BROCCOLINA_TRACK_RESOURCES.name(), true);
        this.configParameters.put(ConfigValue.SERVER_PORT.name(), ConstantsPool.EMPTY_PORT);
        this.configParameters.put(ConfigValue.SERVER_STARTUP_ARGS.name(), new String[0]);
        this.configParameters.put(ConfigValue.JAVACHE_WORKING_DIRECTORY.name(), ConstantsPool.WORKING_DIRECTORY);
        this.configParameters.put(ConfigValue.LIB_DIR_NAME.name(), "lib/");
        this.configParameters.put(ConfigValue.API_DIR_NAME.name(), "api/");
        this.configParameters.put(ConfigValue.LOGS_DIR_NAME.name(), "logs/");
        this.configParameters.put(ConfigValue.PRINT_EXCEPTIONS.name(), true);
        this.configParameters.put(ConfigValue.RESOURCE_HANDLER_ORDER.name(), 1);
        this.configParameters.put(ConfigValue.DISPATCHER_ORDER.name(), 2);
        this.configParameters.put(ConfigValue.ENABLE_RESOURCE_CACHING.name(), true);
        this.configParameters.put(ConfigValue.RESOURCE_CACHING_EXPRESSION.name(), ConstantsPool.DEFAULT_CACHING_EXPRESSION);

    }

    /**
     * Reads the config.ini file and filters those settings that are
     * available as default (set in initDefaultConfigParams)
     * Then gets the current param type and tries to convert the value to that type.
     */
    private void initConfigParams() throws IOException {
        final File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists() || !configFile.isFile()) {
            return;
        }

        final BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(CONFIG_FILE_PATH)));
        //TODO: refactor the parsing -> case when user has ':' in his value.
        while (bufferedReader.ready()) {
            final String line = bufferedReader.readLine();
            final String[] keyValuePair = line.trim().split(":\\s+");

            if (keyValuePair.length != 2) {
                continue;
            }

            keyValuePair[0] = keyValuePair[0].toUpperCase();
            if (!this.configParameters.containsKey(keyValuePair[0])) {
                continue;
            }

            this.configParameters.put(keyValuePair[0], this.dataResolver.resolve(
                    this.configParameters.get(keyValuePair[0]).getClass(), keyValuePair[1]
            ));
        }
    }
    /**
     * As a last step, Javache will check for any environment variables with names that match {@link JavacheConfigValue}
     * Since environment variables are applied last, they have the highest priority.
     */
    private void applyEnvironmentVariables() {
        for (ConfigValue cfg : ConfigValue.values()) {
            final String val = System.getenv(cfg.name());
            if (val == null) {
                continue;
            }

            this.configParameters.put(
                    cfg.name(),
                    this.dataResolver.resolve(
                            this.configParameters.get(cfg.name()).getClass(),
                            val
                    )
            );
        }
    }

    /**
     * Looks for file that contains request handler names and priority.
     */
    protected void loadRequestHandlerConfig() throws IOException {
        final File priorityConfigFile = new File(REQUEST_HANDLER_PRIORITY_FILE);

        if (!priorityConfigFile.exists() || !priorityConfigFile.isFile()) {
            throw new IllegalArgumentException(
                    String.format(REQUEST_HANDLER_PRIORITY_FILE_NOT_FOUND_FORMAT, CONFIG_FOLDER_PATH)
            );
        }

        final String configFileContent = new Reader().readAllLines(new FileInputStream(priorityConfigFile));

        this.requestHandlers = Arrays.stream(configFileContent.split(",\\s+"))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * @param configKey
     * @param type
     * @param <T>
     * @return
     */
    @Override
    public <T> T getConfigValue(Enum<? extends ConfigValue> configKey, Class<T> type) {
        return this.getConfigValue(configKey.name(), type);
    }

    @SuppressWarnings("unchecked")

    @Override
    public <T> T getConfigValue(Enum<? extends ConfigValue> paramName) {
        return (T) this.configParameters.get(paramName.name());
    }


    /**
     * @param paramName
     * @param type
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getConfigValue(String paramName, Class<T> type) {
        return (T) this.configParameters.get(paramName);
    }

    /**
     * @param configKey
     * @param configValue
     */
    @Override
    public void addConfigParam(Enum<? extends ConfigValue> configKey, Object configValue) {
        this.addConfigParam(configKey.name(), configValue);
    }

    /**
     * @param name
     * @param value
     */
    @Override
    public void addConfigParam(String name, Object value) {
        this.configParameters.put(name, value);
    }

    @Override
    public String getConfigParamString(Enum<? extends ConfigValue> paramName) {
        return this.getConfigParamString(paramName.name());
    }

    @Override
    public String getConfigParamString(String paramName) {
        final Object configParam = this.getConfigValue(paramName, Object.class);
        if (configParam != null) {
            return configParam.toString();
        }
        return null;
    }

    final class Reader {

        public Reader() {

        }

        public String readAllLines(InputStream inputStream) throws IOException {
            return new String(this.readAllBytes(inputStream), StandardCharsets.UTF_8);
        }

        public byte[] readAllBytes(InputStream inputStream) throws IOException {
            return inputStream.readAllBytes();
        }
    }
}
