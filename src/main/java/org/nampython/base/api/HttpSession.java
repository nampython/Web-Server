package org.nampython.base.api;

import java.util.Map;

public interface HttpSession {
    void invalidate();
    void addAttribute(String name, Object attribute);
    Object getAttribute(String key);
    boolean isValid();
    String getId();
    Map<String, Object> getAttributes();
}
