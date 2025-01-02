package com.lightningkite.template.utils

import com.lightningkite.kiteui.suspendCoroutineCancellable
import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.template.fcmToken
import com.lightningkite.template.utils.*
import kotlinx.browser.window
import org.w3c.notifications.*
import org.w3c.workers.*
import kotlin.coroutines.resume

actual fun fcmSetup(): Unit {
    try {
        val firebaseAppOptions: dynamic = object {}
        firebaseAppOptions["apiKey"] = "REPLACE ME"
        firebaseAppOptions["authDomain"] = "REPLACE ME"
        firebaseAppOptions["projectId"] = "REPLACE ME"
        firebaseAppOptions["storageBucket"] = "REPLACE ME"
        firebaseAppOptions["messagingSenderId"] = "REPLACE ME"
        firebaseAppOptions["appId"] = "REPLACE ME"

        val app = initializeApp(firebaseAppOptions.unsafeCast<FirebaseOptions>())
        val messaging = getMessaging(app)

        window.navigator.serviceWorker
            .register("firebase-messaging-sw.js", RegistrationOptions(scope = "./"))
            .then { serviceWorkerReg ->

                val serviceWorker = serviceWorkerReg.installing ?: serviceWorkerReg.waiting ?: serviceWorkerReg.active
                ConsoleRoot.log("Service worker state: ${serviceWorker?.state}")
                if(serviceWorker?.state == ServiceWorkerState.ACTIVATED) {
                    finishSetup(serviceWorkerReg, messaging)
                } else {
                    serviceWorker?.addEventListener("statechange", { e ->
                        val target = e.target as ServiceWorker
                        ConsoleRoot.log("Service worker state: ${target.state}")
                        if(target.state == ServiceWorkerState.ACTIVATED)
                            finishSetup(serviceWorkerReg, messaging)
                    })
                }


            }.catch {
                it.printStackTrace()
            }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}


private fun finishSetup(
    serviceWorker: ServiceWorkerRegistration,
    messaging: Messaging
) {
    val messagingOptions: dynamic = object {}
    messagingOptions["vapidKey"] =
        "REPLACE ME"
    messagingOptions["serviceWorkerRegistration"] = serviceWorker

    getToken(
        messaging,
        messagingOptions.unsafeCast<GetTokenOptions>()
    ).then {
        fcmToken.value = it
    }.catch {
        it.printStackTrace()
    }

    onMessage(messaging) { notification ->
        serviceWorker.showNotification(
            notification.notification?.title ?: "Notification",
            NotificationOptions(
                body = notification.notification?.body,
                image = notification.notification?.image,
                icon = notification.notification?.icon,
            )
        )
            .catch { e ->
                console.error("Error sending notificaiton to user", e);
            }
        serviceWorker.update()
    }
    ConsoleRoot.info("FCM Registration complete.")
}

actual suspend fun requestNotificationPermissions(): Unit {
    val result = suspendCoroutineCancellable { cont ->
        Notification.requestPermission { result ->
            cont.resume(result)
        }
        return@suspendCoroutineCancellable {}
    }
    if (result == NotificationPermission.GRANTED) fcmSetup()
}

actual suspend fun notificationPermissions(): Boolean? {
    return if (js("'Notification' in window").unsafeCast<Boolean>())
        when (Notification.permission) {
            NotificationPermission.GRANTED -> true
            NotificationPermission.DENIED -> false
            else -> null
        } else false
}
