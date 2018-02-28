package com.github.streamone.shiro.cache;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.subject.Subject;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <p>.</p>
 *
 * @author streamone
 */
public class RedissonShiroCacheIniTest {

    @Test
    public void test() {
        DefaultSecurityManager securityManager = new DefaultSecurityManager();
        SecurityUtils.setSecurityManager(securityManager);

        IniRealm iniRealm = new IniRealm("classpath:shiro.ini");
        iniRealm.setCachingEnabled(true);
        iniRealm.setAuthenticationCachingEnabled(true);
        securityManager.setRealm(iniRealm);

        Config redissonCfg = new Config();
        redissonCfg.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonClient redisson = Redisson.create(redissonCfg);
        RedissonShiroCacheManager cacheManager = new RedissonShiroCacheManager();
        cacheManager.setRedisson(redisson);
        cacheManager.setConfigLocation("classpath:cache-config.json");
        cacheManager.init();
        securityManager.setCacheManager(cacheManager);

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken("wang", "123");
        try {
            subject.login(token);
        } catch (AuthenticationException e) {
            //
        }
        assertTrue(subject.isAuthenticated());

        Cache<Object, AuthenticationInfo> authenticationInfoCache = iniRealm.getAuthenticationCache();
        assertEquals("123", authenticationInfoCache.get("wang").getCredentials());

        authenticationInfoCache.clear();
    }
}
