package com.github.streamone.shiro.session;

import org.apache.shiro.session.ExpiredSessionException;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.StoppedSessionException;
import org.apache.shiro.session.mgt.AbstractSessionManager;
import org.redisson.api.RMap;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * <p>A {@link org.apache.shiro.session.Session} implementation backed by Redisson Objects.</p>
 *
 * @author streamone
 */
public class RedissonSession implements Session {

    public static final String INFO_ID_KEY = "id";
    public static final String INFO_START_KEY = "startTimestamp";
    public static final String INFO_STOP_KEY = "stopTimestamp";
    public static final String INFO_LAST_KEY = "lastAccessTime";
    public static final String INFO_TIMEOUT_KEY = "timeout";
    public static final String INFO_HOST_KEY = "host";

    private RMap<String, Object> info;

    private RMap<Object, Object> attributes;

    private Serializable id;

    public RedissonSession(RMap<String, Object> info, RMap<Object, Object> attributes, Serializable id) {
        if (info == null || attributes == null || id == null) {
            throw new IllegalArgumentException("Arguments must not be null!");
        }

        this.info = info;
        this.attributes = attributes;
        this.id = id;

        checkState();
    }

    public RedissonSession(RMap<String, Object> info, RMap<Object, Object> attributes, Session session) {
        if (info == null || attributes == null || session == null) {
            throw new IllegalArgumentException("Arguments must not be null!");
        }

        if (session.getId() == null) {
            throw new IllegalArgumentException("Session id must not be null!");
        }

        this.info = info;
        this.attributes = attributes;
        this.id = session.getId();
        init(session);
    }

    protected void init(final Session session) {
        final long timeout = session.getTimeout() > 0 ? session.getTimeout() :
            AbstractSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT;

        this.info.putAll(new HashMap<String, Object>(16) {
            {
                put(INFO_ID_KEY, session.getId());

                put(INFO_TIMEOUT_KEY, timeout);

                Date startTimeStamp = session.getStartTimestamp();
                if (startTimeStamp != null) {
                    put(INFO_START_KEY, startTimeStamp);
                    put(INFO_LAST_KEY, startTimeStamp);
                } else {
                    final Date currentTime = new Date();
                    put(INFO_START_KEY, currentTime);
                    put(INFO_LAST_KEY, currentTime);
                }

                String host = session.getHost();
                if ( host != null) {
                    put(INFO_HOST_KEY, host);
                }
            }
        });

        this.info.expire(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * <p>check the session state</p>
     */
    protected void checkState() {
        if (!this.info.isExists()) {
            throw new ExpiredSessionException();
        }

        if (this.info.containsKey(INFO_STOP_KEY)) {
            throw new StoppedSessionException();
        }
    }

    @Override
    public Serializable getId() {
        return this.id;
    }

    @Override
    public Date getStartTimestamp() {
        Date res = (Date)this.info.get(INFO_START_KEY);
        if (res == null) {
            checkState();
            throw new InvalidSessionException();
        }
        return res;
    }

    @Override
    public Date getLastAccessTime() {
        Date res = (Date)this.info.get(INFO_LAST_KEY);
        if (res == null) {
            checkState();
            throw new InvalidSessionException();
        }
        return res;
    }

    @Override
    public long getTimeout() throws InvalidSessionException {
        Long res = (Long)this.info.get(INFO_TIMEOUT_KEY);
        if (res == null) {
            checkState();
            throw new InvalidSessionException();
        }
        return (long)res;
    }

    @Override
    public void setTimeout(long maxIdleTimeInMillis) throws InvalidSessionException {
        Long prev = (Long)this.info.replace(INFO_TIMEOUT_KEY, maxIdleTimeInMillis);
        if (prev == null) {
            checkState();
            throw new InvalidSessionException();
        }
        if (prev != maxIdleTimeInMillis) {
            this.info.expire(maxIdleTimeInMillis, TimeUnit.MILLISECONDS);
            this.attributes.expire(maxIdleTimeInMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public String getHost() {
        String res = (String)this.info.get(INFO_HOST_KEY);
        if (res == null) {
            checkState();
        }
        return res;
    }

    @Override
    public void touch() throws InvalidSessionException {
        Long timeout = (Long)this.info.get(INFO_TIMEOUT_KEY);
        if (timeout == null) {
            checkState();
            throw new InvalidSessionException();
        }
        this.info.replace(INFO_LAST_KEY, new Date());
        this.info.expire(timeout, TimeUnit.MILLISECONDS);
        this.attributes.expire(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() throws InvalidSessionException {
        checkState();
        this.info.putIfAbsent(INFO_STOP_KEY, new Date());
    }

    @Override
    public Collection<Object> getAttributeKeys() throws InvalidSessionException {
        checkState();
        return this.attributes.readAllKeySet();
    }

    @Override
    public Object getAttribute(Object key) throws InvalidSessionException {
        checkState();
        return this.attributes.get(key);
    }

    @Override
    public void setAttribute(Object key, Object value) throws InvalidSessionException {
        checkState();
        this.attributes.fastPut(key, value);
        //the attributes map is not exist before first setAttribute
        if (this.attributes.remainTimeToLive() < 0) {
            long timeout = this.info.remainTimeToLive();
            //the info map is not exist, so this session is expired or stopped
            if (timeout < 0) {
                this.info.unlink();
                this.attributes.unlink();
                throw new InvalidSessionException();
            } else {
                this.attributes.expire(timeout, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public Object removeAttribute(Object key) throws InvalidSessionException {
        checkState();
        return this.attributes.remove(key);
    }

}
