package com.github.streamone.shiro.cache;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.spring.cache.CacheConfig;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>A {@link org.apache.shiro.cache.CacheManager} implementation
 * backed by Redisson instance.</p>
 *
 * @author streamone
 */
public class RedissonShiroCacheManager implements CacheManager, ResourceLoaderAware, InitializingBean {

    private ResourceLoader resourceLoader;

    private boolean allowNullValues = true;

    private Codec codec;

    private RedissonClient redisson;

    private String configLocation;

    private Map<String, CacheConfig> configMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Cache> instanceMap = new ConcurrentHashMap<>();

    public RedissonShiroCacheManager(){}

    public RedissonShiroCacheManager(RedissonClient redisson){
        this(redisson, (String)null, null);
    }

    public RedissonShiroCacheManager(RedissonClient redisson, Map<String, ? extends CacheConfig> config) {
        this(redisson, config, null);
    }

    public RedissonShiroCacheManager(RedissonClient redisson, Map<String, ? extends CacheConfig> config, Codec codec) {
        this.redisson = redisson;
        this.configMap = (Map<String, CacheConfig>) config;
        this.codec = codec;
    }

    public RedissonShiroCacheManager(RedissonClient redisson, String configLocation) {
        this(redisson, configLocation, null);
    }

    public RedissonShiroCacheManager(RedissonClient redisson, String configLocation, Codec codec) {
        this.redisson = redisson;
        this.configLocation = configLocation;
        this.codec = codec;
    }

    protected CacheConfig createDefaultConfig() {
        return new CacheConfig();
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name) throws CacheException {
        Cache<K, V> cache = this.instanceMap.get(name);
        if (cache != null) {
            return cache;
        }

        CacheConfig config = this.configMap.get(name);
        if (config == null) {
            config = createDefaultConfig();
            configMap.put(name, config);
        }

        if (config.getMaxIdleTime() == 0 && config.getTTL() == 0 && config.getMaxSize() == 0) {
            return createMap(name, config);
        }

        return createMapCache(name, config);
    }

    private <K, V> Cache<K, V> createMap(String name, CacheConfig config) {
        RMap<K, Object> map = getMap(name, config);
        Cache<K, V> cache = new RedissonShiroCache<>(map, this.allowNullValues);
        Cache<K, V> oldCache = this.instanceMap.putIfAbsent(name, cache);
        if (oldCache != null) {
            cache = oldCache;
        }
        return cache;
    }

    protected <K> RMap<K, Object> getMap(String name, CacheConfig config) {
        if (this.codec != null) {
            return  this.redisson.getMap(name, this.codec);
        }
        return this.redisson.getMap(name);
    }

    private <K, V> Cache<K, V> createMapCache(String name, CacheConfig config) {
        RMapCache<K, Object> map = getMapCache(name, config);
        Cache<K, V> cache = new RedissonShiroCache<>(map, config, this.allowNullValues);
        Cache<K, V> oldCache = this.instanceMap.putIfAbsent(name, cache);
        if (oldCache != null) {
            cache = oldCache;
        } else {
            map.setMaxSize(config.getMaxSize());
        }
        return cache;
    }

    protected <K> RMapCache<K, Object> getMapCache(String name, CacheConfig config) {
        if (this.codec != null) {
            return this.redisson.getMapCache(name, this.codec);
        }
        return redisson.getMapCache(name);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.configLocation == null) {
            return;
        }

        Resource resource = resourceLoader.getResource(this.configLocation);
        try {
            this.configMap = (Map<String, CacheConfig>) CacheConfig.fromJSON(resource.getInputStream());
        } catch (IOException e) {
            // try to read yaml
            try {
                this.configMap = (Map<String, CacheConfig>) CacheConfig.fromYAML(resource.getInputStream());
            } catch (IOException e1) {
                throw new BeanDefinitionStoreException(
                        "Could not parse cache configuration at [" + configLocation + "]", e1);
            }
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setConfig(Map<String, ? extends CacheConfig> config) {
        this.configMap = (Map<String, CacheConfig>) config;
    }

    public RedissonClient getRedisson() {
        return redisson;
    }

    public void setRedisson(RedissonClient redisson) {
        this.redisson = redisson;
    }

    public Codec getCodec() {
        return codec;
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }

    public String getConfigLocation() {
        return configLocation;
    }

    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }

    public boolean isAllowNullValues() {
        return allowNullValues;
    }

    public void setAllowNullValues(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
    }
}
