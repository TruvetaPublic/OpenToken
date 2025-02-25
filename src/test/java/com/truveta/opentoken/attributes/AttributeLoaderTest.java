/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.truveta.opentoken.attributes.general.RecordIdAttribute;
import com.truveta.opentoken.attributes.person.LastNameAttribute;

class AttributeLoaderTest {

    @Test
    void loadAttributes_ShouldLoadAttributes() {
        var attributesSet = AttributeLoader.load();

        var recordIdAttribute = attributesSet.stream()
                .filter(RecordIdAttribute.class::isInstance)
                .findFirst();
        assertTrue(recordIdAttribute.isPresent(), "RecordIdAttribute should be loaded");

        var lastNameAttribute = attributesSet.stream()
                .filter(LastNameAttribute.class::isInstance)
                .findFirst();
        assertTrue(lastNameAttribute.isPresent(), "LastNameAttribute should be loaded");
    }
}
