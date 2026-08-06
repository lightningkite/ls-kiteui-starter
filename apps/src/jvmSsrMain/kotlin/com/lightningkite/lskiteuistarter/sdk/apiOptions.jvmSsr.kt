package com.lightningkite.lskiteuistarter.sdk

/**
 * SSR tests override [apiOverride] directly; Local is the safe default to prevent
 * accidental hits against a real backend if something is misconfigured.
 */
actual fun getDefaultServerBackend(): ApiOption = ApiOption.Local
