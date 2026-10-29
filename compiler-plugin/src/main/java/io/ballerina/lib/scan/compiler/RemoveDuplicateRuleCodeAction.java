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
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.projects.plugins.codeaction.CodeAction;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.tools.text.TextEdit;

import java.util.List;
import java.util.Optional;

/**
 * Removes a rule listed more than once from the {@code rules} of a {@code @scan:Suppress} annotation, along with its
 * separator. It is offered only on a member of a list written in the annotation, since a duplicate in a constant would
 * have to be removed from the constant's definition.
 *
 * @since 1.0.0
 */
class RemoveDuplicateRuleCodeAction implements CodeAction {

    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(DiagnosticCode.DUPLICATE_RULE.code());
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext context) {
        return SuppressCodeActions.codeActionInfo(context, "Remove duplicate rule",
                RemoveDuplicateRuleCodeAction::ruleMember, RemoveDuplicateRuleCodeAction::removeMember);
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext context) {
        return SuppressCodeActions.execute(context, RemoveDuplicateRuleCodeAction::ruleMember,
                RemoveDuplicateRuleCodeAction::removeMember);
    }

    private static Optional<Node> ruleMember(NonTerminalNode node) {
        if (node != null && node.parent() instanceof ListConstructorExpressionNode list
                && list.parent() instanceof SpecificFieldNode field
                && SuppressAttachment.fieldName(field).equals(SuppressAttachment.RULES_FIELD)
                && field.parent().parent() instanceof AnnotationNode) {
            return Optional.of(node);
        }
        return Optional.empty();
    }

    private static Optional<TextEdit> removeMember(Node member) {
        ListConstructorExpressionNode list = (ListConstructorExpressionNode) member.parent();
        SeparatedNodeList<Node> members = list.expressions();
        int index = 0;
        while (index < members.size() && members.get(index) != member) {
            index++;
        }
        // The first occurrence is the one kept, so a duplicate always has a member before it
        if (index < 1 || index == members.size()) {
            return Optional.empty();
        }
        return Optional.of(SuppressCodeActions.removeElement(members, index, list.openBracket()));
    }

    @Override
    public String name() {
        return "REMOVE_DUPLICATE_SUPPRESSION_RULE";
    }
}
