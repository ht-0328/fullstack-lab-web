import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.26.0"
}

group = "com.example.fullstacklab"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    // API / 認可 API / 認証 API のクライアントを取得する Maven リポジトリ。
    // ローカル: Reposilite、CI: GitHub Packages、AWS: CodeArtifact（URL と資格情報は環境変数で切り替える）
    maven {
        name = "lab"
        url = uri(providers.environmentVariable("MAVEN_REPO_URL").getOrElse("http://localhost:8081/releases"))
        isAllowInsecureProtocol = true
        providers.environmentVariable("MAVEN_REPO_USER").orNull?.let { user ->
            credentials {
                username = user
                password = providers.environmentVariable("MAVEN_REPO_PASSWORD").getOrElse("")
            }
        }
        content {
            includeGroup("com.example.fullstacklab")
        }
    }
}

val apiClientVersion: String by project
val authzClientVersion: String by project
val authClientVersion: String by project

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // 他リポジトリが OpenAPI から生成して publish したクライアント。ソースではなく成果物に依存する
    implementation("com.example.fullstacklab:api-client:$apiClientVersion")
    implementation("com.example.fullstacklab:authz-client:$authzClientVersion")
    implementation("com.example.fullstacklab:auth-client:$authClientVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// 画面との契約（../openapi/bff.yaml）が正。コントローラーのインターフェースと DTO は src/generated に生成し、git で管理する（業務環境と同じ）。
// 生成コードは手で直さず、仕様を直して `./gradlew generate` で生成し直し、生成物も同じコミットに含める。
// Dockerfile の check-generated ステージが、仕様と生成物のずれを検出する
val generateBffInterfaces by tasks.registering(GenerateTask::class) {
    generatorName.set("kotlin-spring")
    inputSpec.set(layout.projectDirectory.file("../openapi/bff.yaml").asFile.path)
    outputDir.set(layout.projectDirectory.dir("src/generated").asFile.path)
    apiPackage.set("com.example.fullstacklab.bff.generated.api")
    modelPackage.set("com.example.fullstacklab.bff.generated.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "true",
            "useSpringBoot4" to "true",
            "useTags" to "true",
            "useBeanValidation" to "true",
            "documentationProvider" to "none",
            "annotationLibrary" to "none",
            "enumPropertyNaming" to "UPPERCASE",
            "gradleBuildFile" to "false",
            "sourceFolder" to "kotlin",
        ),
    )
    globalProperties.set(mapOf("apis" to "", "models" to ""))
    // 仕様から消えたものが残らないよう、生成する前に前回の生成物を消す（.openapi-generator-ignore は残す）
    doFirst { delete(layout.projectDirectory.dir("src/generated/kotlin")) }
}

// 画面との契約から生成し直す（画面側の型は frontend で `npm run generate:bff`）
tasks.register("generate") {
    dependsOn(generateBffInterfaces)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    sourceSets["main"].kotlin.srcDir("src/generated/kotlin")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 実行可能 jar だけを作る（plain jar は不要）
tasks.jar {
    enabled = false
}
