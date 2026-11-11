package com.lightningkite.lskiteuistarter.app.sdk

/**
 * SSR tests override [com.lightningkite.lskiteuistarter.sdk.apiOverride] directly; Local is the safe default to prevent
 * accidental hits against a real backend if something is misconfigured.
 */
actual fun getDefaultServerBackend(): ApiOption = ApiOption.Local
