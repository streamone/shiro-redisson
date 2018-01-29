package com.github.streamone.shiro.session;

import org.apache.shiro.session.*;
import org.apache.shiro.session.mgt.DefaultSessionContext;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SimpleSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.Redisson;
import org.redisson.RedissonScript;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.streamone.shiro.session.RedissonSessionScript.RETURN_CODE_EXPIRED;
import static org.apache.shiro.session.mgt.AbstractSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
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
        new RedissonSession(null,null, null, null, (Serializable) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSessionByIllegalArguments2() {
        new RedissonSession(null,null, null, null, (Session) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSessionByIllegalArguments3() {
        Session session = new SimpleSession();
        new RedissonSession(mock(RedissonClient.class), null, "", "", session);
    }

    @Test(expected = ExpiredSessionException.class)
    public void testCreateSessionByInvalidState() {
        RedissonScript mockedScript = mock(RedissonScript.class);
        when(mockedScript.eval(anyString(), any(RScript.Mode.class), any(Codec.class), anyString(),
            any(RScript.ReturnType.class), anyList(), any())).thenThrow(new RedisException("-1"));
        RedissonClient mockedRedisson = mock(RedissonClient.class);
        when(mockedRedisson.getScript()).thenReturn(mockedScript);

        RedissonSession newSession = new RedissonSession(mockedRedisson, null, "", "", UUID.randomUUID());
        newSession.getAttributeKeys();
    }

    @Test(expected = StoppedSessionException.class)
    public void testCreateSessionByInvalidState2() {
        RedissonScript mockedScript = mock(RedissonScript.class);
        when(mockedScript.eval(anyString(), any(RScript.Mode.class), any(Codec.class), anyString(),
                any(RScript.ReturnType.class), anyList(), any())).thenThrow(new RedisException("-2"));
        RedissonClient mockedRedisson = mock(RedissonClient.class);
        when(mockedRedisson.getScript()).thenReturn(mockedScript);

        RedissonSession newSession = new RedissonSession(mockedRedisson, null, "", "", UUID.randomUUID());
        newSession.getAttributeKeys();
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
