package org.nampython.base.api;

import java.util.Map;

public interface HttpSessionStorage {

    void addSession(HttpSession session);

    void refreshSessions();

    HttpSession getSession(String sessionId);

    Map<String, HttpSession> getAllSessions();
}
