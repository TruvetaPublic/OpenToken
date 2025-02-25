/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import lombok.Getter;

/**
 * Represents the result of a token generation operation.
 * <p>
 * This class contains the generated tokens and the list of invalid attributes.
 * </p>
 */
@Getter
public class TokenGeneratorResult {

    private Map<String, String> tokens = new TreeMap<>();

    private Set<String> invalidAttributes = new HashSet<>();
}
