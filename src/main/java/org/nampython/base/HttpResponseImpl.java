package org.nampython.base;

import org.nampython.base.api.BaseHttpResponse;
import org.nampython.base.api.HttpCookie;
import org.nampython.base.api.HttpStatus;

import java.util.Map;

public class HttpResponseImpl implements HttpResponse {

    private final BaseHttpResponse response;


    public HttpResponseImpl(BaseHttpResponse response) {
        this.response = response;
    }

    @Override
    public void sendRedirect(String location) {
        this.response.setStatusCode(HttpStatus.SEE_OTHER);
        this.response.setContent(location);
        this.response.addHeader("Location", location);
    }

    @Override
    public void setStatusCode(HttpStatus statusCode) {
        this.response.setStatusCode(statusCode);
    }

    @Override
    public void setContent(String content) {
        this.response.setContent(content);
    }

    @Override
    public void setContent(byte[] content) {
        this.response.setContent(content);
    }

    @Override
    public void addHeader(String header, String value) {
        this.response.addHeader(header, value);
    }

    @Override
    public void addCookie(String name, String value) {
        this.response.addCookie(name, value);
    }

    @Override
    public void addCookie(HttpCookie cookie) {
        this.response.addCookie(cookie);
    }

    @Override
    public String getResponse() {
        return this.response.getResponse();
    }

    @Override
    public HttpStatus getStatusCode() {
        return this.response.getStatusCode();
    }

    @Override
    public byte[] getContent() {
        return this.response.getContent();
    }

    @Override
    public byte[] getBytes() {
        return this.response.getBytes();
    }

    @Override
    public Map<String, String> getHeaders() {
        return this.response.getHeaders();
    }
}
