"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.SocialSecurityNumberAttribute = void 0;
const BaseAttribute_1 = require("../BaseAttribute");
const NotInValidator_1 = require("../validation/NotInValidator");
const RegexValidator_1 = require("../validation/RegexValidator");
const AttributeUtilities_1 = require("../utilities/AttributeUtilities");
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
class SocialSecurityNumberAttribute extends BaseAttribute_1.BaseAttribute {
    constructor() {
        super(SocialSecurityNumberAttribute.NAME, SocialSecurityNumberAttribute.ALIASES, [
            new NotInValidator_1.NotInValidator(SocialSecurityNumberAttribute.INVALID_SSNS),
            new RegexValidator_1.RegexValidator(SocialSecurityNumberAttribute.SSN_REGEX),
        ]);
    }
    /**
     * Validates the social security number value.
     * This method overrides the validate method from BaseAttribute
     * to ensure that the value is normalized before validation.
     */
    validate(value) {
        return super.validate(this.normalize(value)); // Validate normalized SSN
    }
    /**
     * Normalize the social security number value. Remove any dashes and format the
     * value as xxx-xx-xxxx. If not possible return the original but trimmed value.
     *
     * @param originalValue - The social security number value.
     * @returns The normalized SSN in xxx-xx-xxxx format.
     */
    normalize(originalValue) {
        if (!originalValue || originalValue.length === 0) {
            return originalValue;
        }
        // Remove any whitespace
        let trimmedValue = originalValue.trim().replace(AttributeUtilities_1.AttributeUtilities.WHITESPACE, '');
        // Remove any dashes for now
        let normalizedValue = trimmedValue.replace(/-/g, '');
        // Remove decimal point/separator and all following numbers if present
        const decimalIndex = normalizedValue.indexOf('.');
        if (decimalIndex !== -1) {
            normalizedValue = normalizedValue.substring(0, decimalIndex);
        }
        // Check if the string contains only digits
        if (!SocialSecurityNumberAttribute.DIGITS_ONLY_PATTERN.test(normalizedValue)) {
            return originalValue; // Return original value if it contains non-numeric characters
        }
        if (normalizedValue.length < SocialSecurityNumberAttribute.MIN_SSN_LENGTH ||
            normalizedValue.length > SocialSecurityNumberAttribute.SSN_LENGTH) {
            return originalValue; // Invalid length for SSN
        }
        normalizedValue = this.padWithZeros(normalizedValue);
        return this.formatWithDashes(normalizedValue);
    }
    /**
     * If SSN is between 7-8 digits, pad with leading zeros to reach 9 digits.
     *
     * Examples:
     * - "1234567" -> "001234567"
     * - "12345678" -> "012345678"
     */
    padWithZeros(ssn) {
        if (ssn.length >= SocialSecurityNumberAttribute.MIN_SSN_LENGTH &&
            ssn.length < SocialSecurityNumberAttribute.SSN_LENGTH) {
            ssn = ssn.padStart(SocialSecurityNumberAttribute.SSN_LENGTH, '0');
        }
        return ssn;
    }
    /**
     * Takes a 9-digit SSN and adds dashes in the right places.
     *
     * Takes: "123456789"
     * Returns: "123-45-6789"
     *
     * SSN parts:
     * - First 3 digits: Area number
     * - Middle 2 digits: Group number
     * - Last 4 digits: Serial number
     */
    formatWithDashes(value) {
        const areaNumber = value.substring(0, 3);
        const groupNumber = value.substring(3, 5);
        const serialNumber = value.substring(5);
        return `${areaNumber}${SocialSecurityNumberAttribute.DASH}${groupNumber}${SocialSecurityNumberAttribute.DASH}${serialNumber}`;
    }
}
exports.SocialSecurityNumberAttribute = SocialSecurityNumberAttribute;
SocialSecurityNumberAttribute.NAME = 'SocialSecurityNumber';
SocialSecurityNumberAttribute.ALIASES = ['SocialSecurityNumber', 'NationalIdentificationNumber'];
SocialSecurityNumberAttribute.DASH = '-';
SocialSecurityNumberAttribute.MIN_SSN_LENGTH = 7;
SocialSecurityNumberAttribute.SSN_LENGTH = 9;
/**
 * Regular expression to validate Social Security Numbers (SSNs).
 *
 * Allows:
 * - 7 to 9 digits, optionally followed by a decimal separator and zero(s).
 * - Properly formatted SSNs with or without dashes, ensuring:
 *   - The first three digits are not "000", "666", or in the range "900-999".
 *   - The middle two digits are not "00".
 *   - The last four digits are not "0000".
 */
SocialSecurityNumberAttribute.SSN_REGEX = /^(?:\d{7,9}(\.0*)?)$|^(?!000|666|9\d\d)(\d{3})-?(?!00)(\d{2})-?(?!0000)(\d{4})$/;
SocialSecurityNumberAttribute.DIGITS_ONLY_PATTERN = /^\d+$/;
SocialSecurityNumberAttribute.INVALID_SSNS = new Set([
    '111-11-1111',
    '222-22-2222',
    '333-33-3333',
    '444-44-4444',
    '555-55-5555',
    '777-77-7777',
    '888-88-8888',
    // Common placeholder SSNs
    '001-23-4567',
    '009-99-9999',
    '010-10-1010',
    '012-34-5678',
    '087-65-4321',
    '098-76-5432',
    '099-99-9999',
    '111-22-3333',
    '121-21-2121',
    '123-45-6789',
    '123-45-9999',
]);
//# sourceMappingURL=SocialSecurityNumberAttribute.js.map