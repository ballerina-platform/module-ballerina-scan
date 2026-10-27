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
import io.ballerina.compiler.syntax.tree.Minutiae;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
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

    static Optional<CodeActionInfo> codeActionInfo(CodeActionContext context, String title) {
        return codeActionInfo(context, title, SuppressCodeActions::enclosingAnnotation);
    }

    static Optional<CodeActionInfo> codeActionInfo(CodeActionContext context, String title,
                                                   Function<NonTerminalNode, Optional<? extends Node>> target) {
        SyntaxTree syntaxTree = context.currentDocument().syntaxTree();
        return target.apply(findNode(syntaxTree, context.diagnostic().location().lineRange()))
                .map(node -> CodeActionInfo.from(title,
                        List.of(CodeActionArgument.from(NODE_RANGE, node.lineRange()))));
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
