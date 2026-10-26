/// <reference types="vitest" />
/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({
    plugins: [react()],
    test: {
        environment: 'jsdom',
        globals: true,
        include: ['src/**/*.test.{ts,tsx}'],
    },
    server: {
        port: 3000,
        proxy: {
            '/api': {
                target: 'http://localhost:7080',
                changeOrigin: true,
            },
            '/ws': {
                target: 'http://localhost:7080',
                ws: true,
                changeOrigin: true,
            },
        },
    },
});
