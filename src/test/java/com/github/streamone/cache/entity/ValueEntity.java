package com.github.streamone.cache.entity;

/**
 * <p>A value entity class for cache testing.</p>
 *
 * @author streamone
 */
public class ValueEntity {

    private String value;

    public ValueEntity() {}

    public ValueEntity(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ValueEntity) {
            return obj.toString().equals(this.toString());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.value;
    }
}
