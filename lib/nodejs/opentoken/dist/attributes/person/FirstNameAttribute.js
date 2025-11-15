"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.FirstNameAttribute = void 0;
const BaseAttribute_1 = require("../BaseAttribute");
const NotInValidator_1 = require("../validation/NotInValidator");
const AttributeUtilities_1 = require("../utilities/AttributeUtilities");
/**
 * Represents the first name of a person.
 *
 * This class extends BaseAttribute and provides functionality for working with
 * first name fields. It recognizes "FirstName" and "GivenName" as valid aliases
 * for this attribute type.
 */
class FirstNameAttribute extends BaseAttribute_1.BaseAttribute {
    constructor() {
        super(FirstNameAttribute.NAME, FirstNameAttribute.ALIASES, [new NotInValidator_1.NotInValidator(AttributeUtilities_1.AttributeUtilities.COMMON_PLACEHOLDER_NAMES)]);
    }
    normalize(value) {
        let normalizedValue = AttributeUtilities_1.AttributeUtilities.normalizeDiacritics(value);
        // Remove common titles and title abbreviations
        const valueWithoutTitle = normalizedValue.replace(FirstNameAttribute.TITLE_PATTERN, '');
        // If the title removal doesn't result in an empty string, use the title-less value
        if (valueWithoutTitle.length > 0) {
            normalizedValue = valueWithoutTitle;
        }
        // Remove generational suffixes
        const valueWithoutSuffix = normalizedValue.replace(AttributeUtilities_1.AttributeUtilities.GENERATIONAL_SUFFIX_PATTERN, '');
        // If the generational suffix removal doesn't result in an empty string, use it
        if (valueWithoutSuffix.length > 0) {
            normalizedValue = valueWithoutSuffix;
        }
        // Remove trailing periods and middle initials
        normalizedValue = normalizedValue.replace(FirstNameAttribute.TRAILING_PERIOD_AND_INITIAL_PATTERN, '');
        // Remove dashes, spaces and other non-alphabetic characters
        normalizedValue = normalizedValue.replace(AttributeUtilities_1.AttributeUtilities.NON_ALPHABETIC_PATTERN, '');
        return normalizedValue;
    }
}
exports.FirstNameAttribute = FirstNameAttribute;
FirstNameAttribute.NAME = 'FirstName';
FirstNameAttribute.ALIASES = ['FirstName', 'GivenName'];
/** Pattern to match and remove common titles and title abbreviations from first names */
FirstNameAttribute.TITLE_PATTERN = /^(mr|mrs|ms|miss|dr|prof|capt|sir|col|gen|cmdr|lt|rabbi|father|brother|sister|hon|honorable|reverend|rev|doctor)\.?\s+/i;
/** Pattern to match trailing periods and middle initials in names */
FirstNameAttribute.TRAILING_PERIOD_AND_INITIAL_PATTERN = /\s[^\s]\.?$/;
//# sourceMappingURL=FirstNameAttribute.js.map