/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens;

import java.util.ArrayList;
import java.util.Set;

/**
 * A generic interface for the token definition.
 */
public interface BaseTokenDefinition {
    /**
     * Person attribute name for the <b>First Name</b>.
     */
    String FIRST_NAME = "FirstName";

    /**
     * Person attribute name for the <b>Last Name</b>.
     */
    String LAST_NAME = "LastName";

    /**
     * Person attribute name for the <b>Gender</b>.
     */
    String GENDER = "Gender";

    /**
     * Person attribute name for the <b>Birth Date</b>.
     */
    String BIRTH_DATE = "BirthDate";

    /**
     * Person attribute name for the <b>Postal/Zip Code</b>.
     */
    String POSTAL_CODE = "PostalCode";

    /**
     * Person attribute name for the <b>SSN</b>.
     */
    String SOCIAL_SECURITY_NUMBER = "SocialSecurityNumber";

    /**
     * Get the version of the token definition.
     * 
     * @return the token definition version.
     */
    String getVersion();

    /**
     * Get all token identifiers. For example, a set of
     * <code>{ T1, T2, T3, T4, T5 }</code>.
     * <p>
     * The token identifiers are also called rule identifiers because every token is
     * generated from rule definition.
     * 
     * @return a set of token identifiers.
     */
    Set<String> getTokenIdentifiers();

    /**
     * Get the token definition for a given token identifier.
     * 
     * @param tokenId the token/rule identifier.
     * 
     * @return a list of token/rule definition.
     */
    ArrayList<AttributeExpression> getTokenDefinition(String tokenId);
}
