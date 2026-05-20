package com.heroscript.utils

/**
 * Filters KiteUI's debug-level console output on JS.
 *
 * KiteUI (v8) has no on/off switch for its built-in `Log` -> `console.log` plumbing
 * (see `com.lightningkite.kiteui.debugger.js.kt`'s `PlatformLog`). Several internal
 * loggers — most notably `ScreenStack.bindToPlatform` — emit dozens of messages per
 * page load and per navigation. In browser-test sessions this destabilizes the
 * Chrome MCP extension and slows the harness ~4x.
 *
 * This installs a thin wrapper around `console.log` / `console.info` that drops
 * messages whose first argument matches a known KiteUI internal tag prefix.
 * `console.warn`, `console.error`, and any non-kiteui caller are passed through
 * unchanged.
 *
 * Set `window.heroscriptVerboseLogs = true` in DevTools before reload to disable
 * the filter for a session.
 *
 * Upstream FR tracked in FEEDBACK.md — when kiteui exposes a level switch,
 * delete this file and call the upstream API instead.
 */
fun installKiteUiLogFilter() {
    js(
        """
        (function() {
            if (typeof window === 'undefined') return;
            if (window.__heroscriptLogFilterInstalled) return;
            if (window.heroscriptVerboseLogs) return;
            window.__heroscriptLogFilterInstalled = true;

            // Tags emitted by kiteui internals that flood the console under normal use.
            // Matched as a prefix against the first argument to console.log/info.
            var noisyPrefixes = [
                'ScreenStack.bindToPlatform',
                'WS to ',
                '[KiteUI Hydration]',
                '[KiteUI]',
                'currentSession.token',
                'ElementLeaks',
                'viewDebugTarget',
                'Recycler2',
                'anim',
                'navigatorView DEBUG',
                'Root adding element',
                'Setting theme base',
                'Base path is',
                'WARN: placeholder',
                'WARN: cancelButton'
            ];

            // Substrings to match anywhere in the first argument. Use for the
            // recurring 'attempted to calculate applied padding before fully
            // started' kiteui lifecycle noise, which prefixes vary on (placeholder,
            // cancelButton, anonymous view name, etc.).
            var noisySubstrings = [
                'attempted to calculate applied padding before fully started'
            ];

            function shouldDrop(args) {
                if (!args || args.length === 0) return false;
                var first = args[0];
                if (typeof first !== 'string') return false;
                for (var i = 0; i < noisyPrefixes.length; i++) {
                    if (first.indexOf(noisyPrefixes[i]) === 0) return true;
                }
                for (var j = 0; j < noisySubstrings.length; j++) {
                    if (first.indexOf(noisySubstrings[j]) !== -1) return true;
                }
                return false;
            }

            var origLog = console.log.bind(console);
            var origInfo = console.info.bind(console);
            console.log = function() {
                if (shouldDrop(arguments)) return;
                origLog.apply(console, arguments);
            };
            console.info = function() {
                if (shouldDrop(arguments)) return;
                origInfo.apply(console, arguments);
            };
        })();
        """
    )
}
