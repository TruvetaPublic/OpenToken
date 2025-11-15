/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { Validator } from './Validator';
/**
 * A validator that validates a value against a regular expression.
 */
export declare class RegexValidator implements Validator {
    private pattern;
    /**
     * Constructs a new RegexValidator.
     *
     * @param pattern - The regular expression pattern to validate against.
     */
    constructor(pattern: string | RegExp);
    /**
     * Validates the given value against the regular expression.
     *
     * @param value - The value to validate.
     * @returns true if the value matches the pattern; false otherwise.
     */
    validate(value: string): boolean;
}
//# sourceMappingURL=RegexValidator.d.ts.map