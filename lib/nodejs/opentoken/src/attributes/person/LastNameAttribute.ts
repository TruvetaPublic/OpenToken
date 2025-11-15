/**
 * Copyright (c) Truveta. All rights reserved.
 */

import { BaseAttribute } from '../BaseAttribute';
import { NotInValidator } from '../validation/NotInValidator';
import { RegexValidator } from '../validation/RegexValidator';
import { AttributeUtilities } from '../utilities/AttributeUtilities';

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
export class LastNameAttribute extends BaseAttribute {
  private static readonly NAME = 'LastName';
  private static readonly ALIASES = ['LastName', 'Surname'];

  // Pattern kept for reference but validation is done inline in validate method
  // private static readonly LAST_NAME_PATTERN = /^(?:[a-zA-Z]*[aeiouAEIOU][a-zA-Z]*|Ng)$/;

  constructor() {
    super(LastNameAttribute.NAME, LastNameAttribute.ALIASES, [
      new RegexValidator(/.{2,}/), // At least 2 characters
      new NotInValidator(AttributeUtilities.COMMON_PLACEHOLDER_NAMES),
    ]);
  }

  validate(value: string): boolean {
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

  normalize(value: string): string {
    let normalizedValue = AttributeUtilities.normalizeDiacritics(value);

    // Remove generational suffixes
    normalizedValue = normalizedValue.replace(AttributeUtilities.GENERATIONAL_SUFFIX_PATTERN, '');

    // Remove dashes, spaces and other non-alphabetic characters
    normalizedValue = normalizedValue.replace(AttributeUtilities.NON_ALPHABETIC_PATTERN, '');

    return normalizedValue;
  }
}
