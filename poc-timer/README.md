# SDK Java POC timer sample

This standalone Gradle build registers a workflow with `WorkerFactory`, invokes it through a
typed `WorkflowClient` stub, and checks the result after two durable timers fire. Its sole
Temporal dependency is `io.temporal:sdk-java-poc`.

Use JDK 21. Build the POC with `./bootstrap.sh` and `./build.sh` in the `sdk-java-poc`
checkout. The sample resolves the Maven artifact from the sibling POC checkout by default.
Use `-PsdkJavaPocRepo=/path/to/sdk-java-poc/build/maven-repository` to select another location.

Start a local server with `temporal server start-dev` and run from the samples repository root.

```sh
./gradlew -p poc-timer run
./gradlew -p poc-timer dependencies --configuration runtimeClasspath
```

The sample accepts a server target and optional workflow ID through Gradle's `--args` option.

The current JVM Core build can report a worker shutdown panic after the workflow completes.
