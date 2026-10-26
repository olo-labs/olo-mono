pluginManagement {
    repositories { maven { url = uri("../../../olo-toolgate/build/sdk-repo") }; gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { maven { url = uri("../../../olo-toolgate/build/sdk-repo") }; maven { url = uri("../../../olo-toolgate/.dev/maven") }; mavenCentral() }
}
rootProject.name = "olo-knowledge-packages"
include("memory", "rag")
