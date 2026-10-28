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

import com.google.gson.Gson;
import io.ballerina.projects.CodeActionManager;
import io.ballerina.projects.Document;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionContextImpl;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContextImpl;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tests the code actions of the {@code ballerina/scan} compiler plugin.
 */
public class CodeActionTest {

    private static final Gson GSON = new Gson();
    private static final String ADD_REASON = "Add suppression reason";
    private static final String REMOVE_BLANK_REASON = "Remove blank suppression reason";
    private static final String REMOVE_DUPLICATE_RULE = "Remove duplicate rule";
    private static final String REMOVE_SUPPRESSION = "Remove suppression";
    private static final String SUPPRESS_ALL_RULES = "Suppress all rules on this construct";
    private static final String DUPLICATE_RULE_REASON = ", reason: \"Fixed by the parser rewrite\"}";

    @DataProvider
    public Object[][] codeActions() {
        return new Object[][]{
                {20, ADD_REASON, "    @scan:Suppress {reason: \"\"}"},
                {22, ADD_REASON, "    @scan:Suppress {reason: \"\"}"},
                {24, ADD_REASON, "    @scan:Suppress {rules: [\"ballerina:1\"], reason: \"\"}"},
                {26, REMOVE_BLANK_REASON, "    @scan:Suppress"},
                {28, REMOVE_BLANK_REASON, "    @scan:Suppress {rules: [\"ballerina:1\"]}"},
                {30, REMOVE_BLANK_REASON, "    @scan:Suppress {rules: [\"ballerina:1\"]}"},
                {51, REMOVE_DUPLICATE_RULE,
                        "    @scan:Suppress {rules: [\"ballerina:1\", \"ballerina:2\"]" + DUPLICATE_RULE_REASON},
                {53, REMOVE_DUPLICATE_RULE, "    @scan:Suppress {rules: [\"ballerina:1\"]" + DUPLICATE_RULE_REASON},
                {85, REMOVE_SUPPRESSION, "    int s = 19;"},
                {85, SUPPRESS_ALL_RULES, "    @scan:Suppress"},
                {87, SUPPRESS_ALL_RULES, "    @scan:Suppress {reason: \"Fixed by the parser rewrite\"}"},
                {89, REMOVE_SUPPRESSION, "    int u = 21;"},
                {99, REMOVE_DUPLICATE_RULE,
                        "    @scan:Suppress {r\\u{75}les: [\"ballerina:1\"]" + DUPLICATE_RULE_REASON}
        };
    }

    @Test(description = "test a code action rewrites the annotation it is offered on", dataProvider = "codeActions")
    public void testCodeAction(int line, String title, String expected) {
        Assert.assertEquals(applyCodeAction(line, title).split("\\R")[line - 1], expected);
    }

    @Test(description = "test removing a blank reason keeps the comments of the other fields")
    public void testRemoveBlankReasonKeepsComments() {
        List<String> first = List.of(applyCodeAction(33, REMOVE_BLANK_REASON).split("\\R"));
        Assert.assertEquals(first.subList(31, 35), List.of("    @scan:Suppress {",
                "        // Accepted until the parser is replaced", "        rules: [\"ballerina:1\"]", "    }"));
        List<String> last = List.of(applyCodeAction(40, REMOVE_BLANK_REASON).split("\\R"));
        Assert.assertEquals(last.subList(37, 40), List.of("    @scan:Suppress {",
                "        rules: [\"ballerina:1\"] // Accepted until the parser is replaced", "    }"));
        List<String> closed = List.of(applyCodeAction(44, REMOVE_BLANK_REASON).split("\\R"));
        Assert.assertEquals(closed.subList(42, 45), List.of(
                "    @scan:Suppress {rules: [\"ballerina:1\"] // Accepted until the parser is replaced",
                "        }", "    int i = 9;"));
        List<String> trailing = List.of(applyCodeAction(48, REMOVE_BLANK_REASON).split("\\R"));
        Assert.assertEquals(trailing.subList(45, 49), List.of("    @scan:Suppress {",
                "        rules: [\"ballerina:1\"]", "    }", "    int j = 10;"));
    }

