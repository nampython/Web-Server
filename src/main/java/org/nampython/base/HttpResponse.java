package org.nampython.base;


import org.nampython.base.api.BaseHttpResponse;

public interface HttpResponse extends BaseHttpResponse {
    void sendRedirect(String location);

}
