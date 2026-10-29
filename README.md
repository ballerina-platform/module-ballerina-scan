# Ballerina Scan Library

[![Build](https://github.com/ballerina-platform/module-ballerina-scan/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-scan/actions/workflows/build-timestamped-master.yml)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-scan/branch/main/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-scan)
[![Security Scan](https://github.com/ballerina-platform/module-ballerina-scan/actions/workflows/security-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-scan/actions/workflows/security-scan.yml)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-scan.svg?label=Last%20Commit)](https://github.com/ballerina-platform/module-ballerina-scan/commits/main)

## Overview

The `ballerina/scan` package provides language support for the [`bal scan`](https://github.com/ballerina-platform/static-code-analysis-tool) static code analysis tool. It provides the `@scan:Suppress` annotation, which accepts individual `bal scan` findings at the construct that contains them, and a compiler plugin that checks each suppression while the code is built or edited.

```ballerina
import ballerina/scan;

public function main() {
    @scan:Suppress {rules: [scan:CHECKPANIC_USAGE], reason: "The literal always parses"}
    int port = checkpanic int:fromString("9090");
}
```

See the [package documentation](ballerina/README.md) for more examples, where the annotation can be placed, how it is validated, the editor's code actions and the project policy in `Scan.toml`.

## Compatibility

The package is packed with Ballerina 2201.12.0 (Swan Lake Update 12), the oldest distribution it supports, so that it can be used with Update 12 and every later distribution. Applying a suppression requires a `bal scan` version that supports in-source suppressions.

## Build from the source

### Set up the prerequisites

1. Download and install Java SE Development Kit (JDK) version 21. You can install either [OpenJDK](https://adoptium.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/).

2. Export GitHub Personal access token with read package permissions as follows.

    ```
    export packageUser=<Username>
    export packagePAT=<Personal access token>
    ```

### Build the source

Execute the commands below to build from source.

1. To build the library:

    ```
    ./gradlew clean build
    ```

2. To run the tests:

    ```
    ./gradlew clean test
    ```

3. To build the package without the tests:

    ```
    ./gradlew clean build -x test
    ```

4. To publish to maven local:

    ```
    ./gradlew clean build publishToMavenLocal
    ```

5. Publish the generated artifacts to the local Ballerina central repository:

    ```
    ./gradlew clean build -PpublishToLocalCentral=true
    ```

6. Publish the generated artifacts to the Ballerina central repository:

    ```
    ./gradlew clean build -PpublishToCentral=true
    ```

## Contribute to Ballerina

As an open source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`scan` library](https://lib.ballerina.io/ballerina/scan/latest).
* For more information on the `bal scan` tool, go to the [static code analysis tool](https://github.com/ballerina-platform/static-code-analysis-tool).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