    @Test(description = "test removing a duplicate rule keeps the lines and comments of the other rules")
    public void testRemoveDuplicateRuleKeepsComments() {
        List<String> ownLine = List.of(applyCodeAction(59, REMOVE_DUPLICATE_RULE).split("\\R"));
        Assert.assertEquals(ownLine.subList(54, 60), List.of("    @scan:Suppress {", "        rules: [",
                "            \"ballerina:1\",", "            \"ballerina:2\"", "        ],",
                "        reason: \"Fixed by the parser rewrite\""));
        List<String> last = List.of(applyCodeAction(68, REMOVE_DUPLICATE_RULE).split("\\R"));
        Assert.assertEquals(last.subList(65, 68), List.of("        rules: [",
                "            \"ballerina:1\" // Accepted until the parser is replaced", "        ],"));
        List<String> closed = List.of(applyCodeAction(74, REMOVE_DUPLICATE_RULE).split("\\R"));
        Assert.assertEquals(closed.subList(72, 74), List.of(
                "    @scan:Suppress {rules: [\"ballerina:1\"]" + DUPLICATE_RULE_REASON, "    int o = 15;"));
        List<String> commented = List.of(applyCodeAction(79, REMOVE_DUPLICATE_RULE).split("\\R"));
        Assert.assertEquals(commented.subList(77, 80), List.of(
                "    @scan:Suppress {rules: [\"ballerina:1\", // Accepted until the parser is replaced",
                "            \"ballerina:2\"]" + DUPLICATE_RULE_REASON, "    int q = 17;"));
        List<String> lineEnd = List.of(applyCodeAction(81, REMOVE_DUPLICATE_RULE).split("\\R"));
        Assert.assertEquals(lineEnd.subList(80, 83), List.of("    @scan:Suppress {rules: [\"ballerina:1\",",
                "            \"ballerina:2\"]" + DUPLICATE_RULE_REASON, "    int r = 18;"));
    }

    @Test(description = "test a duplicate rule from a constant gets no code action")
    public void testNoCodeActionForDuplicateInConstant() {
        Package currentPackage = CompilerPluginTest.loadPackage("code_actions");
        Assert.assertTrue(currentPackage.getCompilation().diagnosticResult().diagnostics().stream()
                .anyMatch(diagnostic -> isOnLine(diagnostic, 75)
                        && diagnostic.diagnosticInfo().code().equals("SCAN_SUPPRESS_105")));
        Assert.assertEquals(codeActions(currentPackage, 76), List.of());
    }

    @Test(description = "test an empty rule list can be removed with its suppression only when it gives no reason")
    public void testEmptyRuleListCodeActions() {
        Package currentPackage = CompilerPluginTest.loadPackage("code_actions");
        Assert.assertEquals(titles(currentPackage, 85), Set.of(ADD_REASON, REMOVE_SUPPRESSION, SUPPRESS_ALL_RULES));
        Assert.assertEquals(titles(currentPackage, 87), Set.of(SUPPRESS_ALL_RULES));
        Assert.assertEquals(titles(currentPackage, 89),
                Set.of(REMOVE_BLANK_REASON, REMOVE_SUPPRESSION, SUPPRESS_ALL_RULES));
        List<String> removed = List.of(applyCodeAction(85, REMOVE_SUPPRESSION).split("\\R"));
        Assert.assertEquals(removed.subList(83, 85), List.of("    // Placeholder until the rules are known",
                "    int s = 19;"), "the comment above the annotation stays");
    }

