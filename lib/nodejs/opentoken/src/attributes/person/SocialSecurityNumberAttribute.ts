/**
 * Copyright (c) Truveta. All rights reserved.
 */

import { BaseAttribute } from '../BaseAttribute';
import { NotInValidator } from '../validation/NotInValidator';
import { RegexValidator } from '../validation/RegexValidator';
import { AttributeUtilities } from '../utilities/AttributeUtilities';

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
export class SocialSecurityNumberAttribute extends BaseAttribute {
  private static readonly NAME = 'SocialSecurityNumber';
  private static readonly ALIASES = ['SocialSecurityNumber', 'SSN'];
  private static readonly MIN_SSN_LENGTH = 7;
  private static readonly SSN_LENGTH = 9;

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
  private static readonly SSN_REGEX =
    /^(?:\d{7,9}(\.0*)?)$|^(?!000|666|9\d\d)(\d{3})-?(?!00)(\d{2})-?(?!0000)(\d{4})$/;

  private static readonly DIGITS_ONLY_PATTERN = /^\d+$/;

  private static readonly INVALID_SSNS = new Set([
    // All zeros is invalid
    '000000000',
    // Repeating patterns
    '111111111',
    '222222222',
    '333333333',
    '444444444',
    '555555555',
    '777777777',
    '888888888',
    // Common placeholder SSNs (normalized without dashes)
    '009999999',
    '010101010',
    '087654321',
    '098765432',
    '099999999',
    '111223333',
    '121212121',
    '123459999',
  ]);

  constructor() {
    super(SocialSecurityNumberAttribute.NAME, SocialSecurityNumberAttribute.ALIASES, [
      new NotInValidator(SocialSecurityNumberAttribute.INVALID_SSNS),
      new RegexValidator(SocialSecurityNumberAttribute.SSN_REGEX),
    ]);
  }

  /**
   * Validates the social security number value.
   * This method overrides the validate method from BaseAttribute
   * to ensure that the value is normalized before validation.
   */
  validate(value: string): boolean {
    return super.validate(this.normalize(value)); // Validate normalized SSN
  }

  /**
   * Normalize the social security number value. Remove any dashes and return
   * the value as a 9-digit number without dashes. If not possible return the
   * original but trimmed value.
   *
   * @param originalValue - The social security number value.
   * @returns The normalized SSN as 9 digits without dashes (e.g., "123456789").
   */
  normalize(originalValue: string): string {
    if (!originalValue || originalValue.length === 0) {
      return originalValue;
    }

    // Remove any whitespace
    let trimmedValue = originalValue.trim().replace(AttributeUtilities.WHITESPACE, '');

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

    if (
      normalizedValue.length < SocialSecurityNumberAttribute.MIN_SSN_LENGTH ||
      normalizedValue.length > SocialSecurityNumberAttribute.SSN_LENGTH
    ) {
      return originalValue; // Invalid length for SSN
    }

    normalizedValue = this.padWithZeros(normalizedValue);

    // Return without dashes (just 9 digits) to match Java/Python behavior
    return normalizedValue;
  }

  /**
   * If SSN is between 7-8 digits, pad with leading zeros to reach 9 digits.
   *
   * Examples:
   * - "1234567" -> "001234567"
   * - "12345678" -> "012345678"
   */
  private padWithZeros(ssn: string): string {
    if (
      ssn.length >= SocialSecurityNumberAttribute.MIN_SSN_LENGTH &&
      ssn.length < SocialSecurityNumberAttribute.SSN_LENGTH
    ) {
      ssn = ssn.padStart(SocialSecurityNumberAttribute.SSN_LENGTH, '0');
    }
    return ssn;
  }
}
