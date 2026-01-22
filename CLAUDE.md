# Claude Instructions for Kotlin Samples

## Build and Test

Always run tests before committing changes:

```bash
./gradlew :core:test --tests "io.temporal.kotlin.samples.*"
```

## Project Structure

- `core/` - Sample implementations including Kotlin SDK samples
- Kotlin samples are in `core/src/main/kotlin/io/temporal/kotlin/samples/`

## SDK Dependency

This project depends on the local Kotlin SDK at `../sdk-kotlin/`. Changes to the SDK may require rebuilding.
