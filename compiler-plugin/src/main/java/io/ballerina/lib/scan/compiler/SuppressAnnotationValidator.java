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
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.ballerina.lib.scan.compiler.SuppressAttachment.REASON_FIELD;
import static io.ballerina.lib.scan.compiler.SuppressAttachment.RULES_FIELD;

/**
 * Checks the form of each {@code @scan:Suppress} annotation: that every rule ID is well formed and listed once, also
 * across the annotations of a construct, and that the reason is not blank. The values are the ones the compiler
 * resolved, so constants, string templates and escapes count as written. Whether a rule exists is checked by
 * {@code bal scan}, which knows the rules of the scan tool and analyzers it runs. An annotation without a reason gets
 * an internal hint for {@link AddReasonCodeAction}.
 *
 * @since 1.0.0
 */
class SuppressAnnotationValidator implements AnalysisTask<CompilationAnalysisContext> {

    private static final Pattern RULE_ID = Pattern.compile("^(ballerina|[A-Za-z0-9_.]+/[A-Za-z0-9_.]+):[1-9][0-9]*$");

    @Override
    public void perform(CompilationAnalysisContext context) {
        Package currentPackage = context.currentPackage();
        for (ModuleId moduleId : currentPackage.moduleIds()) {
            Module module = currentPackage.module(moduleId);
            SemanticModel semanticModel = context.compilation().getSemanticModel(moduleId);
            Stream.concat(module.documentIds().stream(), module.testDocumentIds().stream())
                    .map(module::document)
                    .forEach(document -> validateDocument(context, document, semanticModel));
        }
    }

    private static void validateDocument(CompilationAnalysisContext context, Document document,
                                         SemanticModel semanticModel) {
        Node root = document.syntaxTree().rootNode();
        if (!(root instanceof ModulePartNode modulePart)) {
            return;
        }
        Set<String> scanPrefixes = SuppressAttachment.scanPrefixes(modulePart);
        if (scanPrefixes.isEmpty()) {
            return;
        }
        // The annotations of a construct share its metadata, or the parameter, return type or body they are on
        Map<Node, ConstructSuppressions> byConstruct = new IdentityHashMap<>();
        modulePart.accept(new NodeVisitor() {
            @Override
            public void visit(AnnotationNode annotation) {
                if (SuppressAttachment.refersToSuppress(annotation, scanPrefixes)) {
                    Optional<SuppressAttachment> suppression = SuppressAttachment.resolve(semanticModel, annotation);
                    if (suppression.isPresent()) {
                        validateAnnotation(context, annotation, suppression.get(),
                                byConstruct.computeIfAbsent(annotation.parent(), key -> new ConstructSuppressions()));
                    } else {
                        report(context, annotation, DiagnosticCode.UNSUPPORTED_PLACEMENT);
                    }
                }
                // A construct inside another annotation's value can carry suppressions of its own
                annotation.annotValue().ifPresent(value -> value.accept(this));
            }
        });
    }

    private static void validateAnnotation(CompilationAnalysisContext context, AnnotationNode annotation,
                                           SuppressAttachment suppression, ConstructSuppressions construct) {
        if (suppression.rules().isEmpty()) {
            if (construct.blanket) {
                report(context, annotation, DiagnosticCode.BLANKET_ALREADY_SUPPRESSED);
            }
            construct.blanket = true;
        } else {
            Node rules = SuppressAttachment.fieldValue(annotation, RULES_FIELD).orElse(annotation);
            validateRules(context, suppression.rules().get(), rules, construct.rules);
        }
        if (suppression.reason().isEmpty()) {
            report(context, annotation, DiagnosticCode.MISSING_REASON);
        } else if (suppression.reason().get().isBlank()) {
            report(context, SuppressAttachment.fieldValue(annotation, REASON_FIELD).orElse(annotation),
                    DiagnosticCode.BLANK_REASON);
        }
    }

    private static void validateRules(CompilationAnalysisContext context, List<String> ruleIds, Node rules,
                                      Set<String> earlierRules) {
        if (ruleIds.isEmpty()) {
            report(context, rules, DiagnosticCode.EMPTY_RULE_LIST);
            return;
        }
        // A list written in place gets each diagnostic on its member; a constant gets them on the reference
        List<Node> members = rules instanceof ListConstructorExpressionNode list
                && list.expressions().size() == ruleIds.size() ? list.expressions().stream().toList() : null;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < ruleIds.size(); i++) {
            Node node = members == null ? rules : members.get(i);
            String id = ruleIds.get(i);
            if (!seen.add(id)) {
                report(context, node, DiagnosticCode.DUPLICATE_RULE, id);
            } else if (!RULE_ID.matcher(id).matches()) {
                report(context, node, DiagnosticCode.INVALID_RULE_FORMAT, id);
            } else if (earlierRules.contains(id)) {
                report(context, node, DiagnosticCode.RULE_ALREADY_SUPPRESSED, id);
            }
        }
        earlierRules.addAll(seen);
    }

    private static void report(CompilationAnalysisContext context, Node node, DiagnosticCode code, Object... args) {
        DiagnosticInfo info = new DiagnosticInfo(code.code(), code.messageFormat(), code.severity());
        context.reportDiagnostic(DiagnosticFactory.createDiagnostic(info, node.location(), args));
    }

    private static final class ConstructSuppressions {

        private final Set<String> rules = new HashSet<>();
        private boolean blanket;
    }
}
