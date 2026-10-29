// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;

@test:Config
function testCoreRuleIds() {
    RuleId[] constants = [CHECKPANIC_USAGE, UNUSED_FUNCTION_PARAMETER, NON_ISOLATED_PUBLIC_FUNCTION,
        NON_ISOLATED_PUBLIC_METHOD, NON_ISOLATED_PUBLIC_CLASS, NON_ISOLATED_PUBLIC_OBJECT, ALWAYS_TRUE_OPERATION,
        ALWAYS_FALSE_OPERATION, REDUNDANT_OPERATION, SELF_ASSIGNMENT, UNUSED_PRIVATE_CLASS_FIELD,
        EMPTY_RANGE_EXPRESSION, HARD_CODED_SECRET, NON_CONFIGURABLE_SECRET];
    RuleId[] ids = from int id in 1 ... 14 select string `ballerina:${id}`;
    test:assertEquals(constants, ids, "each core rule constant names the rule with the matching numeric ID");
}
