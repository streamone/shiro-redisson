package com.github.streamone.shiro.cache;

import com.github.streamone.cache.entity.KeyEntity;
import com.github.streamone.cache.entity.ValueEntity;
import org.apache.shiro.cache.Cache;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/appContext.xml")
public class RedissonShiroCacheTest {

    private static List<Cache> testCaches;

    @Resource(name = "cacheManager")
    private RedissonShiroCacheManager cacheManager;

    @BeforeClass
    public static void init() {
        testCaches = new LinkedList<>();
    }

    @AfterClass
    public static void destroy() {
        for (Cache cache : testCaches) {
            if (cache != null) {
                cache.clear();
            }
        }
    }

    @Test
    public void testStringKeys() {
        Cache<String, String> cache = this.cacheManager.getCache("testStringCache");
        testCaches.add(cache);
        Assert.assertNotNull(cache);

        cache.put("foo", "bar");
        cache.put("any", "one");
        Assert.assertEquals(2, cache.size());

        String val = cache.get("foo");
        Assert.assertEquals("bar", val);

        cache.remove("any");
        Assert.assertEquals(1, cache.size());
    }

    @Test
    public void testObjectKeys() {
        Cache<KeyEntity, ValueEntity> cache = this.cacheManager.getCache("testObjCache");
        testCaches.add(cache);
        Assert.assertNotNull(cache);

        KeyEntity fooKey = new KeyEntity("foo_key");
        ValueEntity fooVal = new ValueEntity("foo_val");
        KeyEntity barKey = new KeyEntity("bar_key");
        ValueEntity barVal = new ValueEntity("var_val");
        cache.put(fooKey, fooVal);
        cache.put(barKey, barVal);
        Assert.assertEquals(2, cache.size());

        ValueEntity val = cache.get(fooKey);
        Assert.assertEquals(fooVal, val);

        cache.remove(barKey);
        Assert.assertEquals(1, cache.size());
    }

    /**
     * <p>test cache config</p>
     * <p>the configs in cache-config.json.</p>
     */
    @Test
    public void testCacheConfig() throws InterruptedException {
        //ttl is 3000 ms
        Cache<String, String> ttlCache = this.cacheManager.getCache("testTTLCache");
        testCaches.add(ttlCache);
        ttlCache.put("foo", "bar");
        Thread.sleep(1000);
        Assert.assertEquals("bar", ttlCache.get("foo"));
        Thread.sleep(2000);
        Assert.assertNull(ttlCache.get("foo"));

        //max idle time is 500 ms
        Cache<String, String> maxIdleCache = this.cacheManager.getCache("testMaxIdleCache");
        testCaches.add(maxIdleCache);
        maxIdleCache.put("foo", "bar");
        maxIdleCache.put("some", "thing");
        Thread.sleep(200);
        maxIdleCache.get("foo");
        Thread.sleep(300);
        Assert.assertEquals("bar", maxIdleCache.get("foo"));
        Assert.assertNull(maxIdleCache.get("some"));

        //max size is 5
        Cache<String, String> maxSizeCache = this.cacheManager.getCache("testMaxSizeCache");
        testCaches.add(maxSizeCache);
        for (int i = 1; i <= 5; i++) {
            String key = "key" + String.valueOf(i);
            String val = "val" + String.valueOf(i);
            maxSizeCache.put(key, val);
        }
        Assert.assertEquals(5, maxSizeCache.size());
        maxSizeCache.put("foo", "bar");
        Assert.assertEquals(5, maxSizeCache.size());
        Assert.assertNull(maxSizeCache.get("key1"));
    }

}
