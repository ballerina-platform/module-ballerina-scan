/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.lib.scan.compiler;

import io.ballerina.projects.plugins.codeaction.CodeAction;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;

import java.util.List;
import java.util.Optional;

/**
 * Removes an empty {@code rules} from a {@code @scan:Suppress} annotation, so that it suppresses every rule on its
 * construct. The title says so, since this widens the suppression rather than keeping its meaning.
 *
 * @since 1.0.0
 */
class SuppressAllRulesCodeAction implements CodeAction {

    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(DiagnosticCode.EMPTY_RULE_LIST.code());
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext context) {
        return SuppressCodeActions.codeActionInfo(context, "Suppress all rules on this construct");
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext context) {
        return SuppressCodeActions.execute(context,
                annotation -> SuppressCodeActions.removeField(annotation, SuppressAttachment.RULES_FIELD));
    }

    @Override
    public String name() {
        return "SUPPRESS_ALL_RULES";
    }
}
