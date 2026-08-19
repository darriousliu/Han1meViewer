// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_core_firebase",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_core_firebase",
      type: .none,
      targets: ["_core_firebase"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      exact: "12.17.0"
    )
  ],
  targets: [
    .target(
      name: "_core_firebase",
      dependencies: [
        .product(
          name: "FirebaseAnalytics",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseCrashlytics",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseRemoteConfig",
          package: "firebase-ios-sdk"
        )
      ]
    )
  ]
)
