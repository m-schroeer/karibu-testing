dependencies {
    api(project(":karibu-testing-v10"))
    compileOnly("com.vaadin:vaadin-spring:12.2.0")
}

val configureBintray = ext["configureBintray"] as (artifactId: String) -> Unit
configureBintray("karibu-testing-v10-spring")
