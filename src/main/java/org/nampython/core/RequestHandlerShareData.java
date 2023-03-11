package org.nampython.core;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class RequestHandlerShareData {
    public static final String HTTP_REQUEST = "HTTP_REQUEST";
    public static final String HTTP_RESPONSE = "HTTP_RESPONSE";
    private final Map<String, Object> storage;
    public RequestHandlerShareData() {
        this.storage = new HashMap<>();
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getStorage() {
        return storage;
    }

    /**
     *
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getObject(String key) {
        return (T) this.storage.get(key);
    }

    /**
     *
     * @param key
     * @param parameterType
     * @return
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, Class<T> parameterType) {
        return (T) this.storage.get(key);
    }


    /**
     *
     * @param key
     * @param o
     * @return
     */
    public boolean addObject(String key, Object o) {
        if (this.storage.containsKey(key)) {
            return false;
        }
        this.storage.put(key, o);
        return true;
    }
}
