pluginManagement {
    val source = System.getenv("TOOLGATE_SOURCE") ?: file("../../../olo-toolgate").absolutePath
    repositories { maven { url = uri("$source/build/sdk-repo") }; gradlePluginPortal() }
}
dependencyResolutionManagement {
    val source = System.getenv("TOOLGATE_SOURCE") ?: file("../../../olo-toolgate").absolutePath
    repositories { maven { url = uri("$source/build/sdk-repo") }; maven { url = uri("$source/.dev/maven") }; mavenCentral() }
}
rootProject.name = "olo-knowledge-packages"
include("memory", "rag")
