package com.lightningkite.lskiteuistarter.sdk

import com.lightningkite.kiteui.telemetry.TelemetryConfig

/**
 * Returns the OTLP telemetry endpoint configuration for this platform, or null to disable telemetry.
 *
 * Override the platform actuals with your Grafana Cloud (or other OTLP) credentials to enable
 * production observability. When non-null, the app automatically instruments HTTP requests,
 * page navigation, app lifecycle, connectivity issues, and exceptions.
 */
expect fun getDefaultTelemetryConfig(): TelemetryConfig?
