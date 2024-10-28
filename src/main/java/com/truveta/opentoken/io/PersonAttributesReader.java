/**
 * Copyright (c) Truveta. All rights reserved.
 * Represents an interface for reading person attributes.
 */
package com.truveta.opentoken.io;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A generic interface for the person attributes reader.
 */
public interface PersonAttributesReader {
    /**
     * Read person attributes from a given input source.
     * <p>
     * Example person attribute map:
     * <code>
     * {
     *   RecordId: 2ea45fee-90c3-494a-a503-36022c9e1281,
     *   FirstName: John,
     *   LastName: Doe,
     *   Gender: Male,
     *   BirthDate: 01/01/2001,
     *   PostalCode: 54321,
     *   SocialSecurityNumber: 123-45-6789
     * }
     * </code>
     * @return a list of person attributes map.
     * @throws java.io.IOException errors encountered while reading from the input data source.
     */
    List<Map<String, String>> readAttributes() throws IOException;
}
