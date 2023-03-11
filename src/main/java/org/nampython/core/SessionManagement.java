package org.nampython.core;

import com.cyecize.ioc.annotations.Service;
import org.nampython.base.*;
import org.nampython.base.api.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class SessionManagement {
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";
    private static final String SESSION_COOKIE_NAME = "JAVACHE_SESSION_ID";
    private final HttpSessionStorage sessionStorage;

    public SessionManagement() {
        this.sessionStorage = new HttpSessionStorageImpl();
    }

    /**
     * Gets looks for a cookie representing the sessionId.
     * If the cookie is not present, adds a new session to the HttpRequest.
     * If the cookie is present, checks if a session with that id exists.
     * If no session exists or the session is not valid,
     * removes the cookie and a new session to the HttpRequest.
     * if the session is valid, sets the session to the HttpRequest.
     */
    public void initSessionIfExistent(HttpRequest request) {
        final HttpCookie cookie = request.getCookies().get(this.getSessionCookieName(request));

        if (cookie != null) {
            final HttpSession session = this.sessionStorage.getSession(cookie.getValue());
            if (this.isSessionValid(session)) {
                request.setSession(session);
            } else {
                request.getCookies().remove(SESSION_COOKIE_NAME);
                this.addNewSession(request);
            }
        } else {
            this.addNewSession(request);
        }
    }

    /**
     * If the session is new, adds it to the sessionStorage map.
     * If the session is valid, adds a cookie.
     * If the session is invalid, removes the cookie.
     */
    public void sendSessionIfExistent(HttpRequest request, HttpResponse response) {
        if (request.getSession() != null) {
            if (this.sessionStorage.getSession(request.getSession().getId()) == null) {
                this.sessionStorage.addSession(request.getSession());
            }
            if (request.getSession().isValid()) {
                HttpCookie cookie = new HttpCookieImpl(this.getSessionCookieName(request), request.getSession().getId());
                String expires = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now().plusDays(1L));
                cookie.setPath("/; expires=" + expires);
                response.addCookie(cookie);
            } else {
                Date var10002 = new Date(0L);
                response.addCookie(SESSION_COOKIE_NAME, "removed; expires=" + var10002.toString());
            }
        }
    }

    public void clearInvalidSessions() {
        this.sessionStorage.refreshSessions();
    }

    public HttpSessionStorage getSessionStorage() {
        return this.sessionStorage;
    }

    private void addNewSession(HttpRequest request) {
        request.setSession(new HttpSessionImpl());
    }

    private boolean isSessionValid(HttpSession session) {
        return session != null && session.isValid();
    }

    private String getSessionCookieName(HttpRequest request) {
        return SESSION_COOKIE_NAME + request.getContextPath();
    }
}
