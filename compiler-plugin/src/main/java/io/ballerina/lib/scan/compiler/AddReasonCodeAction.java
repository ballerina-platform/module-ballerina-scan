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
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.projects.plugins.codeaction.CodeAction;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;

import java.util.List;
import java.util.Optional;

/**
 * Adds an empty {@code reason} to a {@code @scan:Suppress} annotation that has none. The blank reason is reported until
 * it is filled in, so it cannot be left behind unnoticed the way a placeholder could.
 *
 * @since 1.0.0
 */
class AddReasonCodeAction implements CodeAction {

    private static final String REASON = SuppressAttachment.REASON_FIELD + ": \"\"";

    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(DiagnosticCode.MISSING_REASON.code());
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext context) {
        return SuppressCodeActions.codeActionInfo(context, "Add suppression reason");
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext context) {
        return SuppressCodeActions.execute(context, AddReasonCodeAction::addReason);
    }

    private static Optional<TextEdit> addReason(AnnotationNode annotation) {
        if (annotation.annotValue().isEmpty()) {
            return Optional.of(insert(annotation.annotReference().textRange().endOffset(), " {" + REASON + "}"));
        }
        MappingConstructorExpressionNode value = annotation.annotValue().get();
        SeparatedNodeList<MappingFieldNode> fields = value.fields();
        if (fields.isEmpty()) {
            return Optional.of(insert(value.openBrace().textRange().endOffset(), REASON));
        }
        return Optional.of(insert(fields.get(fields.size() - 1).textRange().endOffset(), ", " + REASON));
    }

    private static TextEdit insert(int offset, String text) {
        return TextEdit.from(TextRange.from(offset, 0), text);
    }

    @Override
    public String name() {
        return "ADD_SUPPRESSION_REASON";
    }
}
