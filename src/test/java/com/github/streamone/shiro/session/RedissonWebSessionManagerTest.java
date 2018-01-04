package com.github.streamone.shiro.session;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.DefaultSessionContext;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.servlet.ShiroHttpSession;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionContext;
import org.apache.shiro.web.session.mgt.WebSessionContext;
import org.apache.shiro.web.session.mgt.WebSessionKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.shiro.session.mgt.AbstractSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * <p>RedissonWebSessionManager test case.</p>
 *
 * @author streamone
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/sessionContext.xml")
public class RedissonWebSessionManagerTest {

    @Resource(name = "webSessionManager")
    private RedissonWebSessionManager webSessionManager;

    @Test
    public void testSessionIdUrlRewritingEnabled() {
        RedissonWebSessionManager cloneSessionManager = spy(this.webSessionManager);
        assertEquals(true, cloneSessionManager.isSessionIdUrlRewritingEnabled());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebSessionKey sessionKey = new WebSessionKey(request, response);
        cloneSessionManager.getSession(sessionKey);
        assertTrue((Boolean) request.getAttribute(ShiroHttpServletRequest.SESSION_ID_URL_REWRITING_ENABLED));

        cloneSessionManager.setSessionIdUrlRewritingEnabled(false);
        assertFalse(cloneSessionManager.isSessionIdUrlRewritingEnabled());

        cloneSessionManager.getSession(sessionKey);
        assertFalse((Boolean) request.getAttribute(ShiroHttpServletRequest.SESSION_ID_URL_REWRITING_ENABLED));
    }

    @Test
    public void testCreateWebSession() {
        assertFalse(this.webSessionManager.isServletContainerSessions());

        WebSessionContext sc = new DefaultWebSessionContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        sc.setServletRequest(request);
        sc.setServletResponse(response);
        Session newSession = this.webSessionManager.start(sc);
        assertNotNull(newSession);
        assertEquals(DEFAULT_GLOBAL_SESSION_TIMEOUT, newSession.getTimeout());
        assertNotNull(newSession.getStartTimestamp());

        String sessionId = null;
        String cookieHeader = response.getHeader("Set-Cookie");
        Pattern pattern = Pattern.compile("JSESSIONID=([^;]*);.*");
        Matcher matcher = pattern.matcher(cookieHeader);
        if (matcher.matches()) {
            sessionId = matcher.group(1);
        }
        assertNotNull(sessionId);

        MockHttpServletRequest newRequest = new MockHttpServletRequest();
        MockHttpServletResponse newResponse = new MockHttpServletResponse();
        newRequest.setCookies(new Cookie("JSESSIONID", sessionId));
        Session retrievedSession = this.webSessionManager.getSession(
                new WebSessionKey(newRequest, newResponse));
        assertNotNull(retrievedSession);
        assertEquals(newSession.getStartTimestamp(), retrievedSession.getStartTimestamp());
    }

    @Test
    public void testCreateWebSessionByCookieDisabled() {
        WebSessionContext sc = new DefaultWebSessionContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        sc.setServletRequest(request);
        sc.setServletResponse(response);
        RedissonWebSessionManager cloneSessionManager = spy(this.webSessionManager);
        cloneSessionManager.setSessionIdCookieEnabled(false);
        Session newSession = cloneSessionManager.start(sc);
        assertNotNull(newSession);
        assertEquals(DEFAULT_GLOBAL_SESSION_TIMEOUT, newSession.getTimeout());
        assertNotNull(newSession.getStartTimestamp());
        assertTrue((Boolean) request.getAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_IS_NEW));
        String cookieHeader = response.getHeader("Set-Cookie");
        assertNull(cookieHeader);

