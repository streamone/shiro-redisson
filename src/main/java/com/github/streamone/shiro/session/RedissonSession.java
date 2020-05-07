package com.github.streamone.shiro.session;

import org.apache.shiro.session.ExpiredSessionException;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.StoppedSessionException;
import org.apache.shiro.session.mgt.AbstractSessionManager;
import org.redisson.RedissonScript;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;

import java.io.Serializable;
import java.util.*;

import static com.github.streamone.shiro.session.RedissonSessionScript.*;

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

    private RedissonClient redisson;
    private Codec infoCodec = new JsonJacksonCodec();
    private Codec codec = infoCodec;
    private String infoKey;
    private String attrKey;

    private Serializable id;

    public RedissonSession(RedissonClient redisson, Codec codec, String infoKey, String attrKey,
        Serializable id) {
        if (redisson == null || infoKey == null || attrKey == null || id == null) {
            throw new IllegalArgumentException("Arguments must not be null!");
        }

        this.redisson = redisson;
        if (codec != null) {
            this.codec = codec;
        }
        this.infoKey = infoKey;
        this.attrKey = attrKey;
        this.id = id;
    }

    public RedissonSession(RedissonClient redisson, Codec codec, String infoKey, String attrKey,
        Session session) {
        if (redisson == null || infoKey == null || attrKey == null || session == null) {
            throw new IllegalArgumentException("Arguments must not be null!");
        }

        if (session.getId() == null) {
            throw new IllegalArgumentException("Session id must not be null!");
        }

        this.redisson = redisson;
        if (codec != null) {
            this.codec = codec;
        }
        this.infoKey = infoKey;
        this.attrKey = attrKey;
        this.id = session.getId();
        init(session);
    }

    protected void init(final Session session) {
        final long timeout = session.getTimeout() > 0 ? session.getTimeout() :
            AbstractSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT;

        Date startTimeStamp = session.getStartTimestamp();
        startTimeStamp = startTimeStamp != null ? startTimeStamp : new Date();

        String host = session.getHost();
        host = host != null ? host : "";

        List<Object> keys = new ArrayList<>(1);
        keys.add(this.infoKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.infoCodec);
        script.eval(this.infoKey, RScript.Mode.READ_WRITE, INIT_SCRIPT,
            RScript.ReturnType.VALUE, keys, session.getId(), timeout, startTimeStamp,
            host);
    }

    @Override
    public Serializable getId() {
        return this.id;
    }

    @Override
    public Date getStartTimestamp() {
        List<Object> keys = new ArrayList<>(1);
        keys.add(this.infoKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.infoCodec);
        Date res = null;
        try {
            res = script.eval(this.infoKey, RScript.Mode.READ_ONLY,
                GET_START_SCRIPT,
                RScript.ReturnType.MAPVALUE, keys);
        } catch (RedisException e) {
            convertException(e);
        }

        if (res == null) {
            throw new InvalidSessionException();
        } else {
            return res;
        }
    }

    @Override
    public Date getLastAccessTime() {
        List<Object> keys = new ArrayList<>(1);
        keys.add(this.infoKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.infoCodec);
        Date res = null;
        try {
            res = script.eval(this.infoKey, RScript.Mode.READ_ONLY,
                GET_LAST_SCRIPT,
                RScript.ReturnType.MAPVALUE, keys);
        } catch (RedisException e) {
            convertException(e);
        }

        if (res == null) {
            throw new InvalidSessionException();
        } else {
            return res;
        }
    }

    @Override
    public long getTimeout() throws InvalidSessionException {
        List<Object> keys = new ArrayList<>(1);
        keys.add(this.infoKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.infoCodec);
        Long res = null;
        try {
            res = script.eval(this.infoKey, RScript.Mode.READ_ONLY,
                GET_TIMEOUT_SCRIPT,
                RScript.ReturnType.MAPVALUE, keys);
        } catch (RedisException e) {
            convertException(e);
        }

        if (res == null) {
            throw new InvalidSessionException();
        } else {
            return res;
        }
    }

    @Override
    public void setTimeout(long maxIdleTimeInMillis) throws InvalidSessionException {
        List<Object> keys = new ArrayList<>(1);
        keys.add(this.infoKey);
        keys.add(this.attrKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.infoCodec);
        try {
            script.eval(this.infoKey, RScript.Mode.READ_WRITE,
                SET_TIMEOUT_SCRIPT,
                RScript.ReturnType.VALUE, keys, maxIdleTimeInMillis);
        } catch (RedisException e) {
            convertException(e);
        }
    }

    @Override
    public String getHost() {
        List<Object> keys = new ArrayList<>(1);
        keys.add(this.infoKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.infoCodec);
        String res = null;
        try {
            res = script.eval(this.infoKey, RScript.Mode.READ_ONLY,
                GET_HOST_SCRIPT,
                RScript.ReturnType.MAPVALUE, keys);
        } catch (RedisException e) {
            convertException(e);
        }

        if (res == null) {
            throw new InvalidSessionException();
        } else {
            return res;
        }
    }

    @Override
    public void touch() throws InvalidSessionException {
        List<Object> keys = new ArrayList<>(2);
        keys.add(this.infoKey);
        keys.add(this.attrKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.infoCodec);
        try {
            script.eval(this.infoKey, RScript.Mode.READ_WRITE,
                TOUCH_SCRIPT, RScript.ReturnType.VALUE, keys, new Date());
        } catch (RedisException e) {
            convertException(e);
        }
    }

    @Override
    public void stop() throws InvalidSessionException {
        List<Object> keys = new ArrayList<>(1);
        keys.add(this.infoKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.infoCodec);
        try {
            script.eval(this.infoKey, RScript.Mode.READ_WRITE,
                STOP_SCRIPT, RScript.ReturnType.VALUE, keys, new Date());
        } catch (RedisException e) {
            convertException(e);
        }
    }

    @Override
    public Collection<Object> getAttributeKeys() throws InvalidSessionException {
        List<Object> keys = new ArrayList<>(2);
        keys.add(this.infoKey);
        keys.add(this.attrKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.codec);
        Collection<Object> res = null;
        try {
            res = script.eval(this.infoKey, RScript.Mode.READ_ONLY,
                GET_ATTRKEYS_SCRIPT, RScript.ReturnType.MAPVALUELIST, keys);
        } catch (RedisException e) {
            convertException(e);
        }

        if (res == null) {
            throw new InvalidSessionException();
        } else {
            return res;
        }
    }

    @Override
    public Object getAttribute(Object key) throws InvalidSessionException {
        List<Object> keys = new ArrayList<>(2);
        keys.add(this.infoKey);
        keys.add(this.attrKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.codec);
        Object res = null;
        try {
            res = script.eval(this.infoKey, RScript.Mode.READ_ONLY,
                GET_ATTR_SCRIPT, RScript.ReturnType.MAPVALUE, keys, key);
        } catch (RedisException e) {
            convertException(e);
        }

        return res;
    }

    @Override
    public void setAttribute(Object key, Object value) throws InvalidSessionException {
        List<Object> keys = new ArrayList<>(2);
        keys.add(this.infoKey);
        keys.add(this.attrKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.codec);
        try {
            script.eval(this.infoKey, RScript.Mode.READ_WRITE,
                SET_ATTR_SCRIPT, RScript.ReturnType.VALUE, keys, key, value);
        } catch (RedisException e) {
            convertException(e);
        }
    }

    @Override
    public Object removeAttribute(Object key) throws InvalidSessionException {
        List<Object> keys = new ArrayList<>(2);
        keys.add(this.infoKey);
        keys.add(this.attrKey);

        RedissonScript script = (RedissonScript) this.redisson.getScript(this.codec);
        Object res = null;
        try {
            res = script.eval(this.infoKey, RScript.Mode.READ_WRITE,
                REMOVE_ATTR_SCRIPT, RScript.ReturnType.MAPVALUE, keys, key);
        } catch (RedisException e) {
            convertException(e);
        }

        return res;
    }

    private void convertException(RedisException e) {
        String errMsg = e.getMessage();
        if (RETURN_CODE_EXPIRED.equals(errMsg)) {
            throw new ExpiredSessionException();
        } else if (RETURN_CODE_STOPPED.equals(errMsg)) {
            throw new StoppedSessionException();
        } else if (RETURN_CODE_INVALID.equals(errMsg)) {
            throw new InvalidSessionException();
        } else {
            throw e;
        }
    }

}
