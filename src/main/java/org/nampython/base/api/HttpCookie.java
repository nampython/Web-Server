package org.nampython.base.api;

public interface HttpCookie {

    void setName(String name);

    void setValue(String value);

    void setPath(String path);

    String getName();

    String getValue();

    String getPath();

    String toRFCString();
}
