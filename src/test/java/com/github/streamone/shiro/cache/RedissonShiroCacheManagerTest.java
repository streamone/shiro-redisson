package com.github.streamone.shiro.cache;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.io.ResourceUtils;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.cache.CacheConfig;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * <p>RedissonShiroCacheManager test case.</p>
 *
 * @author streamone
 */
public class RedissonShiroCacheManagerTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructors() {
        RedissonClient client = Redisson.create();
        InputStream in = null;
        File invalidConfigFile = null;
        try {
            String configLocation = "classpath:cache-config.json";
            in = ResourceUtils.getInputStreamForPath(configLocation);
            Map<String, ? extends CacheConfig> configMap = CacheConfig.fromJSON(in);
            Codec codec = new JsonJacksonCodec();

            RedissonShiroCacheManager cacheManager1 = new RedissonShiroCacheManager(client);
            cacheManager1.setConfig(configMap);
            cacheManager1.setCodec(codec);
            cacheManager1.init();
            assertEquals(client, cacheManager1.getRedisson());
            assertEquals(codec, cacheManager1.getCodec());
            Cache<String, String> cache1 =  cacheManager1.getCache("testManagerCache1");
            assertNotNull(cache1);
            assertSame(cache1, cacheManager1.<String, String>getCache("testManagerCache1"));
            cache1.clear();
            Cache<String, String> ttlCache = cacheManager1.getCache("testManagerTTLCache");
            assertNotNull(ttlCache);
            assertSame(ttlCache, cacheManager1.<String, String>getCache("testManagerTTLCache"));
            ttlCache.clear();

            RedissonShiroCacheManager cacheManager2 = new RedissonShiroCacheManager(client, configMap);
            Cache<String, String> cache2 =  cacheManager2.getCache("testManagerCache2");
            assertNotNull(cache2);
            cache2.clear();

            RedissonShiroCacheManager cacheManager3 = new RedissonShiroCacheManager(client,
                configLocation);
            assertEquals(configLocation, cacheManager3.getConfigLocation());
            Cache<String, String> cache3 =  cacheManager3.getCache("testManagerCache3");
            assertNotNull(cache3);
            Cache<String, String> retrievedCache3 = cacheManager3.getCache("testManagerCache3");
            assertEquals(cache3, retrievedCache3);
            cache3.clear();

            String path = this.getClass().getClassLoader().getResource("cache-config.json").getFile();
            invalidConfigFile = File.createTempFile("cac", null, new File(path).getParentFile());
            RedissonShiroCacheManager cacheManager4 = new RedissonShiroCacheManager(client,
                "classpath:" + invalidConfigFile.getName());
            cacheManager4.init();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (client != null && !client.isShutdown()
                    && !client.isShuttingDown()) {
                client.shutdown();
            }

            if (in != null) {
                try {
                    in.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            if (invalidConfigFile != null && invalidConfigFile.exists()) {
                invalidConfigFile.delete();
            }
        }
    }

}
