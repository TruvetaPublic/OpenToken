/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { BaseAttribute } from '../BaseAttribute';
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
export declare class LastNameAttribute extends BaseAttribute {
    private static readonly NAME;
    private static readonly ALIASES;
    constructor();
    validate(value: string): boolean;
    normalize(value: string): string;
}
//# sourceMappingURL=LastNameAttribute.d.ts.map