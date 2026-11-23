# SDK Java POC timer sample

This standalone Gradle build registers a workflow with `WorkerFactory`, invokes it through a
typed `WorkflowClient` stub, and checks the result after two durable timers fire. Its sole
Temporal dependency is `io.temporal:sdk-java-poc`.

Use JDK 21. Build the POC with `./bootstrap.sh` and `./gradlew publish` in the
`sdk-java-poc` checkout. The sample resolves the Maven artifact from the sibling POC checkout by default.
Use `-PsdkJavaPocRepo=/path/to/sdk-java-poc/build/maven-repository` to select another location.

Start a local server with `temporal server start-dev` and run from the samples repository root.

```sh
./gradlew -p poc-timer run
./gradlew -p poc-timer dependencies --configuration runtimeClasspath
```

The sample accepts a server target and optional workflow ID through Gradle's `--args` option.

To verify replay across a worker restart, install the sample and run the verifier with Python 3
and the Temporal CLI on your path. The verifier starts a disposable server and kills the first
worker process after the server records the first timer. A replacement worker completes the
same run, and the verifier checks the result and event history.

```sh
./gradlew -p poc-timer installDist
python3 poc-timer/verify_restart.py
```

## Existing samples

The standalone Gradle build also compiles and runs the original `HelloQuery` and `HelloSignal`
sources from `core/src/main/java`. They use `sdk-java-poc` for the SDK and the samples'
`temporal-envconfig` module for client configuration.

With a local Temporal server running, use these tasks from the samples repository root.

```sh
TEMPORAL_ADDRESS=127.0.0.1:7233 ./gradlew -p poc-timer runHelloQuery
TEMPORAL_ADDRESS=127.0.0.1:7233 ./gradlew -p poc-timer runHelloSignal
./gradlew -p poc-timer dependencies --configuration existingSamplesRuntimeClasspath
```

### Worker Versioning

Use a fresh local Temporal server. Start the 1.0 worker first, then run the original starter
and start the remaining workers in separate terminals as it prompts.

```sh
./gradlew -p poc-timer runWorkerV1
./gradlew -p poc-timer runVersioningStarter
./gradlew -p poc-timer runWorkerV1_1
./gradlew -p poc-timer runWorkerV2
```

The starter attempts promotion as soon as it sees a new build. The server requires both its
workflow and activity pollers to register first. Inspect registration with
`temporal worker deployment describe-version --deployment-name my-deployment --build-id 1.1`.
