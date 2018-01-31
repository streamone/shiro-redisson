package com.github.streamone.shiro.session;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.*;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.apache.shiro.session.mgt.AbstractSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * <p>RedissonSessionManager test case.</p>
 *
 * @author streamone
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/sessionContext.xml")
public class RedissonSessionManagerTest {

    @Resource(name = "sessionManager")
    private RedissonSessionManager sessionManager;

    @Test
    public void testCreateSession() {
        final String host = "localhost";
        SessionFactory noStartTimeFactory = new SessionFactory() {
            @Override
            public Session createSession(SessionContext initData) {
                SimpleSession session;
                if (initData != null) {
                    String host = initData.getHost();
                    if (host != null) {
                        session = new SimpleSession(host);
                        session.setStartTimestamp(null);
                        return session;
                    }
                }
                session = new SimpleSession();
                session.setStartTimestamp(null);
                return session;
            }
        };
        RedissonSessionManager cloneSessionManager = spy(this.sessionManager);
        cloneSessionManager.setSessionFactory(noStartTimeFactory);
        SessionContext sc = new DefaultSessionContext();
        sc.setHost(host);
        Session session = cloneSessionManager.start(sc);
        assertEquals(host, session.getHost());
        assertNotNull(session.getStartTimestamp());

        Session newSession = this.sessionManager.start(new DefaultSessionContext());
        assertEquals("", newSession.getHost());
        assertNotNull(newSession);
        assertEquals(DEFAULT_GLOBAL_SESSION_TIMEOUT, newSession.getTimeout());
        assertNotNull(newSession.getStartTimestamp());

        Session retrievedSession = this.sessionManager.getSession(new DefaultSessionKey(newSession.getId()));
        assertNotNull(retrievedSession);
        assertEquals(newSession.getStartTimestamp(), retrievedSession.getStartTimestamp());
    }

    @Test(expected = UnknownSessionException.class)
    public void testGetSessionByInvalidId() {
        String invalidId = "i_am_not_a_valid_session";
        this.sessionManager.getSession(new DefaultSessionKey(invalidId));
    }

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
