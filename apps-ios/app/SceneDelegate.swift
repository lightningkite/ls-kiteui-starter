//
//  SceneDelegate.swift
//  KiteUI Example App
//
//  Created by Joseph Ivie on 12/14/23.
//

import UIKit
import apps

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
   var window: UIWindow?
   private var app: AppDelegate { UIApplication.shared.delegate as! AppDelegate }

   func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
       guard let windowScene = scene as? UIWindowScene else { return }
       let newWindow = UIWindow(windowScene: windowScene)
       let vc = UIViewController()
       newWindow.rootViewController = vc
       window = newWindow
       newWindow.makeKeyAndVisible()
       App_iosKt.root(viewController: vc, mainNav: app.mainNavigator)

       // A universal link that launches the app arrives here instead of in scene(_:continue:).
       connectionOptions.userActivities.forEach { open($0) }
   }

   func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
       open(userActivity)
   }

   private func open(_ userActivity: NSUserActivity) {
       guard userActivity.activityType == NSUserActivityTypeBrowsingWeb, let url = userActivity.webpageURL else { return }
       app.openURL(url: url.absoluteString, options: [:])
   }


}

