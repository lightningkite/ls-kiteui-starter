//
//  AppDelegate.swift
//  Created by Joseph Ivie on 1/3/24.
//

import UIKit
import apps


@main
class AppDelegate: KiteUiAppDelegate, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    var _mainNavigator: PageNavigator = PageNavigator(routesGetter: { AutoroutesKt.AutoRoutes })
    let _dialogNavigator: PageNavigator = PageNavigator(routesGetter: { AutoroutesKt.AutoRoutes })

    override var mainNavigator: PageNavigator { _mainNavigator }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        DataKt.remMultiplier = 1.23
        return true
    }
 }
