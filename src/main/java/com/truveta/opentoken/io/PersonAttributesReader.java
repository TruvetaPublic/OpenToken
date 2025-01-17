/**
 * Copyright (c) Truveta. All rights reserved.
 * 
 * Represents an interface for reading person attributes.
 */
package com.truveta.opentoken.io;

import java.util.Iterator;
import java.util.Map;

/**
 * A generic interface for a streaming person attributes reader.
 */
public interface PersonAttributesReader extends Iterator<Map<String, String>>, AutoCloseable {

    /**
     * Retrieve the next set of person attributes from an input source.
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
     * 
     * @return a person attributes map.
     */
    @Override
    public Map<String, String> next();
}
