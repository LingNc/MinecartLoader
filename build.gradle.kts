group = "com.lingnc.minecartloader"
version = "1.0.2"
val paperGameVersion = "paper-1.21.7"

plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    // Paper 仓库
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    // WildLoaders API 仓库
    maven {
        url = uri("https://repo.bg-software.com/repository/api/")
    }
}

dependencies {
    // 用你服务器对应的 Paper 版本
    compileOnly("io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")

    // 用你选择的 WildLoadersAPI 版本，比如最新的 2025.2
    compileOnly("com.bgsoftware:WildLoadersAPI:2025.2")
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("MinecartLoader-$paperGameVersion")
    manifest {
        attributes(
            "Implementation-Title" to "MinecartLoader",
            "Implementation-Version" to version,
            "Game-Version" to paperGameVersion
        )
    }
}