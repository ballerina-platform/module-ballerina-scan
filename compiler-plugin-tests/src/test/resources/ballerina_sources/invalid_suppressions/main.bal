// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/scan;

const RULE_LIST = ["ballerina:1", "http:1"];

public function main() {
    @scan:Suppress {rules: ["http:1"], reason: "Malformed rule ID"}
    int a = 1;
    @scan:Suppress {rules: ["ballerina:0", "ballerina", "acme:lint:1"], reason: "Malformed rule IDs"}
    int b = 2;
    @scan:Suppress {rules: ["ballerina:1", "ballerina:1"], reason: "Duplicate rule"}
    int c = 3;
    @scan:Suppress {rules: ["ballerina:1"], reason: " "}
    int d = 4;
    @scan:Suppress {rules: [], reason: "Empty rule list"}
    int e = 5;
    @scan:Suppress {rules: RULE_LIST, reason: "Constant list with a malformed rule"}
    int f = 6;
    @scan:Suppress {
        // SEC-1042
        rules: ["http:1"],
        reason: "Commented fields"
    }
    int g = 7;
    @scan:Suppress {rules: ["ballerina:1"], reason: "First"}
    @scan:Suppress {rules: ["ballerina:1", "ballerina:2"], reason: "Rule already suppressed above"}
    int h = 8;
    @scan:Suppress {rules: ["ballerina:1"], reason: "\t"}
    int i = 9;
    @scan:Suppress {reason: "First"}
    @scan:Suppress {reason: "Every rule again"}
    int j = 10;
    _ = a + b + c + d + e + f + g + h + i + j;
}

function workers() {
    @scan:Suppress {rules: ["http:1"], reason: "Worker"}
    worker W {
    }
}

type Limit record {|
    int max;
|};

annotation Limit Limited on function;

@Limited {
    max: let
        @scan:Suppress {rules: ["http:1"], reason: "Nested in another annotation's value"}
        int base = 10
    in base * 2
}
function limited() {
}

type Opts record {|
    int a = 1;
|};

type Pair [@scan:Suppress {rules: ["http:2"], reason: "Tuple member"} int, string];

type Parser function () returns @scan:Suppress {rules: ["http:3"], reason: "Function type"} int;

function included(@scan:Suppress {rules: ["http:4"], reason: "Included record parameter"} *Opts opts) {
}

@scan:Suppress {rules: ["http:5"], reason: "Destructured module variable"}
var {count} = {count: 1};

function lambdas() {
    var parse = function () returns @scan:Suppress {rules: ["http:6"], reason: "Anonymous function"} int {
        return 1;
    };
    @scan:Suppress {rules: ["http:7"], reason: "Destructured variable"}
    [int, int] [x, y] = [1, 2];
    @scan:Suppress {r\u{75}les: ["ballerina:1", "ballerina:1"], reason: "Escaped field name"}
    int z = 3;
    _ = parse() + x + y + z + count;
}
