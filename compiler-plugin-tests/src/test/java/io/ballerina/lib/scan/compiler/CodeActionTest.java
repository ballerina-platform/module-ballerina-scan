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

/**
 * Tests the code actions of the {@code ballerina/scan} compiler plugin.
 */
public class CodeActionTest {

    private static final Gson GSON = new Gson();
    private static final String ADD_REASON = "Add suppression reason";
    private static final String REMOVE_BLANK_REASON = "Remove blank suppression reason";

    @DataProvider
    public Object[][] codeActions() {
        return new Object[][]{
                {20, ADD_REASON, "    @scan:Suppress {reason: \"\"}"},
                {22, ADD_REASON, "    @scan:Suppress {reason: \"\"}"},
                {24, ADD_REASON, "    @scan:Suppress {rules: [\"ballerina:1\"], reason: \"\"}"},
                {26, REMOVE_BLANK_REASON, "    @scan:Suppress"},
                {28, REMOVE_BLANK_REASON, "    @scan:Suppress {rules: [\"ballerina:1\"]}"},
                {30, REMOVE_BLANK_REASON, "    @scan:Suppress {rules: [\"ballerina:1\"]}"}
        };
    }

    @Test(description = "test a code action rewrites the annotation it is offered on", dataProvider = "codeActions")
    public void testCodeAction(int line, String title, String expected) {
        Assert.assertEquals(applyCodeAction(line, title).split("\\R")[line - 1], expected);
    }

    @Test(description = "test a suppression with a reason gets no code action")
    public void testNoCodeActionWithReason() {
        Assert.assertEquals(codeActions(CompilerPluginTest.loadPackage("valid_suppressions"), 31), List.of());
    }

    private static String applyCodeAction(int line, String title) {
        Package currentPackage = CompilerPluginTest.loadPackage("code_actions");
        List<CodeActionInfo> codeActions = codeActions(currentPackage, line);
        Assert.assertEquals(codeActions.stream().map(CodeActionInfo::getTitle).toList(), List.of(title));

        // Arguments reach execution through JSON, as they do from the language server
        CodeActionInfo codeAction = codeActions.get(0);
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
