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

# Rule `ballerina:1`. Using `checkpanic` lets an unhandled error panic and crash the program instead of being handled.
public const RuleId CHECKPANIC_USAGE = "ballerina:1";

# Rule `ballerina:2`. A function parameter is declared but never used within the function body.
public const RuleId UNUSED_FUNCTION_PARAMETER = "ballerina:2";

# Rule `ballerina:3`. A public function is not marked `isolated`, so it cannot be safely called from concurrently
# executing code.
public const RuleId PUBLIC_NON_ISOLATED_FUNCTION_CONSTRUCT = "ballerina:3";

# Rule `ballerina:4`. A public method is not marked `isolated`, so it cannot be safely called from concurrently
# executing code.
public const RuleId PUBLIC_NON_ISOLATED_METHOD_CONSTRUCT = "ballerina:4";

# Rule `ballerina:5`. A public class is not marked isolated, so its instances cannot be safely shared across
# concurrently executing code.
public const RuleId PUBLIC_NON_ISOLATED_CLASS_CONSTRUCT = "ballerina:5";

# Rule `ballerina:6`. A public object is not marked isolated, so its instances cannot be safely shared across
# concurrently executing code.
public const RuleId PUBLIC_NON_ISOLATED_OBJECT_CONSTRUCT = "ballerina:6";

# Rule `ballerina:7`. An operation always evaluates to `true` regardless of its operands' runtime values.
public const RuleId OPERATION_ALWAYS_EVALUATES_TO_TRUE = "ballerina:7";

# Rule `ballerina:8`. An operation always evaluates to `false` regardless of its operands' runtime values.
public const RuleId OPERATION_ALWAYS_EVALUATES_TO_FALSE = "ballerina:8";

# Rule `ballerina:9`. An operation always reduces to one of its own operands, making the operation itself redundant.
public const RuleId OPERATION_ALWAYS_EVALUATES_TO_SELF_VALUE = "ballerina:9";

# Rule `ballerina:10`. A variable is assigned to itself, which has no effect and usually signals a mistake.
public const RuleId SELF_ASSIGNMENT = "ballerina:10";

# Rule `ballerina:11`. A private class field is declared but never used.
public const RuleId UNUSED_PRIVATE_CLASS_FIELD = "ballerina:11";

# Rule `ballerina:12`. A range expression's bounds never produce any elements, making it dead code.
public const RuleId INVALID_RANGE_EXPRESSION = "ballerina:12";

# Rule `ballerina:13`. A secret such as a password, API key, or token is embedded as a literal value in source code.
public const RuleId HARD_CODED_SECRET = "ballerina:13";

# Rule `ballerina:14`. A secret is assigned a fixed value instead of being exposed as a configurable one.
public const RuleId NON_CONFIGURABLE_SECRET = "ballerina:14";
