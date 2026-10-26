# Change Log
This file contains all the notable changes done to the Ballerina Scan package through the releases.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Introduce the `@scan:Suppress` annotation to accept `bal scan` findings in source, with a compiler plugin that validates its rule IDs and reason
- Add code actions to add a missing `@scan:Suppress` reason and remove a blank one
