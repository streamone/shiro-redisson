package com.github.streamone.shiro.session;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * <p>A {@link SessionDAO} implementation backed by Redisson Objects.</p>
 *
 * @author streamone
 */
public class RedissonSessionDao extends AbstractSessionDAO {

    public static final String SESSION_INFO_KEY_PREFIX = "session:info:";

    public static final String SESSION_ATTR_KEY_PREFIX = "session:attr:";

    private RedissonClient redisson;

    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = generateSessionId(session);
        assignSessionId(session, sessionId);
        RMap<String, Object> sessionInfoMap = this.redisson.getMap(SESSION_INFO_KEY_PREFIX + sessionId);
        RMap<Object, Object> sessionAttrMap = this.redisson.getMap(SESSION_ATTR_KEY_PREFIX + sessionId);
        new RedissonSession(sessionInfoMap, sessionAttrMap, session);
        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        RMap<String, Object> sessionInfoMap = this.redisson.getMap(SESSION_INFO_KEY_PREFIX + sessionId);
        RMap<Object, Object> sessionAttrMap = this.redisson.getMap(SESSION_ATTR_KEY_PREFIX + sessionId);
        if (sessionInfoMap.remainTimeToLive() > 0) {
            return new RedissonSession(sessionInfoMap, sessionAttrMap, sessionId);
        } else {
            return null;
        }
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        //do nothing, the RedissonSession will update the session in redis directly
    }

    @Override
    public void delete(Session session) {
        if (session == null || StringUtils.isEmpty(session.getId())) {
            return;
        }
        Serializable sessionId = session.getId();
        RMap<String, Object> sessionInfoMap = this.redisson.getMap(SESSION_INFO_KEY_PREFIX + sessionId);
        RMap<Object, Object> sessionAttrMap = this.redisson.getMap(SESSION_ATTR_KEY_PREFIX + sessionId);
        sessionInfoMap.unlink();
        sessionAttrMap.unlink();
    }

    @Override
    public Collection<Session> getActiveSessions() {
        //for performance reasons, this method should not be called
        return Collections.EMPTY_LIST;
    }



    public RedissonClient getRedisson() {
        return redisson;
    }

    public void setRedisson(RedissonClient redisson) {
        this.redisson = redisson;
    }
}
