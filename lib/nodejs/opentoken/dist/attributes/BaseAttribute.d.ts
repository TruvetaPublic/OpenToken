/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { Attribute } from './Attribute';
import { Validator } from './validation/Validator';
/**
 * Base class for all attributes.
 */
export declare abstract class BaseAttribute implements Attribute {
    protected name: string;
    protected aliases: string[];
    protected validators: Validator[];
    /**
     * Constructs a new BaseAttribute.
     *
     * @param name - The name of the attribute.
     * @param aliases - The aliases of the attribute.
     * @param validators - The validators for the attribute.
     */
    constructor(name: string, aliases?: string[], validators?: Validator[]);
    getName(): string;
    getAliases(): string[];
    /**
     * Normalizes the attribute value.
     * Default implementation returns the value as-is.
     * Subclasses should override this method to provide custom normalization.
     *
     * @param value - The attribute value.
     * @returns The normalized attribute value.
     */
    normalize(value: string): string;
    /**
     * Validates the attribute value using all configured validators.
     *
     * @param value - The attribute value.
     * @returns true if the value passes all validators; false otherwise.
     */
    validate(value: string): boolean;
}
//# sourceMappingURL=BaseAttribute.d.ts.map