/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { BaseAttribute } from '../BaseAttribute';
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
export declare class SocialSecurityNumberAttribute extends BaseAttribute {
    private static readonly NAME;
    private static readonly ALIASES;
    private static readonly DASH;
    private static readonly MIN_SSN_LENGTH;
    private static readonly SSN_LENGTH;
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
    private static readonly SSN_REGEX;
    private static readonly DIGITS_ONLY_PATTERN;
    private static readonly INVALID_SSNS;
    constructor();
    /**
     * Validates the social security number value.
     * This method overrides the validate method from BaseAttribute
     * to ensure that the value is normalized before validation.
     */
    validate(value: string): boolean;
    /**
     * Normalize the social security number value. Remove any dashes and format the
     * value as xxx-xx-xxxx. If not possible return the original but trimmed value.
     *
     * @param originalValue - The social security number value.
     * @returns The normalized SSN in xxx-xx-xxxx format.
     */
    normalize(originalValue: string): string;
    /**
     * If SSN is between 7-8 digits, pad with leading zeros to reach 9 digits.
     *
     * Examples:
     * - "1234567" -> "001234567"
     * - "12345678" -> "012345678"
     */
    private padWithZeros;
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
    private formatWithDashes;
}
//# sourceMappingURL=SocialSecurityNumberAttribute.d.ts.map