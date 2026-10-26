plugins { id("io.ololabs.toolgate.sdk") }
toolgate { descriptorGeneration.set(1) }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
