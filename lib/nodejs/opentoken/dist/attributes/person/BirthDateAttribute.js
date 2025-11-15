"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.BirthDateAttribute = void 0;
const DateAttribute_1 = require("../general/DateAttribute");
const DateRangeValidator_1 = require("../validation/DateRangeValidator");
/**
 * Represents the birth date attribute.
 *
 * This class extends DateAttribute and provides functionality for working with
 * birth date fields. It recognizes "BirthDate" and "DateOfBirth" as valid aliases
 * for this attribute type.
 *
 * The attribute performs normalization on input values, converting them to a
 * standard format (yyyy-MM-dd) (inherited from DateAttribute).
 *
 * The attribute also performs validation on input values, ensuring they:
 * - Match one of the supported date formats (inherited from DateAttribute)
 * - Fall within an acceptable range for birth dates (1910-01-01 to today)
 */
class BirthDateAttribute extends DateAttribute_1.DateAttribute {
    constructor() {
        const minDate = new Date('1910-01-01');
        super(BirthDateAttribute.NAME, BirthDateAttribute.ALIASES, [
            new DateRangeValidator_1.DateRangeValidator(minDate, null, true),
        ]);
    }
}
exports.BirthDateAttribute = BirthDateAttribute;
BirthDateAttribute.NAME = 'BirthDate';
BirthDateAttribute.ALIASES = ['BirthDate', 'DateOfBirth'];
//# sourceMappingURL=BirthDateAttribute.js.map