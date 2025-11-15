/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { BaseAttribute } from '../BaseAttribute';
/**
 * Represents an assigned sex of a person.
 *
 * This class extends BaseAttribute and provides functionality for working with
 * these type of fields. It recognizes "Sex" or "Gender" as valid aliases for
 * this attribute type.
 *
 * The attribute performs normalization on input values, converting them to a
 * standard format (Male or Female).
 */
export declare class SexAttribute extends BaseAttribute {
    private static readonly NAME;
    private static readonly ALIASES;
    /**
     * Regular expression pattern for validating sex/gender values.
     *
     * This pattern matches the following formats (case-insensitive):
     * - "M" or "F"
     * - "Male" or "Female"
     */
    private static readonly VALIDATE_REGEX;
    constructor();
    normalize(value: string): string;
}
//# sourceMappingURL=SexAttribute.d.ts.map