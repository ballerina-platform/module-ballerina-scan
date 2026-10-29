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

import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.projects.plugins.codeaction.CodeAction;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.tools.text.TextEdit;

import java.util.List;
import java.util.Optional;

/**
 * Removes a blank {@code reason} from a {@code @scan:Suppress} annotation, along with its separator, or the whole
 * annotation value when the reason is its only field.
 *
 * @since 1.0.0
 */
class RemoveBlankReasonCodeAction implements CodeAction {

    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(DiagnosticCode.BLANK_REASON.code());
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext context) {
        return SuppressCodeActions.codeActionInfo(context, "Remove blank suppression reason",
                RemoveBlankReasonCodeAction::removeReason);
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext context) {
        return SuppressCodeActions.execute(context, RemoveBlankReasonCodeAction::removeReason);
    }

    private static Optional<TextEdit> removeReason(AnnotationNode annotation) {
        return SuppressCodeActions.removeField(annotation, SuppressAttachment.REASON_FIELD);
    }

    @Override
    public String name() {
        return "REMOVE_BLANK_SUPPRESSION_REASON";
    }
}
