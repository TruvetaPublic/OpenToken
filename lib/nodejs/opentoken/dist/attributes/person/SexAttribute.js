"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.SexAttribute = void 0;
const BaseAttribute_1 = require("../BaseAttribute");
const RegexValidator_1 = require("../validation/RegexValidator");
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
class SexAttribute extends BaseAttribute_1.BaseAttribute {
    constructor() {
        super(SexAttribute.NAME, SexAttribute.ALIASES, [
            new RegexValidator_1.RegexValidator(SexAttribute.VALIDATE_REGEX),
        ]);
    }
    normalize(value) {
        if (!value || value.length === 0) {
            return value;
        }
        const firstChar = value.charAt(0).toUpperCase();
        switch (firstChar) {
            case 'M':
                return 'Male';
            case 'F':
                return 'Female';
            default:
                return value;
        }
    }
}
exports.SexAttribute = SexAttribute;
SexAttribute.NAME = 'Sex';
SexAttribute.ALIASES = ['Sex', 'Gender'];
/**
 * Regular expression pattern for validating sex/gender values.
 *
 * This pattern matches the following formats (case-insensitive):
 * - "M" or "F"
 * - "Male" or "Female"
 */
SexAttribute.VALIDATE_REGEX = /^([Mm](ale)?|[Ff](emale)?)$/;
//# sourceMappingURL=SexAttribute.js.map