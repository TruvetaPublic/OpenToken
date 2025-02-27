/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.parquet;

import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.hadoop.conf.Configuration;

import com.truveta.opentoken.attributes.Attribute;
import com.truveta.opentoken.attributes.AttributeLoader;
import com.truveta.opentoken.io.PersonAttributesReader;

import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.io.Closeable;

/**
 * Reads person attributes from a Parquet file.
 * Implements the {@link PersonAttributesReader} interface.
 */
public class PersonAttributesParquetReader implements PersonAttributesReader, Closeable {
    private ParquetReader<Group> reader;
    private Group currentGroup;
    private Iterator<Group> iterator;
    private boolean closed = false;
    private boolean hasNextCalled = false;

    private Map<String, Attribute> attributeMap = new HashMap<>();

    /**
     * Initialize the class with the input file in Parquet format.
     * 
     * @param filePath the input file path
     * @throws IOException if an I/O error occurs
     */
    public PersonAttributesParquetReader(String filePath) throws IOException {
        Configuration conf = new Configuration();
        Path path = new Path(filePath);

        Set<Attribute> attributes = AttributeLoader.load();
        for (Attribute attribute : attributes) {
            for (String alias : attribute.getAliases()) {
                attributeMap.put(alias.toLowerCase(), attribute);
            }

        }

        GroupReadSupport readSupport = new GroupReadSupport();

        this.reader = ParquetReader.builder(readSupport, path).withConf(conf).build();

        this.iterator = new Iterator<Group>() {
            @Override
            public boolean hasNext() {
                try {
                    currentGroup = reader.read();
                    return currentGroup != null;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public Group next() {
                return currentGroup;
            }
        };
    }

    @Override
    public boolean hasNext() {
        if (closed) {
            throw new NoSuchElementException("Reader is closed");
        }
        if (!hasNextCalled) {
            hasNextCalled = iterator.hasNext();
        }
        return hasNextCalled;
    }

    @Override
    public Map<Class<? extends Attribute>, String> next() {
        if (closed || !hasNextCalled) {
            throw new NoSuchElementException("Reader is closed");
        }
        hasNextCalled = false;

        Group group = iterator.next();
        Map<Class<? extends Attribute>, String> attributes = new HashMap<>();
        GroupType schema = group.getType();
        for (Type field : schema.getFields()) {
            String fieldName = field.getName();
            int fieldIndex = schema.getFieldIndex(fieldName);
            if (group.getFieldRepetitionCount(fieldIndex) > 0) {
                String fieldValue = group.getValueToString(fieldIndex, 0);
                attributes.put(attributeMap.get(fieldName.toLowerCase()).getClass(), fieldValue);
            }
        }

        return attributes;
    }

    @Override
    public void close() throws IOException {
        reader.close();
        closed = true;
    }
}
