import { defineConfig } from 'vite'
import svgr from 'vite-plugin-svgr';
import react from '@vitejs/plugin-react'
import vitePluginCssInjectedByJs from 'vite-plugin-css-injected-by-js'

export default defineConfig({
  plugins: [
    react(),
    vitePluginCssInjectedByJs(),
    svgr(),
  ],
  define: {
    'process.env.NODE_ENV': '"production"',
    'process.env': '{}',
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  build: {
    lib: {
      entry: 'src/main.tsx',
      formats: ['iife'],
      name: 'DskChatWidget',
      fileName: () => 'widget.js',
    },
    assetsInlineLimit: 100000000,
    cssCodeSplit: false,
  },
})