        MockHttpServletRequest newRequest = new MockHttpServletRequest();
        MockHttpServletResponse newResponse = new MockHttpServletResponse();
        newRequest.setCookies(new Cookie("JSESSIONID", newSession.getId().toString()));
        Session retrievedSession = cloneSessionManager.getSession(
                new WebSessionKey(newRequest, newResponse));
        assertNull(retrievedSession);
    }

    @Test
    public void testCreateSession() {
        SessionContext sc = new DefaultSessionContext();
        Session newSession = this.webSessionManager.start(sc);
        assertNotNull(newSession);
        assertNotNull(newSession.getId());
        assertEquals(DEFAULT_GLOBAL_SESSION_TIMEOUT, newSession.getTimeout());
        assertNotNull(newSession.getStartTimestamp());

        Session retrievedSession = this.webSessionManager.getSession(new DefaultSessionKey(newSession.getId()));
        assertNotNull(retrievedSession);
        assertEquals(newSession.getStartTimestamp(), retrievedSession.getStartTimestamp());
    }

    @Test
    public void testCustomerizedSessionIdCookie() {
        WebSessionContext sc = new DefaultWebSessionContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        sc.setServletRequest(request);
        sc.setServletResponse(response);
        RedissonWebSessionManager cloneSessionManager = spy(this.webSessionManager);
        assertEquals(ShiroHttpSession.DEFAULT_SESSION_ID_NAME, cloneSessionManager.getSessionIdCookie().getName());
        String customerizedSessionIdName = "mySessionId";
        cloneSessionManager.setSessionIdCookie(new SimpleCookie(customerizedSessionIdName));
        Session newSession = cloneSessionManager.start(sc);
        String sessionId = null;
        String cookieHeader = response.getHeader("Set-Cookie");
        Pattern pattern = Pattern.compile(customerizedSessionIdName + "=([^;]*);.*");
        Matcher matcher = pattern.matcher(cookieHeader);
        if (matcher.matches()) {
            sessionId = matcher.group(1);
        }
        assertNotNull(sessionId);
        assertEquals(newSession.getId(), sessionId);
    }

    @Test(expected = UnknownSessionException.class)
    public void testStopWebSession() {
        WebSessionContext sc = new DefaultWebSessionContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        sc.setServletRequest(request);
        sc.setServletResponse(response);
        Session newSession = this.webSessionManager.start(sc);
        assertNotNull(newSession);

        newSession.stop();

        MockHttpServletRequest newRequest = new MockHttpServletRequest();
        MockHttpServletResponse newResponse = new MockHttpServletResponse();
        newRequest.setCookies(new Cookie("JSESSIONID", newSession.getId().toString()));
        Session retrievedSession = this.webSessionManager.getSession(
                new WebSessionKey(newRequest, newResponse));
    }

    @Test(expected = UnknownSessionException.class)
    public void testStopSession() {
        SessionContext sc = new DefaultSessionContext();
        Session newSession = this.webSessionManager.start(sc);
        assertNotNull(newSession);

        newSession.stop();

        Session retrievedSession = this.webSessionManager.getSession(new DefaultSessionKey(newSession.getId()));
    }

    @Test
    public void testGetSessionByRequestURI() {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        WebSessionKey notHttpSessionKey = new WebSessionKey(request, response);
        assertNull(this.webSessionManager.getSession(notHttpSessionKey));

        MockHttpServletRequest noUriHttpRequest = new MockHttpServletRequest();
        noUriHttpRequest.setRequestURI(null);
        MockHttpServletResponse noUriHttpResponse = new MockHttpServletResponse();
        WebSessionKey noRequestUriSessionKey = new WebSessionKey(noUriHttpRequest, noUriHttpResponse);
        assertNull(this.webSessionManager.getSession(noRequestUriSessionKey));

        MockHttpServletRequest invalidUriSessionIdHttpRequest1 = new MockHttpServletRequest();
        invalidUriSessionIdHttpRequest1.setRequestURI("http://server.com/any.do?foo=bar");
        MockHttpServletResponse invalidUriSessionIdHttpResponse1 = new MockHttpServletResponse();
        WebSessionKey invalidUriIdSessionKey1 = new WebSessionKey(invalidUriSessionIdHttpRequest1, invalidUriSessionIdHttpResponse1);
        assertNull(this.webSessionManager.getSession(invalidUriIdSessionKey1));

        MockHttpServletRequest invalidUriSessionIdHttpRequest2 = new MockHttpServletRequest();
        invalidUriSessionIdHttpRequest2.setRequestURI("http://server.com/any.do;WRONGSESSIONIDNAME=key12345?foo=bar");
        MockHttpServletResponse invalidUriSessionIdHttpResponse2 = new MockHttpServletResponse();
        WebSessionKey invalidUriIdSessionKey2 = new WebSessionKey(invalidUriSessionIdHttpRequest2, invalidUriSessionIdHttpResponse2);
        assertNull(this.webSessionManager.getSession(invalidUriIdSessionKey2));

        MockHttpServletRequest validUriSessionIdHttpRequest = new MockHttpServletRequest();
        validUriSessionIdHttpRequest.setRequestURI("http://server.com/any.do;JSESSIONID=key12345;abc?foo=bar");
        MockHttpServletResponse validUriSessionIdHttpResponse = new MockHttpServletResponse();
        WebSessionKey validUriIdSessionKey = new WebSessionKey(validUriSessionIdHttpRequest, validUriSessionIdHttpResponse);
        try {
            this.webSessionManager.getSession(validUriIdSessionKey);
            fail("expect UnknownSessionException");
        } catch (UnknownSessionException e) {
            //expected exception
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("key12345",
                validUriSessionIdHttpRequest.getAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID));
        assertEquals(ShiroHttpServletRequest.URL_SESSION_ID_SOURCE,
                validUriSessionIdHttpRequest.getAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE));
    }

}
