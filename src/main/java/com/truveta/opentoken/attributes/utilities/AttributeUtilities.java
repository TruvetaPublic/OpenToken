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
     * Pattern that matches any character that is not an alphabetic character (a-z
     * or A-Z).
     * Used for removing or identifying non-alphabetic characters in strings.
     */
    public static final Pattern NON_ALPHABETIC_PATTERN = Pattern.compile("[^a-zA-Z]");

    private AttributeUtilities() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Removes diacritic marks from the given string.
     * 
     * This method performs the following steps:
     * 1. Trims the input string
     * 2. Normalizes the string using NFD form, which separates characters from
     * their diacritical marks
     * 3. Removes all diacritical marks using a predefined regular expression
     * pattern
     * 
     * @param value The string from which to remove diacritical marks
     * @return A new string with all diacritical marks removed
     */
    public static String normalizeDiacritics(String value) {
        return DIACRITICS.matcher(Normalizer.normalize(value.trim(), Normalizer.Form.NFD)).replaceAll("");
    }
}
