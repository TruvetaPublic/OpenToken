/**
 * Copyright (c) Truveta. All rights reserved.
 */

import { PersonAttributesReader } from '../io/PersonAttributesReader';
import { PersonAttributesWriter } from '../io/PersonAttributesWriter';
import { TokenTransformer } from '../tokentransformer/TokenTransformer';
import { TokenRegistry } from '../tokens/TokenRegistry';
import { AttributeLoader } from '../attributes/AttributeLoader';
import { Metadata } from '../Metadata';

/**
 * Process all person attributes.
 *
 * This class is used to read person attributes from input source,
 * generate tokens for each person record and write the tokens back
 * to the output data source.
 */
export class PersonAttributesProcessor {
  /**
   * Reads person attributes from the input data source, generates tokens,
   * and writes the result back to the output data source.
   */
  static async process(
    reader: PersonAttributesReader,
    writer: PersonAttributesWriter,
    transformers: TokenTransformer[],
    metadata: Metadata
  ): Promise<void> {
    const tokens = TokenRegistry.loadAll();
    const attributes = AttributeLoader.load();
    let rowCounter = 0;

    // Track invalid attributes and blank tokens
    const invalidAttributeCount = new Map<string, number>();
    const blankTokensByRuleCount = new Map<string, number>();

    while (await reader.hasNext()) {
      const row = await reader.next();
      rowCounter++;

      const rowInvalidAttributes = new Set<string>();
      const rowBlankTokens = new Set<string>();

      // For each token definition, generate a token
      for (const token of tokens) {
        const tokenId = token.getIdentifier();
        const definition = token.getDefinition();

        // Build token signature from attribute expressions
        const signatureParts: string[] = [];
        for (const expr of definition) {
          const exprAttributeClass = expr.getAttributeClass();
          let effectiveValue = '';
          let found = false;

          for (const attr of Array.from(attributes)) {
            if (attr.constructor.name !== exprAttributeClass) continue;

            const attrName = attr.getName();
            let value = row.get(attrName);

            // Also try aliases
            if (!value) {
              for (const alias of attr.getAliases()) {
                value = row.get(alias);
                if (value) break;
              }
            }

            if (value) {
              if (attr.validate(value)) {
                const normalized = attr.normalize(value);
                effectiveValue = expr.getEffectiveValue(normalized);
                found = true;
                break;
              } else {
                // Track invalid attribute
                rowInvalidAttributes.add(attrName);
              }
            }
          }

          if (!found) {
            // Track that this attribute was missing/invalid for this token
            rowBlankTokens.add(tokenId);
          }

          signatureParts.push(effectiveValue);
        }

        const signature = signatureParts.join('|');

        // Check if token is blank (all parts empty)
        const isBlank = signatureParts.every((part) => part === '');
        if (isBlank) {
          rowBlankTokens.add(tokenId);
        }

        // Apply transformers (hash, encrypt)
        let tokenValue = signature;
        for (const transformer of transformers) {
          tokenValue = transformer.transform(tokenValue);
        }

        // Write the token
        const outputRecord = new Map<string, string>();
        outputRecord.set(
          'RecordId',
          row.get('RecordId') || row.get('record-id') || `row-${rowCounter}`
        );
        outputRecord.set('RuleId', tokenId);
        outputRecord.set('Token', tokenValue);

        await writer.writeAttributes(outputRecord);
      }

      // Track invalid attributes
      if (rowInvalidAttributes.size > 0) {
        for (const attrName of rowInvalidAttributes) {
          invalidAttributeCount.set(
            attrName,
            (invalidAttributeCount.get(attrName) || 0) + 1
          );
        }
      }

      // Track blank tokens
      if (rowBlankTokens.size > 0) {
        for (const ruleId of rowBlankTokens) {
          blankTokensByRuleCount.set(
            ruleId,
            (blankTokensByRuleCount.get(ruleId) || 0) + 1
          );
        }
      }

      if (rowCounter % 1000 === 0) {
        console.log(`Processed ${rowCounter} records`);
      }
    }

    // Log invalid attribute statistics
    if (invalidAttributeCount.size > 0) {
      for (const [attrName, count] of invalidAttributeCount.entries()) {
        console.log(
          `Total invalid Attribute count for [${attrName}]: ${count.toLocaleString()}`
        );
      }
      const totalInvalid = Array.from(invalidAttributeCount.values()).reduce(
        (sum, count) => sum + count,
        0
      );
      console.log(
        `Total number of records with invalid attributes: ${totalInvalid.toLocaleString()}`
      );
    }

    // Log blank token statistics
    if (blankTokensByRuleCount.size > 0) {
      for (const [ruleId, count] of blankTokensByRuleCount.entries()) {
        console.log(
          `Total blank tokens for rule [${ruleId}]: ${count.toLocaleString()}`
        );
      }
      const totalBlankTokens = Array.from(
        blankTokensByRuleCount.values()
      ).reduce((sum, count) => sum + count, 0);
      console.log(
        `Total blank tokens generated: ${totalBlankTokens.toLocaleString()}`
      );
    }

    metadata.totalRows = rowCounter;
    console.log(`Processed a total of ${rowCounter} records`);
  }
}
