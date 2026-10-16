plugins {
    base
}

tasks.named("build") {
    dependsOn(":neoforge:build")
}
