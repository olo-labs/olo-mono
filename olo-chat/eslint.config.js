/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import js from '@eslint/js'
import tseslint from 'typescript-eslint'

/**
 * Architecture rules:
 * - Stores own side effects; components remain declarative.
 * - No component directly calls API (use store actions).
 * - No store imports another store (domain isolation).
 */
export default [
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['src/components/**/*.tsx', 'src/components/**/*.ts'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['**/api/rest', '../api/rest', '../../api/rest', './api/rest'],
              message:
                'Components must not call API directly. Use a store and types/tenant or lib/tenantDisplay for types and display helpers.',
            },
          ],
        },
      ],
    },
  },
  {
    files: ['src/store/**/*.ts'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: [
                './tenantConfig',
                './runtime',
                './ledger',
                './plugins',
                './schema',
                '../tenantConfig',
                '../runtime',
                '../ledger',
                '../plugins',
                '../schema',
              ],
              message: 'Domain stores must not import other domain stores. Use URL or runContext for cross-domain data.',
            },
          ],
        },
      ],
    },
  },
]
