package com.github.streamone.shiro.util;

import org.apache.shiro.util.ByteSource;
import org.apache.shiro.util.SimpleByteSource;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;

/**
 * <p>A serializable implementation of {@link ByteSource}.</p>
 *
 * @author streamone
 */
public class SerializableByteSource  extends SimpleByteSource implements Serializable {

    /**
     * <p>Add default constructor to support more serialization solution such as jackson.</p>
     */
    public SerializableByteSource(){
        super((byte[]) null);
    }

    public SerializableByteSource(byte[] bytes){
        super(bytes);
    }

    public SerializableByteSource(char[] chars) {
        super(chars);
    }

    public SerializableByteSource(String string) {
        super(string);
    }

    public SerializableByteSource(ByteSource source) {
        super(source);
    }

    public SerializableByteSource(File file) {
        super(file);
    }

    public SerializableByteSource(InputStream stream) {
        super(stream);
    }

}
