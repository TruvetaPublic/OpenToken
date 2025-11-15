/**
 * Copyright (c) Truveta. All rights reserved.
 */
/**
 * Utility functions for attribute processing including normalizing accents,
 * standardizing formats, and other attribute-related transformations.
 */
export declare class AttributeUtilities {
    /** Pattern that matches any character that is not an alphabetic character (a-z or A-Z) */
    static readonly NON_ALPHABETIC_PATTERN: RegExp;
    /** Pattern that matches one or more whitespace characters */
    static readonly WHITESPACE: RegExp;
    /** Pattern that matches generational suffixes at the end of a string */
    static readonly GENERATIONAL_SUFFIX_PATTERN: RegExp;
    /**
     * A set of common placeholder names used to identify non-identifying or
     * placeholder text in data fields.
     */
    static readonly COMMON_PLACEHOLDER_NAMES: Set<string>;
    /**
     * Removes diacritic marks from the given string.
     *
     * This method performs the following steps:
     * 1. Trims the input string
     * 2. Normalizes the string using NFD form, which separates characters from their diacritical marks
     * 3. Removes all diacritical marks
     *
     * @param value - The string from which to remove diacritical marks
     * @returns A new string with all diacritical marks removed
     */
    static normalizeDiacritics(value: string): string;
}
//# sourceMappingURL=AttributeUtilities.d.ts.map