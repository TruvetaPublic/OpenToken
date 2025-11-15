/**
 * Copyright (c) Truveta. All rights reserved.
 */
/**
 * An attribute expression determines how the value of an attribute is normalized for consumption.
 *
 * Components used to compose the attribute expression:
 * - T - trim
 * - U - convert to upper case
 * - S(start_index, end_index) - substring of value
 * - D - treat as date
 * - M(regex) - match the regular expression
 * - | - expression separator
 *
 * Examples:
 * - T|S(0,3)|U - trim the value, then take first 3 characters, and then convert to upper case
 * - T|D - trim the value, treat the value as a date in the yyyy-MM-dd format
 * - T|M("\\d+") - trim the value, then make sure that the value matches the regular expression
 */
export declare class AttributeExpression {
    private static readonly EXPRESSION_PATTERN;
    private attributeClass;
    private expressions;
    constructor(attributeClass: any, expressions: string);
    /**
     * Get the effective value for an attribute after application of the attribute expression.
     *
     * @param value - The attribute value
     * @returns The effective value after applying the attribute expression
     */
    getEffectiveValue(value: string): string;
    private eval;
    /**
     * Substring expression S(start,end)
     */
    private substringExpression;
    /**
     * Replace expression R(oldString,newString)
     */
    private replaceExpression;
    /**
     * RegEx match M(regex)
     */
    private matchExpression;
    /**
     * Date expression - returns date in yyyy-MM-dd format
     */
    private dateExpression;
    getAttributeClass(): any;
    getExpressions(): string;
}
//# sourceMappingURL=AttributeExpression.d.ts.map