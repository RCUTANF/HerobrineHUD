tasks.register("runClient") {
    group = "run"
    description = "NeoForge runClient stub (module not configured yet)."
    doLast {
        throw GradleException("NeoForge module is not configured yet. Use Fabric run tasks for now.")
    }
}

tasks.register("runServer") {
    group = "run"
    description = "NeoForge runServer stub (module not configured yet)."
    doLast {
        throw GradleException("NeoForge module is not configured yet. Use Fabric run tasks for now.")
    }
}

