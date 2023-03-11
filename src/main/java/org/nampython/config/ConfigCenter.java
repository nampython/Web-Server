package org.nampython.config;

public interface ConfigCenter {
    <T> T getConfigValue(Enum<? extends ConfigValue> configKey, Class<T> type);
    <T> T getConfigValue(Enum<? extends ConfigValue> paramName);
    <T> T getConfigValue(String paramName, Class<T> type);
    void addConfigParam(Enum<? extends ConfigValue> configKey, Object ConfigValue);
    void addConfigParam(String name, Object value);
    String getConfigParamString(Enum<? extends ConfigValue>  paramName);
    String getConfigParamString(String paramName);

}
