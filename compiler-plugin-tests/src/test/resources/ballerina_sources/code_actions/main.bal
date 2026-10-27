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

public function main() {
    @scan:Suppress
    int a = 1;
    @scan:Suppress {}
    int b = 2;
    @scan:Suppress {rules: ["ballerina:1"]}
    int c = 3;
    @scan:Suppress {reason: ""}
    int d = 4;
    @scan:Suppress {reason: " ", rules: ["ballerina:1"]}
    int e = 5;
    @scan:Suppress {rules: ["ballerina:1"], reason: ""}
    int f = 6;
    @scan:Suppress {
        reason: " ",
        // Accepted until the parser is replaced
        rules: ["ballerina:1"]
    }
    int g = 7;
    @scan:Suppress {
        rules: ["ballerina:1"], // Accepted until the parser is replaced
        reason: ""
    }
    int h = 8;
    @scan:Suppress {rules: ["ballerina:1"], // Accepted until the parser is replaced
        reason: ""}
    int i = 9;
    @scan:Suppress {
        rules: ["ballerina:1"],
        reason: "" // Blank for now
    }
    int j = 10;
    @scan:Suppress {rules: ["ballerina:1", "ballerina:1", "ballerina:2"], reason: "Fixed by the parser rewrite"}
    int k = 11;
    @scan:Suppress {rules: ["ballerina:1", "ballerina:1"], reason: "Fixed by the parser rewrite"}
    int l = 12;
    @scan:Suppress {
        rules: [
            "ballerina:1",
            // Copied from the gateway
            "ballerina:1", // Kept for the gateway
            "ballerina:2"
        ],
        reason: "Fixed by the parser rewrite"
    }
    int m = 13;
    @scan:Suppress {
        rules: [
            "ballerina:1", // Accepted until the parser is replaced
            "ballerina:1"
        ],
        reason: "Fixed by the parser rewrite"
    }
    int n = 14;
    @scan:Suppress {rules: ["ballerina:1",
            "ballerina:1"], reason: "Fixed by the parser rewrite"}
    int o = 15;
    @scan:Suppress {rules: DUPLICATED_RULES, reason: "Fixed by the parser rewrite"}
    int p = 16;
    _ = a + b + c + d + e + f + g + h + i + j + k + l + m + n + o + p;
}

const string[] DUPLICATED_RULES = ["ballerina:1", "ballerina:1"];
