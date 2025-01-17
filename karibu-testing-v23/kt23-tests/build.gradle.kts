dependencies {
    api(project(":karibu-testing-v10:kt10-tests")) {
        exclude(group = "com.vaadin")
    }
    api("com.github.mvysny.karibudsl:karibu-dsl-v23:${properties["karibudsl11_version"]}")
    api(project(":karibu-testing-v23"))
    api("com.vaadin:vaadin-spring:12.2.0")
    compileOnly("com.vaadin:vaadin:${properties["vaadin23_next_version"]}")
}
