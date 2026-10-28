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
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextLine;

import java.util.List;
import java.util.Optional;

import static io.ballerina.lib.scan.compiler.SuppressCodeActions.replace;

/**
 * Removes a {@code @scan:Suppress} annotation with an empty {@code rules}, which suppresses nothing. It is offered
 * only when the annotation gives no reason, so that a written justification is never removed with it.
 *
 * @since 1.0.0
 */
class RemoveSuppressionCodeAction implements CodeAction {

    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(DiagnosticCode.EMPTY_RULE_LIST.code());
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext context) {
        boolean hasReason = SuppressCodeActions.annotationAt(context)
                .flatMap(annotation -> SuppressAttachment.resolve(context.currentSemanticModel(), annotation))
                .flatMap(SuppressAttachment::reason)
                .filter(reason -> !reason.isBlank())
                .isPresent();
        return hasReason ? Optional.empty()
                : SuppressCodeActions.codeActionInfo(context, "Remove suppression",
                        RemoveSuppressionCodeAction::removeAnnotation);
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext context) {
        return SuppressCodeActions.execute(context, RemoveSuppressionCodeAction::removeAnnotation);
    }

    // Comments on and above the annotation's lines are kept, since they may be about the construct
    private static Optional<TextEdit> removeAnnotation(AnnotationNode annotation) {
        TextDocument document = annotation.syntaxTree().textDocument();
        LineRange range = annotation.lineRange();
        TextLine firstLine = document.line(range.startLine().line());
        TextLine lastLine = document.line(range.endLine().line());
        String before = firstLine.text().substring(0, range.startLine().offset());
        String after = lastLine.text().substring(range.endLine().offset());
        int start = annotation.textRange().startOffset();
        int end = annotation.textRange().endOffset();
        if (!before.isBlank()) {
            return Optional.of(replace(start - (before.length() - before.stripTrailing().length()), end, ""));
        }
        if (after.isBlank()) {
            return Optional.of(replace(firstLine.startOffset(), lastLine.endOffsetWithNewLines(), ""));
        }
        return Optional.of(replace(start, end + after.length() - after.stripLeading().length(), ""));
    }

    @Override
    public String name() {
        return "REMOVE_SUPPRESSION";
    }
}