    @Test(description = "test removing a suppression keeps what shares its lines")
    public void testRemoveSuppressionKeepsItsLines() {
        List<String> afterAnnotation = List.of(applyCodeAction(90, REMOVE_SUPPRESSION).split("\\R"));
        Assert.assertEquals(afterAnnotation.subList(89, 91), List.of("    @display {label: \"v\"}", "    int v = 22;"));
        List<String> withComment = List.of(applyCodeAction(92, REMOVE_SUPPRESSION).split("\\R"));
        Assert.assertEquals(withComment.subList(91, 93), List.of("    // TODO pick the rules", "    int w = 23;"));
    }

    @Test(description = "test removing the first field keeps a comment above the next field on its own line")
    public void testRemoveFirstFieldBeforeComment() {
        List<String> lines = List.of(applyCodeAction(94, REMOVE_BLANK_REASON).split("\\R"));
        Assert.assertEquals(lines.subList(93, 98), List.of("    @scan:Suppress {", "        // SEC-1042",
                "        rules: [\"ballerina:1\"]", "    }", "    int x = 24;"));
    }

    @Test(description = "test a suppression with a reason gets no code action")
    public void testNoCodeActionWithReason() {
        Assert.assertEquals(codeActions(CompilerPluginTest.loadPackage("valid_suppressions"), 31), List.of());
    }

    private static String applyCodeAction(int line, String title) {
        Package currentPackage = CompilerPluginTest.loadPackage("code_actions");
        CodeActionInfo codeAction = codeActions(currentPackage, line).stream()
                .filter(info -> info.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no '" + title + "' code action on line " + line));

        // Arguments reach execution through JSON, as they do from the language server
        List<CodeActionArgument> arguments = codeAction.getArguments().stream()
                .map(argument -> CodeActionArgument.from(GSON.toJsonTree(argument)))
                .toList();
        Document document = mainDocument(currentPackage);
        PackageCompilation compilation = currentPackage.getCompilation();
        List<DocumentEdit> edits = compilation.getCodeActionManager().executeCodeAction(codeAction.getProviderName(),
                CodeActionExecutionContextImpl.from(filePath(currentPackage).toUri().toString(),
                        filePath(currentPackage), null, document,
                        compilation.getSemanticModel(document.documentId().moduleId()), arguments));
        Assert.assertEquals(edits.size(), 1);
        return edits.get(0).getModifiedSyntaxTree().toSourceCode();
    }

    private static Set<String> titles(Package currentPackage, int line) {
        return codeActions(currentPackage, line).stream().map(CodeActionInfo::getTitle).collect(Collectors.toSet());
    }

    private static List<CodeActionInfo> codeActions(Package currentPackage, int line) {
        PackageCompilation compilation = currentPackage.getCompilation();
        CodeActionManager codeActionManager = compilation.getCodeActionManager();
        Document document = mainDocument(currentPackage);
        LinePosition cursor = LinePosition.from(line - 1, 6);
        return compilation.diagnosticResult().diagnostics().stream()
                .filter(diagnostic -> diagnostic.location().lineRange().fileName().equals("main.bal"))
                .filter(diagnostic -> isOnLine(diagnostic, line - 1))
                .flatMap(diagnostic -> codeActionManager.codeActions(CodeActionContextImpl.from(
                        filePath(currentPackage).toUri().toString(), filePath(currentPackage), cursor, document,
                        compilation.getSemanticModel(document.documentId().moduleId()), diagnostic))
                        .getCodeActions().stream())
                .toList();
    }

    private static boolean isOnLine(Diagnostic diagnostic, int line) {
        return diagnostic.location().lineRange().startLine().line() <= line
                && diagnostic.location().lineRange().endLine().line() >= line;
    }

    private static Document mainDocument(Package currentPackage) {
        return currentPackage.getDefaultModule().documentIds().stream()
                .map(currentPackage.getDefaultModule()::document)
                .filter(document -> document.name().equals("main.bal"))
                .findFirst()
                .orElseThrow();
    }

    private static Path filePath(Package currentPackage) {
        return currentPackage.project().sourceRoot().resolve("main.bal");
    }
}
