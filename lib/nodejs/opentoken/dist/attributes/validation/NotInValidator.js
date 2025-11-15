"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.NotInValidator = void 0;
/**
 * A validator that checks if a value is NOT in a given set of invalid values.
 */
class NotInValidator {
    /**
     * Constructs a new NotInValidator.
     *
     * @param invalidValues - Set of values that are considered invalid
     */
    constructor(invalidValues) {
        this.invalidValues =
            invalidValues instanceof Set ? invalidValues : new Set(invalidValues);
    }
    /**
     * Validates that the given value is NOT in the set of invalid values.
     *
     * @param value - The value to validate.
     * @returns true if the value is NOT in the invalid values set; false otherwise.
     */
    validate(value) {
        if (!value) {
            return false;
        }
        return !this.invalidValues.has(value);
    }
}
exports.NotInValidator = NotInValidator;
//# sourceMappingURL=NotInValidator.js.map