// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_shared",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_shared",
      type: .none,
      targets: ["_shared"]
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
      name: "_shared",
      dependencies: [
        .product(
          name: "FirebaseAnalytics",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseCrashlytics",
          package: "firebase-ios-sdk"
        )
      ]
    )
  ]
)
