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
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
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
 * Shared steps of the code actions that edit the {@code @scan:Suppress} annotation a diagnostic was reported in.
 *
 * @since 1.0.0
 */
final class SuppressCodeActions {

    private static final String ANNOTATION_RANGE = "annotation.range";

    private SuppressCodeActions() {
    }

    static Optional<CodeActionInfo> codeActionInfo(CodeActionContext context, String title) {
        SyntaxTree syntaxTree = context.currentDocument().syntaxTree();
        return enclosingAnnotation(syntaxTree, context.diagnostic().location().lineRange())
                .map(annotation -> CodeActionInfo.from(title,
                        List.of(CodeActionArgument.from(ANNOTATION_RANGE, annotation.lineRange()))));
    }

    static List<DocumentEdit> execute(CodeActionExecutionContext context,
                                      Function<AnnotationNode, Optional<TextEdit>> edit) {
        SyntaxTree syntaxTree = context.currentDocument().syntaxTree();
        return context.arguments().stream()
                .filter(argument -> ANNOTATION_RANGE.equals(argument.key()))
                .findFirst()
                .flatMap(argument -> enclosingAnnotation(syntaxTree, argument.valueAs(LineRange.class)))
                .flatMap(edit)
                .map(textEdit -> syntaxTree.textDocument().apply(TextDocumentChange.from(new TextEdit[]{textEdit})))
                .map(document -> List.of(new DocumentEdit(context.fileUri(), SyntaxTree.from(document))))
                .orElse(List.of());
    }

    private static Optional<AnnotationNode> enclosingAnnotation(SyntaxTree syntaxTree, LineRange lineRange) {
        TextDocument document = syntaxTree.textDocument();
        int start = document.textPositionFrom(lineRange.startLine());
        int end = document.textPositionFrom(lineRange.endLine());
        NonTerminalNode node = ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start),
                true);
        while (node != null && !(node instanceof AnnotationNode)) {
            node = node.parent();
        }
        return Optional.ofNullable((AnnotationNode) node);
    }
}
