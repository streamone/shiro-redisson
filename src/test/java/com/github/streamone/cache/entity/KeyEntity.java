package com.github.streamone.cache.entity;

import java.io.Serializable;

/**
 * <p>A key entity class for cache testing.</p>
 *
 * @author streamone
 */
public class KeyEntity implements Serializable {

    private String key;

    public KeyEntity() {}

    public KeyEntity(String key) {
        this.key = key;
    }

}
