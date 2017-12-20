package com.github.streamone.shiro.session;

import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.DefaultSessionContext;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.apache.shiro.session.mgt.AbstractSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT;
import static org.junit.Assert.*;

/**
 * <p>RedissonSession test case.</p>
 *
 * @author streamone
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/sessionContext.xml")
public class RedissonSessionTest {

    @Resource(name = "sessionManager")
    private SessionManager sessionManager;

    @Test
    public void testCreateSession() {
        Session newSession = sessionManager.start(new DefaultSessionContext());
        assertNotNull(newSession);
        assertEquals(DEFAULT_GLOBAL_SESSION_TIMEOUT, newSession.getTimeout());
        assertNotNull(newSession.getStartTimestamp());

        Session retrievedSession = sessionManager.getSession(new DefaultSessionKey(newSession.getId()));
        assertNotNull(retrievedSession);
        assertEquals(newSession.getStartTimestamp(), retrievedSession.getStartTimestamp());
    }

    @Test
    public void testModifySession() {
        Session newSession = sessionManager.start(new DefaultSessionContext());
        newSession.setTimeout(DEFAULT_GLOBAL_SESSION_TIMEOUT / 2);
        assertEquals(DEFAULT_GLOBAL_SESSION_TIMEOUT / 2,
            sessionManager.getSession(new DefaultSessionKey(newSession.getId())).getTimeout());

        Date lastAccessTime = newSession.getLastAccessTime();
        newSession.touch();
        assertTrue(sessionManager.getSession(new DefaultSessionKey(newSession.getId())).
            getLastAccessTime().after(lastAccessTime));
    }

    @Test
    public void testSessionAttribute() {
        Session newSession = sessionManager.start(new DefaultSessionContext());
        newSession.setAttribute("foo", "bar");
        assertEquals("bar", sessionManager.getSession(
            new DefaultSessionKey(newSession.getId())).getAttribute("foo"));
        assertNull(sessionManager.getSession(
            new DefaultSessionKey(newSession.getId())).getAttribute("notExistKey"));

        newSession.setAttribute("hash", "code");
        Collection<Object> keys = sessionManager.getSession(new DefaultSessionKey(
            newSession.getId())).getAttributeKeys();
        assertTrue(keys.size() == 2);
        assertTrue(keys.contains("foo"));
        assertTrue(keys.contains("hash"));

        sessionManager.getSession(new DefaultSessionKey(newSession.getId())).removeAttribute("foo");
        assertNull(newSession.getAttribute("foo"));
    }

    @Test(expected = InvalidSessionException.class)
    public void testSessionExpiring() throws InterruptedException {
        Session newSession = sessionManager.start(new DefaultSessionContext());
        newSession.setAttribute("foo", "bar");
        long timeout = 50;
        newSession.setTimeout(timeout);
        TimeUnit.MILLISECONDS.sleep(timeout);
        assertNull(sessionManager.getSession(new DefaultSessionKey(newSession.getId())));
        newSession.touch();
    }

    @Test(expected = UnknownSessionException.class)
    public void testStopSession() {
        Session newSession = sessionManager.start(new DefaultSessionContext());
        newSession.setAttribute("foo", "bar");
        newSession.stop();
        sessionManager.getSession(new DefaultSessionKey(newSession.getId()));
    }
}
