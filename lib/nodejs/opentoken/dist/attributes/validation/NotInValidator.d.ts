/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { Validator } from './Validator';
/**
 * A validator that checks if a value is NOT in a given set of invalid values.
 */
export declare class NotInValidator implements Validator {
    private invalidValues;
    /**
     * Constructs a new NotInValidator.
     *
     * @param invalidValues - Set of values that are considered invalid
     */
    constructor(invalidValues: Set<string> | string[]);
    /**
     * Validates that the given value is NOT in the set of invalid values.
     *
     * @param value - The value to validate.
     * @returns true if the value is NOT in the invalid values set; false otherwise.
     */
    validate(value: string): boolean;
}
//# sourceMappingURL=NotInValidator.d.ts.map