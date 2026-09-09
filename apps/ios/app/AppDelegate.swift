//
//  AppDelegate.swift
//  Created by Joseph Ivie on 1/3/24.
//

import UIKit
import apps


@main
class AppDelegate: KiteUiAppDelegate, UIApplicationDelegate, UNUserNotificationCenterDelegate {
   var window: UIWindow?
   var _mainNavigator:PageNavigator = PageNavigator(routesGetter: { AutoroutesKt.AutoRoutes })
   let _dialogNavigator: PageNavigator = PageNavigator(routesGetter: { AutoroutesKt.AutoRoutes })

   override var mainNavigator: PageNavigator { _mainNavigator }

   func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
       // Override point for customization after application launch.
       DataKt.remMultiplier = 1.23

       let newWindow = UIWindow()
       let vc = UIViewController()
       newWindow.rootViewController = vc
       window = newWindow
       newWindow.makeKeyAndVisible()

       App_iosKt.root(viewController: vc, mainNav: _mainNavigator, dialogNav: _dialogNavigator)

       return true
   }


   func application(
       _ application: UIApplication,
       continue userActivity: NSUserActivity,
       restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void
   ) ->Bool {
       guard userActivity.activityType == NSUserActivityTypeBrowsingWeb, let incomingURL = userActivity.webpageURL else {
           return false
       }
       openURL(url: incomingURL.absoluteString, options: [:])

       return false
   }
}


