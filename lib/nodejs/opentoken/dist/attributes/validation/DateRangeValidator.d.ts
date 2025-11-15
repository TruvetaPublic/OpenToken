/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { Validator } from './Validator';
/**
 * A validator that asserts that a date value is within a configurable date range.
 *
 * This validator can be configured with:
 * - Minimum date (inclusive) - can be null for no lower bound
 * - Maximum date (inclusive) - can be null for no upper bound
 * - Use current date as maximum - dynamically sets max to today
 */
export declare class DateRangeValidator implements Validator {
    private minDate;
    private maxDate;
    private useCurrentDateAsMax;
    /**
     * Creates a DateRangeValidator with specified minimum and maximum dates.
     *
     * @param minDate - The minimum allowed date (inclusive), or null for no lower bound
     * @param maxDate - The maximum allowed date (inclusive), or null for no upper bound
     * @param useCurrentDateAsMax - If true, uses current date as maximum
     */
    constructor(minDate?: Date | null, maxDate?: Date | null, useCurrentDateAsMax?: boolean);
    /**
     * Validates that the date value is within the configured range.
     *
     * @param value - The date string to validate
     * @returns true if the date is within the configured range, false otherwise
     */
    validate(value: string): boolean;
    /**
     * Parses a date string in various supported formats.
     *
     * @param value - The date string to parse
     * @returns The parsed Date object, or null if parsing fails
     */
    private parseDate;
}
//# sourceMappingURL=DateRangeValidator.d.ts.map