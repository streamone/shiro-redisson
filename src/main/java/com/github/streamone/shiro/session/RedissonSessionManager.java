package com.github.streamone.shiro.session;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.*;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * <p>A {@link org.apache.shiro.session.mgt.SessionManager} implementation backed by Redisson Objects.</p>
 *
 * @author streamone
 */
public class RedissonSessionManager extends AbstractNativeSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(RedissonSessionManager.class);

    private SessionFactory sessionFactory;

    protected SessionDAO sessionDAO;

    public RedissonSessionManager() {
        this.sessionFactory = new SimpleSessionFactory();
    }

    @Override
    protected Session createSession(SessionContext context) throws AuthorizationException {
        Session s = newSessionInstance(context);
        if (logger.isTraceEnabled()) {
            logger.trace("Creating session for host {}", s.getHost());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Creating new EIS record for new session instance [" + s + "]");
        }
        sessionDAO.create(s);
        return s;
    }

    @Override
    protected Session doGetSession(SessionKey key) throws InvalidSessionException {
        if (logger.isTraceEnabled()) {
            logger.trace("Attempting to retrieve session with key {}", key);
        }
        Serializable sessionId = getSessionId(key);
        if (sessionId == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to resolve session ID from SessionKey [{}].  Returning null to indicate a " +
                        "session could not be found.", key);
            }
            return null;
        }
        Session s = sessionDAO.readSession(sessionId);
        if (s == null) {
            //session ID was provided, meaning one is expected to be found, but we couldn't find one:
            String msg = "Could not find session with ID [" + sessionId + "]";
            throw new UnknownSessionException(msg);
        }
        return s;
    }

    @Override
    protected void afterStopped(Session session) {
        this.sessionDAO.delete(session);
    }

    protected Session newSessionInstance(SessionContext context) {
        return getSessionFactory().createSession(context);
    }

    protected Serializable getSessionId(SessionKey sessionKey) {
        return sessionKey.getSessionId();
    }



    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public SessionDAO getSessionDAO() {
        return sessionDAO;
    }

    public void setSessionDAO(SessionDAO sessionDAO) {
        this.sessionDAO = sessionDAO;
    }
}
