/**
 * Copyright (c) Truveta. All rights reserved.
 */

import { FirstNameAttribute } from '../../../src/attributes/person/FirstNameAttribute';

describe('FirstNameAttribute', () => {
  let firstNameAttribute: FirstNameAttribute;

  beforeEach(() => {
    firstNameAttribute = new FirstNameAttribute();
  });

  test('getName should return FirstName', () => {
    expect(firstNameAttribute.getName()).toBe('FirstName');
  });

  test('getAliases should return FirstName and GivenName', () => {
    const expectedAliases = ['FirstName', 'GivenName'];
    const actualAliases = firstNameAttribute.getAliases();
    expect(actualAliases).toEqual(expectedAliases);
  });

  test('normalize should return unchanged value for basic names', () => {
    const input = 'John';
    expect(firstNameAttribute.normalize(input)).toBe(input);
  });

  test('normalize should remove accents', () => {
    expect(firstNameAttribute.normalize('José')).toBe('JOSE');
    expect(firstNameAttribute.normalize('Vũ')).toBe('VU');
    expect(firstNameAttribute.normalize('François')).toBe('FRANCOIS');
    expect(firstNameAttribute.normalize('Renée')).toBe('RENEE');
  });

  test('normalize should remove titles', () => {
    expect(firstNameAttribute.normalize('Dr. John')).toBe('JOHN');
    expect(firstNameAttribute.normalize('Mr. Smith')).toBe('SMITH');
    expect(firstNameAttribute.normalize('Mrs. Jane')).toBe('JANE');
  });

  test('normalize should handle middle initials', () => {
    expect(firstNameAttribute.normalize('John M')).toBe('JOHN');
    expect(firstNameAttribute.normalize('Mary A.')).toBe('MARY');
  });

  test('normalize should remove suffixes', () => {
    expect(firstNameAttribute.normalize('John Jr')).toBe('JOHN');
    expect(firstNameAttribute.normalize('William III')).toBe('WILLIAM');
  });

  test('normalize should convert to uppercase', () => {
    expect(firstNameAttribute.normalize('john')).toBe('JOHN');
    expect(firstNameAttribute.normalize('Mary')).toBe('MARY');
  });

  test('validate should return true for any non-empty string', () => {
    expect(firstNameAttribute.validate('John')).toBe(true);
    expect(firstNameAttribute.validate('Jane Doe')).toBe(true);
    expect(firstNameAttribute.validate('X')).toBe(true);
  });

  test('validate should return false for empty string', () => {
    expect(firstNameAttribute.validate('')).toBe(false);
  });

  test('validate should return false for null or undefined', () => {
    expect(firstNameAttribute.validate(null as any)).toBe(false);
    expect(firstNameAttribute.validate(undefined as any)).toBe(false);
  });
});
