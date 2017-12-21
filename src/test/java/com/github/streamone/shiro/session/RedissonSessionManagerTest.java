package com.github.streamone.shiro.session;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.*;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>RedissonSessionManager test case.</p>
 *
 * @author streamone
 */
public class RedissonSessionManagerTest {

    @Test
    public void testBeanSetter() {
        RedissonSessionManager sessionManager = new RedissonSessionManager();
        RedissonSessionDao sessionDao = mock(RedissonSessionDao.class);
        sessionManager.setSessionDAO(sessionDao);
        assertEquals(sessionDao, sessionManager.getSessionDAO());

        SessionFactory factory = sessionManager.getSessionFactory();
        assertTrue(factory != null && factory instanceof SimpleSessionFactory);

        final String host = "anonymous.host";
        sessionManager.setSessionFactory(new SessionFactory() {
            @Override
            public Session createSession(SessionContext initData) {
                Session session = mock(Session.class);
                when(session.getHost()).thenReturn(host);
                return session;
            }
        });
        Session session = sessionManager.createSession(new DefaultSessionContext());
        assertEquals(host, session.getHost());
    }

    @Test
    public void testDoGetSessionWithNullId() {
        RedissonSessionManager sessionManager = new RedissonSessionManager();
        RedissonSessionDao sessionDao = mock(RedissonSessionDao.class);
        sessionManager.setSessionDAO(sessionDao);
        SessionKey key = new DefaultSessionKey();
        assertNull(sessionManager.doGetSession(key));
    }

    @Test(expected = UnknownSessionException.class)
    public void testDoGetSessionWithNotExistId() {
        RedissonSessionManager sessionManager = new RedissonSessionManager();
        RedissonSessionDao sessionDao = mock(RedissonSessionDao.class);
        sessionManager.setSessionDAO(sessionDao);
        when(sessionDao.readSession(anyString())).thenReturn(null);
        SessionKey key = new DefaultSessionKey(new JavaUuidSessionIdGenerator().generateId(null));
        sessionManager.doGetSession(key);
    }

}
