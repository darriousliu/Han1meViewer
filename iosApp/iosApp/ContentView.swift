import ComposeApp
import SwiftUI

struct ContentView: View {
    var body: some View {
        VStack(spacing: 12) {
            Text("Han1meViewer")
                .font(.title)
            Text(IosBootstrap.shared.status())
                .foregroundStyle(.secondary)
        }
        .padding()
    }
}
