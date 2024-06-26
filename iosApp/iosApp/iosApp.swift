
import SwiftUI
import BoostBuddyShared

@main
struct iOSApp: App {
    init() {
        NapierProxyKt.debugLogBuild()
        PlatformDataConfiguration.shared.createDependenciesTree(
            platformConfiguration: PlatformConfiguration.companion.shared
        )
    }

    var body: some Scene {
            WindowGroup {
                ContentView()
            }
        }

}

struct ComposeView: UIViewControllerRepresentable {
    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate: AppDelegate
    func makeUIViewController(context: Context) -> UIViewController {
        MainKt.MainViewController(rootComponent: appDelegate.root)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}
