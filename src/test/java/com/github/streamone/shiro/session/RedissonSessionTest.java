package com.github.streamone.shiro.session;

import org.apache.shiro.session.*;
import org.apache.shiro.session.mgt.DefaultSessionContext;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SimpleSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RMap;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.apache.shiro.session.mgt.AbstractSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>RedissonSession test case.</p>
 *
 * @author streamone
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/sessionContext.xml")
public class RedissonSessionTest {

    @Resource(name = "sessionManager")
    private RedissonSessionManager sessionManager;

    @Resource(name = "webSessionManager")
    private RedissonWebSessionManager webSessionManager;

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSessionByIllegalArguments() {
        new RedissonSession(null, null, (Serializable) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSessionByIllegalArguments2() {
        new RedissonSession(null, null, (Session) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSessionByIllegalArguments3() {
        Session session = new SimpleSession();
        new RedissonSession(mock(RMap.class), mock(RMap.class), session);
    }

    @Test(expected = ExpiredSessionException.class)
    public void testCreateSessionByInvalidState() {
        RMap<String, Object> notExistInfoMap = mock(RMap.class);
        when(notExistInfoMap.isExists()).thenReturn(false);
        new RedissonSession(notExistInfoMap, mock(RMap.class), UUID.randomUUID());
    }

    @Test(expected = StoppedSessionException.class)
    public void testCreateSessionByInvalidState2() {
        RMap<String, Object> noKeyInfoMap = mock(RMap.class);
        when(noKeyInfoMap.isExists()).thenReturn(true);
        when(noKeyInfoMap.containsKey(RedissonSession.INFO_STOP_KEY)).thenReturn(true);
        new RedissonSession(noKeyInfoMap, mock(RMap.class), UUID.randomUUID());
    }

    @Test
    public void testModifySession() {
        Session newSession = this.sessionManager.start(new DefaultSessionContext());
        newSession.setTimeout(DEFAULT_GLOBAL_SESSION_TIMEOUT / 2);
        assertEquals(DEFAULT_GLOBAL_SESSION_TIMEOUT / 2,
            this.sessionManager.getSession(new DefaultSessionKey(newSession.getId())).getTimeout());

        Date lastAccessTime = newSession.getLastAccessTime();
        newSession.touch();
        assertTrue(this.sessionManager.getSession(new DefaultSessionKey(newSession.getId())).
            getLastAccessTime().after(lastAccessTime));
    }

    @Test
    public void testSessionAttribute() {
        Session newSession = this.sessionManager.start(new DefaultSessionContext());
        newSession.setAttribute("foo", "bar");
        assertEquals("bar", this.sessionManager.getSession(
            new DefaultSessionKey(newSession.getId())).getAttribute("foo"));
        assertNull(this.sessionManager.getSession(
            new DefaultSessionKey(newSession.getId())).getAttribute("notExistKey"));

        newSession.setAttribute("hash", "code");
        Collection<Object> keys = this.sessionManager.getSession(new DefaultSessionKey(
            newSession.getId())).getAttributeKeys();
        assertTrue(keys.size() == 2);
        assertTrue(keys.contains("foo"));
        assertTrue(keys.contains("hash"));

        this.sessionManager.getSession(new DefaultSessionKey(newSession.getId())).removeAttribute("foo");
        assertNull(newSession.getAttribute("foo"));

        this.sessionManager.getSession(new DefaultSessionKey(newSession.getId())).removeAttribute("hash");
        assertNull(newSession.getAttribute("hash"));

        keys = this.sessionManager.getSession(new DefaultSessionKey(
                newSession.getId())).getAttributeKeys();
        assertTrue(keys.size() == 0);
    }

    @Test(expected = InvalidSessionException.class)
    public void testSessionExpiring() throws InterruptedException {
        Session newSession = this.sessionManager.start(new DefaultSessionContext());
        newSession.setAttribute("foo", "bar");
        long timeout = 50;
        newSession.setTimeout(timeout);
        TimeUnit.MILLISECONDS.sleep(timeout);
        this.sessionManager.getSession(new DefaultSessionKey(newSession.getId()));
    }

    @Test(expected = UnknownSessionException.class)
    public void testStopSession() {
        Session newSession = this.sessionManager.start(new DefaultSessionContext());
        newSession.setAttribute("foo", "bar");
        newSession.stop();
        this.sessionManager.getSession(new DefaultSessionKey(newSession.getId()));
    }

}
