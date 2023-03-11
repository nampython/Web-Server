package org.nampython.config;

import java.util.Map;

public class AdditionalConfig extends ConfigHandler{
    public AdditionalConfig(Map<String, Object> configs) {
        super();
        super.configParameters.putAll(configs);
    }
    /**
     * transfer runtime config map to global map.
     */
    private void setConfig(Map<String, Object> config) {
        super.configParameters.putAll(config);
    }


    protected void loadRequestHandlerConfig() {
    }
}
