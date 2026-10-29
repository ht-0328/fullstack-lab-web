# syntax=docker/dockerfile:1
# ビルドコンテキストは web/（画面との契約 openapi/bff.yaml を含めるため）
FROM eclipse-temurin:21-jdk-noble AS build
# API / 認可 API のクライアントを取得する Maven リポジトリ（ローカル: Reposilite、CI: GitHub Packages、AWS: CodeArtifact）
ARG MAVEN_REPO_URL=http://localhost:8081/releases
ENV MAVEN_REPO_URL=$MAVEN_REPO_URL
WORKDIR /workspace/bff
COPY openapi /workspace/openapi
COPY bff/gradlew bff/settings.gradle.kts bff/build.gradle.kts bff/gradle.properties ./
COPY bff/gradle gradle
# 依存解決を先に行い、ソース変更時もこのレイヤーをキャッシュさせる。
# 認証が必要なリポジトリ向けの資格情報は build secret で渡す（イメージには残らない。無ければ匿名で取得する）。
# `gradle dependencies` が取得するのはメタデータまでで、jar 本体はコンパイル時に取得される。
# そのため後続の Gradle 実行にも同じ secret を渡す
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=secret,id=maven_repo_user,env=MAVEN_REPO_USER \
    --mount=type=secret,id=maven_repo_password,env=MAVEN_REPO_PASSWORD \
    ./gradlew dependencies --no-daemon -q > /dev/null
COPY bff/src src

# 生成コードが契約と一致していることを確かめる。契約だけ直して生成し忘れた変更や、生成コードを手で直した変更をここで落とす
FROM build AS check-generated
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=secret,id=maven_repo_user,env=MAVEN_REPO_USER \
    --mount=type=secret,id=maven_repo_password,env=MAVEN_REPO_PASSWORD \
    cp -r src/generated /tmp/generated \
    && ./gradlew generate --no-daemon \
    && diff -r /tmp/generated src/generated \
    || { echo '生成コードが契約と一致しません。bff で ./gradlew generate（集約リポジトリでは ./scripts/generate.sh）を実行し、生成物もコミットしてください' >&2; exit 1; }

# docker build --target test で単体テストを実行する（ホスト側に JDK は不要）
FROM check-generated AS test
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=secret,id=maven_repo_user,env=MAVEN_REPO_USER \
    --mount=type=secret,id=maven_repo_password,env=MAVEN_REPO_PASSWORD \
    ./gradlew test --no-daemon

FROM build AS package
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=secret,id=maven_repo_user,env=MAVEN_REPO_USER \
    --mount=type=secret,id=maven_repo_password,env=MAVEN_REPO_PASSWORD \
    ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-noble AS runtime
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=package /workspace/bff/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
