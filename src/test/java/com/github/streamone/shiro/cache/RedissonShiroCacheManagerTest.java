package com.github.streamone.shiro.cache;

import org.apache.shiro.cache.Cache;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.cache.CacheConfig;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * <p>RedissonShiroCacheManager test case.</p>
 *
 * @author streamone
 */
public class RedissonShiroCacheManagerTest {

    @Test(expected = BeanDefinitionStoreException.class)
    public void testConstructors() {
        RedissonClient client = Redisson.create();
        InputStream in = null;
        File invalidConfigFile = null;
        try {
            in = this.getClass().getResourceAsStream("/cache-config.json");
            Map<String, ? extends CacheConfig> configMap = CacheConfig.fromJSON(in);
            Codec codec = new JsonJacksonCodec();
            String configLocation = "classpath:/cache-config.json";

            RedissonShiroCacheManager cacheManager1 = new RedissonShiroCacheManager(client);
            cacheManager1.setConfig(configMap);
            cacheManager1.setCodec(codec);
            cacheManager1.afterPropertiesSet();
            assertEquals(client, cacheManager1.getRedisson());
            assertEquals(codec, cacheManager1.getCodec());
            Cache<String, String> cache1 =  cacheManager1.getCache("testManagerCache1");
            assertNotNull(cache1);
            cache1.clear();

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

            invalidConfigFile = File.createTempFile("cac", null);
            RedissonShiroCacheManager cacheManager4 = new RedissonShiroCacheManager(client,
                "file:" + invalidConfigFile.getCanonicalPath());
            cacheManager4.setResourceLoader(new DefaultResourceLoader());
            cacheManager4.afterPropertiesSet();
        } catch (BeanDefinitionStoreException e) {
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

            if (invalidConfigFile != null) {
                invalidConfigFile.delete();
            }
        }
    }

}
