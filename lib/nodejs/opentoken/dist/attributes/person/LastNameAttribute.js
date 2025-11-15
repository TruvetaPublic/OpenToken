"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.LastNameAttribute = void 0;
const BaseAttribute_1 = require("../BaseAttribute");
const NotInValidator_1 = require("../validation/NotInValidator");
const RegexValidator_1 = require("../validation/RegexValidator");
const AttributeUtilities_1 = require("../utilities/AttributeUtilities");
/**
 * Represents the last name (surname) of a person.
 *
 * This class extends BaseAttribute and provides functionality for working with
 * last name fields. It recognizes "LastName" and "Surname" as valid aliases.
 *
 * The attribute validates that the last name:
 * - Must be at least 2 characters long
 * - For 2-character names, must contain at least one vowel or be "Ng"
 * - Cannot be a common placeholder value
 */
class LastNameAttribute extends BaseAttribute_1.BaseAttribute {
    // Pattern kept for reference but validation is done inline in validate method
    // private static readonly LAST_NAME_PATTERN = /^(?:[a-zA-Z]*[aeiouAEIOU][a-zA-Z]*|Ng)$/;
    constructor() {
        super(LastNameAttribute.NAME, LastNameAttribute.ALIASES, [
            new RegexValidator_1.RegexValidator(/.{2,}/), // At least 2 characters
            new NotInValidator_1.NotInValidator(AttributeUtilities_1.AttributeUtilities.COMMON_PLACEHOLDER_NAMES),
        ]);
    }
    validate(value) {
        if (!super.validate(value)) {
            return false;
        }
        const normalized = this.normalize(value);
        // For 2-character names, must contain at least one vowel or be "Ng"
        if (normalized.length === 2) {
            return /[aeiouAEIOU]/.test(normalized) || normalized === 'Ng';
        }
        return true;
    }
    normalize(value) {
        let normalizedValue = AttributeUtilities_1.AttributeUtilities.normalizeDiacritics(value);
        // Remove generational suffixes
        normalizedValue = normalizedValue.replace(AttributeUtilities_1.AttributeUtilities.GENERATIONAL_SUFFIX_PATTERN, '');
        // Remove dashes, spaces and other non-alphabetic characters
        normalizedValue = normalizedValue.replace(AttributeUtilities_1.AttributeUtilities.NON_ALPHABETIC_PATTERN, '');
        return normalizedValue;
    }
}
exports.LastNameAttribute = LastNameAttribute;
LastNameAttribute.NAME = 'LastName';
LastNameAttribute.ALIASES = ['LastName', 'Surname'];
//# sourceMappingURL=LastNameAttribute.js.map