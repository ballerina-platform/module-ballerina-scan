## Overview

This package provides the `@scan:Suppress` annotation, which accepts a finding of the [`bal scan`](https://github.com/ballerina-platform/static-code-analysis-tool) static code analysis tool at the construct that contains it, along with the reason it is accepted.

```ballerina
import ballerina/scan;

public function main() {
    @scan:Suppress {rules: ["ballerina:1"], reason: "The literal always parses"}
    int port = checkpanic int:fromString("9090");
}
```

A suppressed finding is not dropped. It stays in the scan reports with its reason, so every accepted finding remains reviewable.

### Suppress findings

Suppress one rule:

```ballerina
@scan:Suppress {rules: ["ballerina:1"], reason: "The literal always parses"}
int port = checkpanic int:fromString("9090");
```

Suppress several rules for the same reason:

```ballerina
@scan:Suppress {rules: ["ballerina:1", "ballerina:2"], reason: "Kept until the parser is replaced"}
function parsePort(string input, string locale) returns int {
    return checkpanic int:fromString(input);
}
```

Give each rule its own reason by repeating the annotation:

```ballerina
@scan:Suppress {rules: ["ballerina:1"], reason: "The input is validated by the caller"}
@scan:Suppress {rules: ["ballerina:2"], reason: "locale is part of the public API, SEC-1042"}
function parsePort(string input, string locale) returns int {
    return checkpanic int:fromString(input);
}
```

Share a rule list through a constant:

```ballerina
const LEGACY_PARSER_RULES = ["ballerina:1", "ballerina:2"];

@scan:Suppress {rules: LEGACY_PARSER_RULES, reason: "Kept until the parser is replaced"}
function parsePort(string input, string locale) returns int {
    return checkpanic int:fromString(input);
}
```

Suppress every rule by leaving out `rules`. This also accepts rules added to `bal scan` later, so prefer naming the rules:

```ballerina
@scan:Suppress {reason: "Generated code"}
function generatedMapping(json payload) returns Order|error => payload.cloneWithType();
```

Both fields are optional, so `@scan:Suppress` and `@scan:Suppress {rules: ["ballerina:1"]}` are valid too. A project can require them, as described in [Project policy](#project-policy).

### Rule IDs

Use the rule ID that `bal scan` prints with a finding, or list the rules available to a package with `bal scan --list-rules`.

| Form | Example | Rule of |
|---|---|---|
| `ballerina:<number>` | `ballerina:1` | `bal scan` itself |
| `<org>/<package>:<number>` | `ballerina/http:1` | The analyzer of a package |

### Where to place it

The annotation covers the findings within the construct it is attached to. The rule IDs below only illustrate the syntax:

```ballerina
@scan:Suppress {rules: ["ballerina:1"], reason: "Startup configuration is always valid"}
final int port = checkpanic int:fromString("9090");

@scan:Suppress {rules: ["ballerina:1"], reason: "Generated type"}
type Order record {|
    @scan:Suppress {rules: ["ballerina:1"], reason: "Legacy field name"}
    string order_id;
|};

@scan:Suppress {rules: ["ballerina/http:1"], reason: "Internal network only"}
listener http:Listener ordersListener = new (port);

service /orders on ordersListener {
    @scan:Suppress {rules: ["ballerina/http:2"], reason: "Read-only endpoint behind the gateway"}
    resource function get [string id]() returns Order => {order_id: id};
}

@scan:Suppress {rules: ["ballerina:1"], reason: "Pure function"}
function parse(@scan:Suppress {rules: ["ballerina:1"], reason: "Trusted input"} string value)
        returns @scan:Suppress {rules: ["ballerina:1"], reason: "Always an int"} int {
    @scan:Suppress {rules: ["ballerina:1"], reason: "Validated above"}
    int result = checkpanic int:fromString(value);

    @scan:Suppress {rules: ["ballerina:1"], reason: "Worker input is trusted"}
    worker w {
    }
    return result;
}

function nativeHash(string value) returns string = @scan:Suppress {rules: ["ballerina:1"], reason: "Native code"} external;
```

It can also be attached to a constant, a class, an object field or method, and a service.

- A suppression on a service, class or object type does not reach the findings inside its members. Annotate the member instead.
- A finding in a statement that is not a declaration can be suppressed by turning the statement into one:

  ```ballerina
  @scan:Suppress {rules: ["ballerina:1"], reason: "The literal always parses"}
  var _ = checkpanic int:fromString("1");
  ```

### Validation

Rule IDs and reasons are checked as the compiler evaluates them, so constants, string templates and escapes are resolved first.

| Problem | Example | Reported by | Result |
|---|---|---|---|
| Malformed rule ID | `rules: ["http:1"]` | `bal build` | Error |
| Empty rule list | `rules: []` | `bal build` | Error |
| Rule listed twice | `rules: ["ballerina:1", "ballerina:1"]` | `bal build` | Warning |
| Rule already suppressed by another annotation on the construct | Two annotations naming `ballerina:1` | `bal build`, `bal scan` | Warning |
| Second annotation without `rules` on the construct | Two `@scan:Suppress {reason: "..."}` | `bal build`, `bal scan` | Warning |
| Blank reason | `reason: ""` | `bal build` | Warning |
| Unknown rule | `rules: ["ballerina:999"]` | `bal scan` | Scan fails |
| Rule of an analyzer the scan does not run | `rules: ["acme/lint:1"]` | `bal scan` | Warning |
| Suppression that matched no finding | The finding was fixed | `bal scan` | Warning |
| Missing reason or rules, when the project requires them | `@scan:Suppress {rules: ["ballerina:1"]}` | `bal scan` | Scan fails |

When the scan fails on a suppression, it reports nothing: no findings are printed, saved or sent to a platform.

### Code actions

The editor offers these fixes:

| Problem | Code action | Result |
|---|---|---|
| No reason | Add suppression reason | Adds `reason: ""` to fill in |
| Blank reason | Remove blank suppression reason | Removes `reason` |
| Rule listed twice | Remove duplicate rule | Removes the later occurrence |
| Empty rule list | Suppress all rules on this construct | Removes `rules` |
| Empty rule list, and no reason | Remove suppression | Removes the annotation |

### Project policy

A project can require every suppression to give a reason or name its rules, or disregard suppressions altogether, in `Scan.toml`:

```toml
[suppression]
enabled = true
requireReason = true
requireRuleId = true
```

| Setting | Command-line option | Effect |
|---|---|---|
| `enabled = false` | `--ignore-suppressions` | Reports every finding, as though nothing were suppressed |
| `requireReason = true` | `--require-suppression-reason` | Fails the scan when a suppression gives no reason |
| `requireRuleId = true` | `--require-suppression-rule-id` | Fails the scan when a suppression names no rule |

### Review suppressed findings

- The console prints active findings only, with a summary such as `1 issue(s), 2 suppressed`.
- The JSON and SARIF reports keep suppressed findings, each with a `suppressions` entry carrying the reason.
- The HTML report (`bal scan --scan-report`) lists them under the `Suppressed` filter.
- `bal scan --ignore-suppressions` shows every finding, to audit what is suppressed.
