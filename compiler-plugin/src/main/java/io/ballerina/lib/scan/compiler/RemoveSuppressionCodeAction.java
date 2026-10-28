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

import static io.ballerina.lib.scan.compiler.SuppressCodeActions.hasLineBreak;
import static io.ballerina.lib.scan.compiler.SuppressCodeActions.indentation;
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
        return SuppressCodeActions.codeActionInfo(context, "Remove suppression")
                .filter(info -> SuppressCodeActions.annotationAt(context)
                        .flatMap(annotation -> SuppressAttachment.resolve(context.currentSemanticModel(), annotation))
                        .flatMap(SuppressAttachment::reason)
                        .filter(reason -> !reason.isBlank())
                        .isEmpty());
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext context) {
        return SuppressCodeActions.execute(context, RemoveSuppressionCodeAction::removeAnnotation);
    }

    private static Optional<TextEdit> removeAnnotation(AnnotationNode annotation) {
        int start = annotation.textRange().startOffset();
        // An annotation on a line of its own goes with that line; comments above it stay
        if (hasLineBreak(annotation.trailingMinutiae())) {
            start -= indentation(annotation.leadingMinutiae().toString()).length();
        }
        return Optional.of(replace(start, annotation.textRangeWithMinutiae().endOffset(), ""));
    }

    @Override
    public String name() {
        return "REMOVE_SUPPRESSION";
    }
}
