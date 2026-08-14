import SharedBridge
import SwiftUI
import UIKit

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        makeSharedViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
