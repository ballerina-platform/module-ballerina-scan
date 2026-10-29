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

const string RULE = "ballerina:1";
const RULES = ["ballerina:1", "ballerina/http:2"];
const rules = ["exampleOrg/example_module_static_code_analyzer:3"];

type LocalSuppressConfig record {|
    string[] rules;
|};

const annotation LocalSuppressConfig Suppress on source var;

@scan:Suppress
public function main() {
    @scan:Suppress {rules: ["ballerina:1", "ballerina/http:2"], reason: "Literal rule IDs"}
    int a = 1;
    @scan:Suppress {rules: [RULE], reason: "Rule ID through a constant"}
    int b = 2;
    @scan:Suppress {rules: RULES, reason: "Rule IDs through a constant list"}
    int c = 3;
    @scan:Suppress {rules}
    int d = 4;
    @scan:Suppress {'rules: ["ballerina:2"], "reason": "Quoted field names"}
    int e = 5;
    @scan:Suppress {rules: ["ballerina:1"], reason: "First reason"}
    @scan:Suppress {rules: ["ballerina:2"], reason: "Second reason"}
    int f = 6;
    @Suppress {rules: ["http:1", "http:1"]}
    int g = 7;
    @scan:Suppress {rules: ["ballerina:\u{31}"], reason: "Escaped rule ID"}
    int h = 8;
    @scan:Suppress {rules: [string `ballerina:1`], reason: "Template of a constant"}
    int i = 9;
    @scan:Suppress {
        // SEC-1042
        reason: "Commented reason"
    }
    int j = 10;
    @scan:Suppress {
        rules: [scan:CHECKPANIC_USAGE, scan:UNUSED_FUNCTION_PARAMETER, scan:PUBLIC_NON_ISOLATED_FUNCTION_CONSTRUCT,
            scan:PUBLIC_NON_ISOLATED_METHOD_CONSTRUCT, scan:PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT,
            scan:PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT, scan:OPERATION_ALWAYS_EVALUATES_TO_TRUE,
            scan:OPERATION_ALWAYS_EVALUATES_TO_FALSE, scan:OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE,
            scan:SELF_ASSIGNMENT, scan:UNUSED_PRIVATE_CLASS_FIELD, scan:INVALID_RANGE_EXPRESSION,
            scan:HARD_CODED_SECRET, scan:NON_CONFIGURABLE_SECRET],
        reason: "Rules through the constants of the package"
    }
    int k = 11;
    _ = a + b + c + d + e + f + g + h + i + j + k;
}
