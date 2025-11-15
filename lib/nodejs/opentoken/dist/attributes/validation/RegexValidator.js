"use strict";
/**
 * Copyright (c) Truveta. All rights reserved.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.RegexValidator = void 0;
/**
 * A validator that validates a value against a regular expression.
 */
class RegexValidator {
    /**
     * Constructs a new RegexValidator.
     *
     * @param pattern - The regular expression pattern to validate against.
     */
    constructor(pattern) {
        this.pattern = typeof pattern === 'string' ? new RegExp(pattern) : pattern;
    }
    /**
     * Validates the given value against the regular expression.
     *
     * @param value - The value to validate.
     * @returns true if the value matches the pattern; false otherwise.
     */
    validate(value) {
        if (!value) {
            return false;
        }
        return this.pattern.test(value);
    }
}
exports.RegexValidator = RegexValidator;
//# sourceMappingURL=RegexValidator.js.map