# Build the existing Kotlin server and both Compose web apps together.
FROM eclipse-temurin:25-jdk-jammy AS build
# The Node.js runtime downloaded by Kotlin/Wasm requires libatomic.so.1.
RUN apt-get update && apt-get install -y --no-install-recommends libatomic1 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /workspace
COPY . .
# Override the Windows-only Java path for this Linux build. Run compilation in
# one JVM and limit parallel workers to keep memory use predictable on hosts.
RUN java -Dorg.gradle.java.home=/opt/java/openjdk \
    -Dorg.gradle.jvmargs="-Xmx3g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8" \
    -jar gradle/wrapper/gradle-wrapper.jar --no-daemon --no-parallel --max-workers=2 \
    -Pkotlin.compiler.execution.strategy=in-process \
    :server:installDist :buyerApp:wasmJsBrowserDistribution :merchantApp:wasmJsBrowserDistribution

FROM eclipse-temurin:25-jre-jammy
# Product placeholders use Java2D text rendering even in headless mode.
RUN apt-get update && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENV PORT=8080 \
    WEB_DIST_BUYER=/app/web/buyer \
    WEB_DIST_MERCHANT=/app/web/merchant \
    JAVA_OPTS="-Xms64m -Xmx512m -Djava.awt.headless=true"
COPY --from=build /workspace/server/build/install/server /app/server
COPY --from=build /workspace/buyerApp/build/dist/wasmJs/productionExecutable /app/web/buyer
COPY --from=build /workspace/merchantApp/build/dist/wasmJs/productionExecutable /app/web/merchant
EXPOSE 8080
CMD ["/app/server/bin/server"]
