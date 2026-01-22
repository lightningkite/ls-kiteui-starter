import { defineConfig } from 'vite'

export default defineConfig({
    root: "kotlin",
    server: {
        host: true,
        port: 8941,
        allowedHosts: ["*"],
        proxy: {
            '/api': {
                target: 'http://localhost:8081',
                // changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, ''),
                ws: true,
            }
        }
    },
})