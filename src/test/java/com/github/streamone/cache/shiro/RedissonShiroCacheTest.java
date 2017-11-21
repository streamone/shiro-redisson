package com.github.streamone.cache.shiro;

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
    public void testStringCache() {
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

}
