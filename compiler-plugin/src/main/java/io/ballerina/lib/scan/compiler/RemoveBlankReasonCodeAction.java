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
            if (fields.get(i) instanceof SpecificFieldNode field
                    && SuppressAttachment.fieldName(field).equals(SuppressAttachment.REASON_FIELD)) {
                return Optional.of(removal(annotation, value, i));
            }
        }
        return Optional.empty();
    }

    private static TextEdit removal(AnnotationNode annotation, MappingConstructorExpressionNode value, int index) {
        SeparatedNodeList<MappingFieldNode> fields = value.fields();
        if (fields.size() == 1) {
            return replace(annotation.annotReference().textRange().endOffset(), value.textRange().endOffset(), "");
        }
        MappingFieldNode field = fields.get(index);
        if (index == 0) {
            Token separator = fields.getSeparator(0);
            MappingFieldNode next = fields.get(1);
            int end = separator.textRangeWithMinutiae().endOffset();
            // A reason on the brace's line pulls the next field up to it, unless comments keep that field apart
            if (!hasLineBreak(value.openBrace().trailingMinutiae()) && hasLineBreak(separator.trailingMinutiae())
                    && !hasComment(next.leadingMinutiae())) {
                end = next.textRange().startOffset();
            }
            return replace(field.textRangeWithMinutiae().startOffset(), end, "");
        }
        // The reason's comments go with it, while a comment on the separator belongs to the field before it
        Token separator = fields.getSeparator(index - 1);
        String separatorTrivia = separator.trailingMinutiae().toString();
        String fieldTrivia = field.trailingMinutiae().toString();
        String lineBreak = separatorTrivia.contains("\r\n") || fieldTrivia.contains("\r\n") ? "\r\n" : "\n";
        String kept = hasComment(separator.trailingMinutiae()) ? separatorTrivia
                : hasLineBreak(separator.trailingMinutiae()) || hasLineBreak(field.trailingMinutiae()) ? lineBreak : "";
        // What followed the reason on its line, such as the closing brace, keeps the reason's indentation
        if (kept.endsWith("\n") && !hasLineBreak(field.trailingMinutiae())) {
            kept += indentation(field.leadingMinutiae().toString());
        }
        return replace(fields.get(index - 1).textRange().endOffset(), field.textRangeWithMinutiae().endOffset(), kept);
    }

    private static String indentation(String leadingTrivia) {
        String lastLine = leadingTrivia.substring(leadingTrivia.lastIndexOf('\n') + 1);
        return lastLine.isBlank() ? lastLine : "";
    }

    private static boolean hasLineBreak(MinutiaeList minutiae) {
        for (Minutiae minutia : minutiae) {
            if (minutia.kind() == SyntaxKind.END_OF_LINE_MINUTIAE) {
                return true;
            }
        }
        return false;
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
