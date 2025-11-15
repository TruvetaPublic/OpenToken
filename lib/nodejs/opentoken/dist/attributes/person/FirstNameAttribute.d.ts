/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { BaseAttribute } from '../BaseAttribute';
/**
 * Represents the first name of a person.
 *
 * This class extends BaseAttribute and provides functionality for working with
 * first name fields. It recognizes "FirstName" and "GivenName" as valid aliases
 * for this attribute type.
 */
export declare class FirstNameAttribute extends BaseAttribute {
    private static readonly NAME;
    private static readonly ALIASES;
    /** Pattern to match and remove common titles and title abbreviations from first names */
    private static readonly TITLE_PATTERN;
    /** Pattern to match trailing periods and middle initials in names */
    private static readonly TRAILING_PERIOD_AND_INITIAL_PATTERN;
    constructor();
    normalize(value: string): string;
}
//# sourceMappingURL=FirstNameAttribute.d.ts.map