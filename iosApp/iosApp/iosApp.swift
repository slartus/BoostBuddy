
import SwiftUI
import BoostBuddyShared

@main
struct iOSApp: App {
    init() {
        NapierProxyKt.debugLogBuild()
        PlatformDataConfiguration().createDependenciesTree(
          platformConfiguration: PlatformConfiguration.companion.shared,
          analyticsTrackers: []
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let root = RootComponentImpl(
            componentContext: DefaultComponentContext(lifecycle: ApplicationLifecycle())
        )
        return MainKt.MainViewController(rootComponent: root)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}
