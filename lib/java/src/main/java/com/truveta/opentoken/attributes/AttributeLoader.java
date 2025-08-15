/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.lang.reflect.Modifier;

import org.reflections.Reflections;

public final class AttributeLoader {

    private AttributeLoader() {
    }

    public static Set<Attribute> load() {
        Reflections reflections = new Reflections(AttributeLoader.class.getPackageName());

        Set<Class<? extends Attribute>> classes = reflections.getSubTypesOf(Attribute.class)
                .stream()
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .collect(Collectors.toSet());

        Set<Attribute> attributes = new HashSet<>();
        for (Class<? extends Attribute> clazz : classes) {
            try {
                attributes.add(clazz.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return attributes;
    }
}
