plugins {
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    version.set("20.19.0")
    npmVersion.set("10.8.2")
    download.set(true)
}

val npmRunBuild = tasks.register<com.github.gradle.node.npm.task.NpmTask>("npm_run_build") {
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "build"))
    inputs.dir("src")
    inputs.files("package.json", "package-lock.json", "vite.config.js", "tsconfig.json", "tsconfig.node.json")
    outputs.dir("dist")
}

tasks.register("build") {
    dependsOn(npmRunBuild)
    description = "Build the frontend (npm run build via Gradle)"
}

tasks.register<com.github.gradle.node.npm.task.NpmTask>("npm_run_test") {
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "test"))
}

tasks.register<com.github.gradle.node.npm.task.NpmTask>("npm_run_lint") {
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "lint"))
}

tasks.register("test") {
    dependsOn("npm_run_test")
    description = "Run frontend tests"
}

tasks.register("lint") {
    dependsOn("npm_run_lint")
    description = "Run ESLint"
}
