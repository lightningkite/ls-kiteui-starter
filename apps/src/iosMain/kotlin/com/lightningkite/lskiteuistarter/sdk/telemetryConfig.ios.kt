package com.lightningkite.lskiteuistarter.sdk

import com.lightningkite.kiteui.telemetry.TelemetryConfig

// Replace with your Grafana OTLP endpoint + base64 token to enable production monitoring.
// TelemetryConfig(endpoint = "https://otlp-gateway-prod-us-central-0.grafana.net/otlp", headers = mapOf("Authorization" to "Basic <base64-token>"))
actual fun getDefaultTelemetryConfig(): TelemetryConfig? = null
