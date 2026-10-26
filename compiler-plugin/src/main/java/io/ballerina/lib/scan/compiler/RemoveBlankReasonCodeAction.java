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
import io.ballerina.compiler.syntax.tree.Minutiae;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
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
        return SuppressCodeActions.codeActionInfo(context, "Remove blank suppression reason");
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext context) {
        return SuppressCodeActions.execute(context, RemoveBlankReasonCodeAction::removeReason);
    }

    private static Optional<TextEdit> removeReason(AnnotationNode annotation) {
        if (annotation.annotValue().isEmpty()) {
            return Optional.empty();
        }
        MappingConstructorExpressionNode value = annotation.annotValue().get();
        SeparatedNodeList<MappingFieldNode> fields = value.fields();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i) instanceof SpecificFieldNode field && SuppressAnnotationValidator.fieldName(field)
                    .equals(SuppressAnnotationValidator.REASON_FIELD)) {
                return Optional.of(removal(annotation, fields, i));
            }
        }
        return Optional.empty();
    }

    private static TextEdit removal(AnnotationNode annotation, SeparatedNodeList<MappingFieldNode> fields, int index) {
        if (fields.size() == 1) {
            return replace(annotation.annotReference().textRange().endOffset(),
                    annotation.annotValue().get().textRange().endOffset(), "");
        }
        // Comments around the reason go with it; those of the neighbouring field, on its separator, stay
        if (index > 0) {
            Token separator = fields.getSeparator(index - 1);
            int start = fields.get(index - 1).textRange().endOffset();
            if (!hasComment(separator.trailingMinutiae())) {
                return replace(start, fields.get(index).textRange().endOffset(), "");
            }
            // The kept comment ends with its line break, which replaces the one after the reason
            return replace(start, fields.get(index).textRangeWithMinutiae().endOffset(),
                    separator.trailingMinutiae().toString());
        }
        return replace(fields.get(0).textRangeWithMinutiae().startOffset(),
                fields.getSeparator(0).textRangeWithMinutiae().endOffset(), "");
    }

    private static boolean hasComment(MinutiaeList minutiae) {
        for (Minutiae minutia : minutiae) {
            if (minutia.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                return true;
            }
        }
        return false;
    }

    private static TextEdit replace(int start, int end, String text) {
        return TextEdit.from(TextRange.from(start, end - start), text);
    }

    @Override
    public String name() {
        return "REMOVE_BLANK_SUPPRESSION_REASON";
    }
}
