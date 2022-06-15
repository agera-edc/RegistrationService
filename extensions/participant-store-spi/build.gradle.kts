plugins {
    `java-library`
}

val jacksonVersion: String by project
val faker: String by project

dependencies {
    api(project(":extensions:dataspace-authority-spi"))
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
}

