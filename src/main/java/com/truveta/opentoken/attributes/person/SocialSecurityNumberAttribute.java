/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.NotInValidator;
import com.truveta.opentoken.attributes.validation.RegexValidator;

/**
 * Represents the social security number attribute.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * social security number fields. It recognizes "SocialSecurityNumber" and
 * "NationalIdentificationNumber" as valid aliases for this attribute type.
 * 
 * The attribute performs normalization on input values, converting them to a
 * standard format (xxx-xx-xxxx).
 * 
 * The attribute also performs validation on input values, ensuring they match
 * the following format:
 * - xxx-xx-xxxx
 * - xxxxxxxxx
 */
public class SocialSecurityNumberAttribute extends BaseAttribute {

    private static final String NAME = "SocialSecurityNumber";
    private static final String[] ALIASES = new String[] { NAME, "NationalIdentificationNumber" };
    private static final String SSN_REGEX = "^(?!0{3})(?!6{3})[0-8]\\d{2}-?(?!0{2})\\d{2}-?(?!0{4})\\d{4}$";

    public SocialSecurityNumberAttribute() {
        super(List.of(
                new NotInValidator(
                        List.of(
                                "000-00-0000",
                                "111-11-1111",
                                "222-22-2222",
                                "333-33-3333",
                                "444-44-4444",
                                "555-55-5555",
                                "666-66-6666",
                                "777-77-7777",
                                "888-88-8888",
                                "999-99-9999")),
                new RegexValidator(SSN_REGEX)));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String[] getAliases() {
        return ALIASES;
    }

    @Override
    public String normalize(String value) {
        return value;
    }
}
