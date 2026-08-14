// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "SharedBridge",
    platforms: [
        .iOS(.v15),
    ],
    products: [
        .library(
            name: "SharedBridge",
            targets: ["SharedBridge"],
        ),
    ],
    targets: [
        .target(name: "SharedBridge"),
    ],
)
