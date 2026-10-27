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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Annotatable;
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ExternalFunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExternalFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The value of a {@code @scan:Suppress} annotation as the compiler resolved it, so that constants, string templates
 * and escapes are already evaluated.
 *
 * @param rules  the rule IDs, or empty when the annotation omits {@code rules} and so applies to every rule
 * @param reason the reason, or empty when the annotation omits it
 * @since 1.0.0
 */
record SuppressAttachment(Optional<List<String>> rules, Optional<String> reason) {

    static final String RULES_FIELD = "rules";
    static final String REASON_FIELD = "reason";
    private static final String SCAN_ORG = "ballerina";
    private static final String SCAN_MODULE = "scan";
    private static final String ANNOTATION_NAME = "Suppress";

    /**
     * Resolves the annotation when it is a {@code @scan:Suppress} whose value the compiler could evaluate.
     */
    static Optional<SuppressAttachment> resolve(SemanticModel semanticModel, AnnotationNode annotation) {
        return attachments(semanticModel, annotation).stream()
                .filter(attachment -> attachment.getLocation()
                        .map(location -> location.lineRange().equals(annotation.lineRange())).orElse(false))
                .filter(SuppressAttachment::isSuppress)
                .findFirst()
                .flatMap(AnnotationAttachmentSymbol::attachmentValue)
                .flatMap(SuppressAttachment::fromValue);
    }

    /**
     * The expression given for a field of the annotation in the source, used to place diagnostics and edits.
     */
    static Optional<Node> fieldValue(AnnotationNode annotation, String name) {
        if (annotation.annotValue().isEmpty()) {
            return Optional.empty();
        }
        for (MappingFieldNode field : annotation.annotValue().get().fields()) {
            if (field instanceof SpecificFieldNode specific && fieldName(specific).equals(name)) {
                // A shorthand field such as `{rules}` takes the value of the constant its name refers to
                return Optional.of(specific.valueExpr().map(Node.class::cast).orElse(specific.fieldName()));
            }
        }
        return Optional.empty();
    }

    // `rules`, `'rules` and `"rules"` all name the same field; the token text leaves out surrounding comments
    static String fieldName(SpecificFieldNode field) {
        Node name = field.fieldName();
        String text = name instanceof BasicLiteralNode literal ? literal.literalToken().text() : ((Token) name).text();
        if (text.startsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text.startsWith("'") ? text.substring(1) : text;
    }

    // The construct's symbol holds the attachments, except on a return type, an external body and a listener
    private static List<AnnotationAttachmentSymbol> attachments(SemanticModel semanticModel,
                                                                AnnotationNode annotation) {
        Node parent = annotation.parent();
        if (parent instanceof ReturnTypeDescriptorNode) {
            Symbol owner = semanticModel.symbol(parent.parent().parent()).orElse(null);
            FunctionTypeSymbol type = owner instanceof FunctionSymbol function ? function.typeDescriptor()
                    : owner instanceof FunctionTypeSymbol functionType ? functionType : null;
            return type == null ? List.of()
                    : type.returnTypeAnnotations().map(Annotatable::annotAttachments).orElse(List.of());
        }
        if (parent instanceof ExternalFunctionBodyNode body) {
            return semanticModel.symbol(body.parent())
                    .filter(ExternalFunctionSymbol.class::isInstance)
                    .map(symbol -> ((ExternalFunctionSymbol) symbol).annotAttachmentsOnExternal())
                    .orElse(List.of());
        }
        Node construct = parent instanceof MetadataNode ? parent.parent() : parent;
        Node named = construct instanceof ListenerDeclarationNode listener ? listener.variableName() : construct;
        return semanticModel.symbol(named)
                .filter(Annotatable.class::isInstance)
                .map(symbol -> ((Annotatable) symbol).annotAttachments())
                .orElse(List.of());
    }

    // Identified by the attachment's annotation, since a worker's annotation node has no symbol of its own
    private static boolean isSuppress(AnnotationAttachmentSymbol attachment) {
        AnnotationSymbol annotation = attachment.typeDescriptor();
        return annotation.getName().filter(ANNOTATION_NAME::equals).isPresent()
                && annotation.getModule().map(module -> module.id().orgName().equals(SCAN_ORG)
                && module.id().moduleName().equals(SCAN_MODULE)).orElse(false);
    }

    private static Optional<SuppressAttachment> fromValue(ConstantValue value) {
        if (!(value.value() instanceof Map<?, ?> fields)) {
            return Optional.empty();
        }
        Object rules = unwrap(fields.get(RULES_FIELD));
        Object reason = unwrap(fields.get(REASON_FIELD));
        if (rules != null && !(rules instanceof List<?>) || reason != null && !(reason instanceof String)) {
            return Optional.empty();
        }
        List<String> ruleIds = null;
        if (rules != null) {
            ruleIds = new ArrayList<>();
            for (Object member : (List<?>) rules) {
                Object rule = unwrap(member);
                if (rule == null) {
                    return Optional.empty();
                }
                // Members are compiler-internal values whose string form is the member's value
                ruleIds.add(rule instanceof String text ? text : String.valueOf(rule));
            }
        }
        return Optional.of(new SuppressAttachment(Optional.ofNullable(ruleIds), Optional.ofNullable((String) reason)));
    }

    private static Object unwrap(Object value) {
        return value instanceof ConstantValue constant ? constant.value() : value;
    }
}
