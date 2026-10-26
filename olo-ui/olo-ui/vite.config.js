/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/// <reference types="vitest" />
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
                target: 'http://localhost:8082',
                changeOrigin: true,
            },
            '/runtime-api': {
                target: 'http://localhost:7080',
                changeOrigin: true,
                rewrite: function (path) { return path.replace(/^\/runtime-api/, '/api'); },
                configure: function (proxy) {
                    proxy.on('proxyRes', function (proxyRes, req) {
                        var _a;
                        if ((_a = req.url) === null || _a === void 0 ? void 0 : _a.includes('/events')) {
                            proxyRes.headers['cache-control'] = 'no-cache';
                            proxyRes.headers['x-accel-buffering'] = 'no';
                        }
                    });
                },
            },
        },
    },
});
