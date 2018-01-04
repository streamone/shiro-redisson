package com.github.streamone.shiro.cache;

import com.github.streamone.cache.entity.KeyEntity;
import com.github.streamone.cache.entity.ValueEntity;
import org.apache.shiro.cache.Cache;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.spy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>RedissonShiroCache test case.</p>
 *
 * @author streamone
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/cacheContext.xml")
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
        assertNotNull(cache);

        cache.put("foo", "bar");
        ((RedissonShiroCache) cache).fastPut("any", "one");
        ((RedissonShiroCache) cache).putIfAbsent("other", "thing");
        ((RedissonShiroCache) cache).putIfAbsent("any", "two");
        ((RedissonShiroCache) cache).fastPutIfAbsent("any", "three");
        assertEquals(3, cache.size());
        assertEquals(3, cache.values().size());
        assertEquals("bar", cache.get("foo"));
        assertEquals("one", cache.get("any"));
        assertEquals("thing", cache.get("other"));

        cache.remove("any");
        assertFalse(cache.keys().contains("any"));

        ((RedissonShiroCache) cache).fastRemove("foo");
        assertFalse(cache.keys().contains("foo"));
    }

    @Test
    public void testObjectKeys() {
        Cache<KeyEntity, ValueEntity> cache = this.cacheManager.getCache("testObjCache");
        testCaches.add(cache);
        assertNotNull(cache);

        KeyEntity fooKey = new KeyEntity("foo_key");
        ValueEntity fooVal = new ValueEntity("foo_val");
        KeyEntity barKey = new KeyEntity("bar_key");
        ValueEntity barVal = new ValueEntity("var_val");
        cache.put(fooKey, fooVal);
        ((RedissonShiroCache) cache).fastPut(barKey, barVal);
        assertEquals(2, cache.size());

        ValueEntity val = cache.get(fooKey);
        assertEquals(fooVal, val);

        cache.remove(barKey);
        assertFalse(cache.keys().contains(barKey));
        ((RedissonShiroCache) cache).fastRemove(fooKey);
        assertFalse(cache.keys().contains(fooKey));
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
        assertEquals("bar", ttlCache.get("foo"));
        Thread.sleep(2000);
        assertNull(ttlCache.get("foo"));

        //max idle time is 500 ms
        Cache<String, String> maxIdleCache = this.cacheManager.getCache("testMaxIdleCache");
        testCaches.add(maxIdleCache);
        maxIdleCache.put("foo", "bar");
        maxIdleCache.put("some", "thing");
        Thread.sleep(200);
        maxIdleCache.get("foo");
        Thread.sleep(300);
        assertEquals("bar", maxIdleCache.get("foo"));
        assertNull(maxIdleCache.get("some"));

        //max size is 5
        Cache<String, String> maxSizeCache = this.cacheManager.getCache("testMaxSizeCache");
        testCaches.add(maxSizeCache);
        for (int i = 1; i <= 5; i++) {
            String key = "key" + String.valueOf(i);
            String val = "val" + String.valueOf(i);
            maxSizeCache.put(key, val);
        }
        assertEquals(5, maxSizeCache.size());
        maxSizeCache.put("foo", "bar");
        assertEquals(5, maxSizeCache.size());
        assertNull(maxSizeCache.get("key1"));
    }

    @Test
    public void testNullValues() {
        RedissonShiroCacheManager cloneCacheManager = spy(this.cacheManager);
        cloneCacheManager.setAllowNullValues(true);
        assertTrue(cloneCacheManager.isAllowNullValues());

        Cache<String, String> allowNullValuesCache = cloneCacheManager.getCache("testAllowNullValuesCache");
        testCaches.add(allowNullValuesCache);
        // this cache is configed in cache-config.json
        Cache<String, String> allowNullConfigedCache = cloneCacheManager.getCache("testAllowNullConfigedCache");
        testCaches.add(allowNullConfigedCache);

        cloneCacheManager.setAllowNullValues(false);
        assertFalse(cloneCacheManager.isAllowNullValues());

        Cache<String, String> disallowNullValuesCache =
                cloneCacheManager.getCache("testDisallowNullValuesCache");
        testCaches.add(disallowNullValuesCache);
        Cache<String, String> disallowNullConfigedCache =
                cloneCacheManager.getCache("testDisallowNullConfigedCache");
        testCaches.add(disallowNullConfigedCache);

        doTestNull(allowNullValuesCache, disallowNullValuesCache);

        doTestNull(allowNullConfigedCache, disallowNullConfigedCache);
    }

    private void doTestNull(Cache<String, String> allowCache, Cache<String, String> disallowCache) {
        allowCache.put("foo", null);
        assertNull(allowCache.get("foo"));
        assertTrue(allowCache.keys().contains("foo"));

        ((RedissonShiroCache) allowCache).putIfAbsent("bar", null);
        assertNull(allowCache.get("bar"));
        assertTrue(allowCache.keys().contains("bar"));


        ((RedissonShiroCache) allowCache).fastPut("any", null);
        assertNull(allowCache.get("any"));
        assertTrue(allowCache.keys().contains("any"));

        ((RedissonShiroCache) allowCache).fastPutIfAbsent("other", null);
        assertNull(allowCache.get("other"));
        assertTrue(allowCache.keys().contains("other"));

        disallowCache.put("foo", null);
        assertNull(disallowCache.get("foo"));
        assertFalse(disallowCache.keys().contains("foo"));

        ((RedissonShiroCache) disallowCache).putIfAbsent("bar", null);
        assertNull(disallowCache.get("bar"));
        assertFalse(disallowCache.keys().contains("bar"));

        ((RedissonShiroCache) disallowCache).fastPut("any", null);
        assertNull(disallowCache.get("any"));
        assertFalse(disallowCache.keys().contains("any"));

        ((RedissonShiroCache) disallowCache).fastPutIfAbsent("other", null);
        assertNull(disallowCache.get("other"));
        assertFalse(disallowCache.keys().contains("other"));
    }

}
