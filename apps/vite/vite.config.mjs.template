import { defineConfig } from 'vite'

// by Claude — read ports from environment variables for testing flexibility
const frontendPort = parseInt(process.env.FRONTEND_PORT || '8941')
const backendPort = parseInt(process.env.BACKEND_PORT || '8081')

export default defineConfig({
    root: "kotlin",
    server: {
        host: true,
        port: frontendPort,
        allowedHosts: ["*"],
        proxy: {
            '/api': {
                target: `http://localhost:${backendPort}`,
                // changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, ''),
                ws: true,
            }
        }
    },
})
