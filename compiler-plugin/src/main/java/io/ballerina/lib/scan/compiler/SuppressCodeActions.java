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
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Shared steps of the code actions that edit a {@code @scan:Suppress} annotation, or a node within it, that a
 * diagnostic was reported on.
 *
 * @since 1.0.0
 */
final class SuppressCodeActions {

    private static final String NODE_RANGE = "node.range";

    private SuppressCodeActions() {
    }

    static Optional<CodeActionInfo> codeActionInfo(CodeActionContext context, String title,
                                                   Function<AnnotationNode, Optional<TextEdit>> edit) {
        return codeActionInfo(context, title, SuppressCodeActions::enclosingAnnotation, edit);
    }

    // Offered only when the edit can be made, so that applying the action always changes the source
    static <T extends Node> Optional<CodeActionInfo> codeActionInfo(CodeActionContext context, String title,
                                                                    Function<NonTerminalNode, Optional<T>> target,
                                                                    Function<T, Optional<TextEdit>> edit) {
        SyntaxTree syntaxTree = context.currentDocument().syntaxTree();
        return target.apply(findNode(syntaxTree, context.diagnostic().location().lineRange()))
                .filter(node -> edit.apply(node).isPresent())
                .map(node -> CodeActionInfo.from(title,
                        List.of(CodeActionArgument.from(NODE_RANGE, node.lineRange()))));
    }

    static Optional<AnnotationNode> annotationAt(CodeActionContext context) {
        return enclosingAnnotation(findNode(context.currentDocument().syntaxTree(),
                context.diagnostic().location().lineRange()));
    }

    static List<DocumentEdit> execute(CodeActionExecutionContext context,
                                      Function<AnnotationNode, Optional<TextEdit>> edit) {
        return execute(context, SuppressCodeActions::enclosingAnnotation, edit);
    }

    static <T extends Node> List<DocumentEdit> execute(CodeActionExecutionContext context,
                                                       Function<NonTerminalNode, Optional<T>> target,
                                                       Function<T, Optional<TextEdit>> edit) {
        SyntaxTree syntaxTree = context.currentDocument().syntaxTree();
        return context.arguments().stream()
                .filter(argument -> NODE_RANGE.equals(argument.key()))
                .findFirst()
                .flatMap(argument -> target.apply(findNode(syntaxTree, argument.valueAs(LineRange.class))))
                .flatMap(edit)
                .map(textEdit -> syntaxTree.textDocument().apply(TextDocumentChange.from(new TextEdit[]{textEdit})))
                .map(document -> List.of(new DocumentEdit(context.fileUri(), SyntaxTree.from(document))))
                .orElse(List.of());
    }

    static Optional<TextEdit> removeField(AnnotationNode annotation, String name) {
        if (annotation.annotValue().isEmpty()) {
            return Optional.empty();
        }
        MappingConstructorExpressionNode value = annotation.annotValue().get();
        SeparatedNodeList<MappingFieldNode> fields = value.fields();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i) instanceof SpecificFieldNode field
                    && SuppressAttachment.fieldName(field).equals(name)) {
                // The value goes with its only field, since an empty `{}` adds nothing
                return Optional.of(fields.size() == 1
                        ? replace(annotation.annotReference().textRange().endOffset(), value.textRange().endOffset(),
                                "")
                        : removeElement(fields, i, value.openBrace()));
            }
        }
        return Optional.empty();
    }

    /**
     * Removes a member of a mapping or list constructor together with a separator, keeping the layout of the members
     * around it and the comments that belong to them.
     */
    static TextEdit removeElement(SeparatedNodeList<? extends Node> elements, int index, Token open) {
        Node element = elements.get(index);
        if (index == 0) {
            Token separator = elements.getSeparator(0);
            int start = element.textRangeWithMinutiae().startOffset();
            // An element on the opening token's line pulls the next one up to it, unless comments keep that one apart
            if (!hasLineBreak(open.trailingMinutiae()) && hasLineBreak(separator.trailingMinutiae())) {
                return hasComment(elements.get(1).leadingMinutiae())
                        ? replace(start, separator.textRangeWithMinutiae().endOffset(),
                                lineBreak(separator.trailingMinutiae().toString()))
                        : replace(start, elements.get(1).textRange().startOffset(), "");
            }
            return replace(start, separator.textRangeWithMinutiae().endOffset(), "");
        }
        Node previous = elements.get(index - 1);
        Token separator = elements.getSeparator(index - 1);
        if (index < elements.size() - 1) {
            Token next = elements.getSeparator(index);
            // An element on a line of its own goes with that line, including the comments on it
            if (hasLineBreak(separator.trailingMinutiae()) && hasLineBreak(next.trailingMinutiae())) {
                return replace(element.textRangeWithMinutiae().startOffset(), next.textRangeWithMinutiae().endOffset(),
                        "");
            }
            // An element ending its line goes with the separator before it, so the line keeps its break
            if (hasLineBreak(next.trailingMinutiae())) {
                return replace(previous.textRange().endOffset(), element.textRange().endOffset(), "");
            }
            // Otherwise it goes with the separator after it, so a comment on the separator before it stays
            return replace(element.textRange().startOffset(), next.textRangeWithMinutiae().endOffset(), "");
        }
        // The last element's comments go with it, while a comment on the separator belongs to the element before it
        String separatorTrivia = separator.trailingMinutiae().toString();
        String kept = hasComment(separator.trailingMinutiae()) ? separatorTrivia
                : hasLineBreak(element.trailingMinutiae()) ? lineBreak(element.trailingMinutiae().toString()) : "";
        // What followed the element on its line, such as the closing token, keeps the element's indentation
        if (kept.endsWith("\n") && !hasLineBreak(element.trailingMinutiae())) {
            kept += indentation(element.leadingMinutiae().toString());
        }
        return replace(previous.textRange().endOffset(), element.textRangeWithMinutiae().endOffset(), kept);
    }

    static String indentation(String leadingTrivia) {
        String lastLine = leadingTrivia.substring(leadingTrivia.lastIndexOf('\n') + 1);
        return lastLine.isBlank() ? lastLine : "";
    }

    static String lineBreak(String trivia) {
        return trivia.contains("\r\n") ? "\r\n" : "\n";
    }

    static boolean hasLineBreak(MinutiaeList minutiae) {
        for (Minutiae minutia : minutiae) {
            if (minutia.kind() == SyntaxKind.END_OF_LINE_MINUTIAE) {
                return true;
            }
        }
        return false;
    }

    static boolean hasComment(MinutiaeList minutiae) {
        for (Minutiae minutia : minutiae) {
            if (minutia.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                return true;
            }
        }
        return false;
    }

    static TextEdit replace(int start, int end, String text) {
        return TextEdit.from(TextRange.from(start, end - start), text);
    }

    private static Optional<AnnotationNode> enclosingAnnotation(NonTerminalNode node) {
        while (node != null && !(node instanceof AnnotationNode)) {
            node = node.parent();
        }
        return Optional.ofNullable((AnnotationNode) node);
    }

    private static NonTerminalNode findNode(SyntaxTree syntaxTree, LineRange lineRange) {
        TextDocument document = syntaxTree.textDocument();
        int start = document.textPositionFrom(lineRange.startLine());
        int end = document.textPositionFrom(lineRange.endLine());
        return ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start), true);
    }
}
