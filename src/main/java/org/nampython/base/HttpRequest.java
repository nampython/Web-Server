package org.nampython.base;


import org.nampython.base.api.BaseHttpRequest;

public interface HttpRequest extends BaseHttpRequest {

    void setContextPath(String contextPath);

    String getContextPath();

    String getRelativeRequestURL();
}
