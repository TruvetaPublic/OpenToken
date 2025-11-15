/**
 * Copyright (c) Truveta. All rights reserved.
 */
import { SerializableAttribute } from '../SerializableAttribute';
import { Validator } from '../validation/Validator';
/**
 * Base class for date-based attributes.
 */
export declare class DateAttribute extends SerializableAttribute {
    /**
     * Constructs a new DateAttribute.
     *
     * @param name - The name of the attribute.
     * @param aliases - The aliases of the attribute.
     * @param validators - The validators for the attribute.
     */
    constructor(name: string, aliases?: string[], validators?: Validator[]);
    /**
     * Normalizes a date string to yyyy-MM-dd format.
     *
     * @param value - The date string to normalize.
     * @returns The normalized date string in yyyy-MM-dd format.
     */
    normalize(value: string): string;
}
//# sourceMappingURL=DateAttribute.d.ts.map