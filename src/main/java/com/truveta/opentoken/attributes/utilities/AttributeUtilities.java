/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.utilities;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * This class includes functions such as normalizing accents,
 * standardizing formats, and other attribute-related transformations.
 * 
 */
public class AttributeUtilities {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

    /**
     * Pattern that matches one or more whitespace characters.
     * This includes spaces, tabs, line breaks, and other Unicode whitespace.
     * 
     * Examples:
     * " " -> single space
     * "\t" -> tab
     * "\n" -> newline
     * "\r\n" -> carriage return + newline
     * " " -> multiple spaces
     */
    public static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private AttributeUtilities() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String normalize(String value) {
        return DIACRITICS.matcher(Normalizer.normalize(value.trim(), Normalizer.Form.NFD)).replaceAll("");
    }
}
