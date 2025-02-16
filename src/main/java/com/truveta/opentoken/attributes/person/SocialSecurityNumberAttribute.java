/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.NotInValidator;
import com.truveta.opentoken.attributes.validation.RegexValidator;

public class SocialSecurityNumberAttribute extends BaseAttribute {

    private static final String NAME = "SocialSecurityNumber";
    private static final String[] ALIASES = new String[] { NAME, "NationalIdentificationNumber" };
    private static final String SSN_REGEX = "^(?!0{3})(?!6{3})[0-8]\\d{2}-?(?!0{2})\\d{2}-?(?!0{4})\\d{4}$";

    protected SocialSecurityNumberAttribute() {
        super(List.of(
                new NotInValidator(
                        NAME,
                        List.of(
                                "000-00-0000",
                                "000000000",
                                "111-11-1111",
                                "111111111",
                                "222-22-2222",
                                "222222222",
                                "333-33-3333",
                                "333333333",
                                "444-44-4444",
                                "444444444",
                                "555-55-5555",
                                "555555555",
                                "666-66-6666",
                                "666666666",
                                "777-77-7777",
                                "777777777",
                                "888-88-8888",
                                "888888888",
                                "999-99-9999",
                                "999999999"

                        )),
                new RegexValidator(NAME, SSN_REGEX)));
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
