//
//  AppDelegate.swift
//  iosApp
//
//  Created by Artem Slinkin on 10.03.2024.
//

import Foundation
import SwiftUI
import BoostBuddyShared

class AppDelegate: NSObject, UIApplicationDelegate {
    let root: RootComponent = RootComponentImpl(
        componentContext: DefaultComponentContext(lifecycle: ApplicationLifecycle())
    )
}
