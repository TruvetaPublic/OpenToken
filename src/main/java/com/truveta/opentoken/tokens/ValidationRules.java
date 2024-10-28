// Copyright (c) Truveta. All rights reserved.

package com.truveta.opentoken.tokens;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
 
/**
 * Maintains a collection of validators that asserts
 * if validity of person attributes values.
 */
public class ValidationRules {
    private final List<AttributeValidator> validationRules;

    /**
     * Initializes all the validation rules.
     */
    public ValidationRules() {
        // Create regex expressions for validation rules
        final String ssnExpression = "^(?!0{3})(?!6{3})[0-8]\\d{2}-(?!0{2})\\d{2}-(?!0{4})\\d{4}$";
        final String genderExpression = "^(Male|Female)$";
        final String postalCodeExpression = "^\\d{5}(-\\d{4})?$";
        
        validationRules = new ArrayList<AttributeValidator>();

        // Validate all attributes to ensure they are not null
        validationRules.add(new NullValidator("*"));
        validationRules.add(new NotInValidator("SocialSecurityNumber",
            new String[] {
                "000-00-0000",
                "111-11-1111",
                "222-22-2222",
                "333-33-3333",
                "444-44-4444",
                "555-55-5555",
                "666-66-6666",
                "777-77-7777",
                "888-88-8888",
                "999-99-9999"
            }
        ));
        validationRules.add(new RegexValidator("SocialSecurityNumber", ssnExpression));
        validationRules.add(new RegexValidator("Gender", genderExpression));
        validationRules.add(new RegexValidator("PostalCode", postalCodeExpression));
    }

    /**
     * Get all the validators.
     * 
     * @return a list of the attribute validators.
     */
    public List<AttributeValidator> getValidationRules() {
        return validationRules;
    }

    /*
     * Validate person attribute.
     *
     * @param personAttributes The person attributes. It is a map of the person attribute
     * name to value. This version of the library supports the following person attributes:
     * <ul>
     *   <li>FirstName</li>
     *   <li>LastName</li>
     *   <li>Gender</li>
     *   <li>BirthDate</li>
     *   <li>PostalCode</li>
     *   <li>SocialSecurityNumber</li>
     * </ul>
     * @param attributeName the person attribute name.
     * 
     * @return <code>true</code> if the attribute value is valid; <code>false</false> otherwise.
     */
    public boolean validate(Map<String, String> personAttributes, String attributeName) {
        for (AttributeValidator validationRule : validationRules) {
            if (!validationRule.eval(attributeName, personAttributes.get(attributeName))) {
                return false;
            }
        }
        return true;
    }
}
