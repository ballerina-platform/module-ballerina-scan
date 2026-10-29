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

import io.ballerina.projects.Package;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Tests the {@code @scan:Suppress} validation of the {@code ballerina/scan} compiler plugin.
 */
public class CompilerPluginTest {

    private static final Path RESOURCE_DIRECTORY = Path.of("src", "test", "resources", "ballerina_sources")
            .toAbsolutePath();
    private static final Path DISTRIBUTION_PATH = Path.of("..", "target", "ballerina-runtime").toAbsolutePath();
    private static final String INVALID_RULE_FORMAT = "invalid rule ID '%s': expected 'ballerina:<number>' or "
            + "'<org>/<package>:<number>'";
    private static final String UNSUPPORTED_PLACEMENT =
            "@scan:Suppress is not supported on this construct and is ignored by bal scan";

    @Test(description = "test well-formed suppressions and annotations of other modules are not reported")
    public void testValidSuppressions() {
        // Any error, such as an unresolved ballerina/scan import, would also leave the plugin nothing to report
        List<String> errors = loadPackage("valid_suppressions").getCompilation().diagnosticResult().errors().stream()
                .map(Diagnostic::toString).toList();
        Assert.assertEquals(errors, List.of());
        Assert.assertEquals(scanDiagnostics("valid_suppressions"), List.of());
    }

    @Test(description = "test malformed suppressions are reported in source and test files")
    public void testInvalidSuppressions() {
        Assert.assertEquals(scanDiagnostics("invalid_suppressions"), List.of(
                "ERROR aliased.bal:19 " + INVALID_RULE_FORMAT.formatted("http:8"),
                "ERROR main.bal:22 " + INVALID_RULE_FORMAT.formatted("http:1"),
                "ERROR main.bal:24 " + INVALID_RULE_FORMAT.formatted("ballerina:0"),
                "ERROR main.bal:24 " + INVALID_RULE_FORMAT.formatted("ballerina"),
                "ERROR main.bal:24 " + INVALID_RULE_FORMAT.formatted("acme:lint:1"),
                "WARNING main.bal:26 rule 'ballerina:1' is listed more than once",
                "WARNING main.bal:28 blank suppression reason; it is treated as absent",
                "ERROR main.bal:30 empty rule list; name the rules to suppress, or omit 'rules' to suppress "
                        + "every rule",
                "ERROR main.bal:32 " + INVALID_RULE_FORMAT.formatted("http:1"),
                "ERROR main.bal:36 " + INVALID_RULE_FORMAT.formatted("http:1"),
                "WARNING main.bal:41 rule 'ballerina:1' is already suppressed on this construct",
                "WARNING main.bal:43 blank suppression reason; it is treated as absent",
                "WARNING main.bal:46 every rule is already suppressed on this construct",
                "ERROR main.bal:52 " + INVALID_RULE_FORMAT.formatted("http:1"),
                "ERROR main.bal:65 " + INVALID_RULE_FORMAT.formatted("http:1"),
                "ERROR main.bal:76 " + INVALID_RULE_FORMAT.formatted("http:2"),
                "WARNING main.bal:78 " + UNSUPPORTED_PLACEMENT,
                "ERROR main.bal:80 " + INVALID_RULE_FORMAT.formatted("http:4"),
                "WARNING main.bal:83 " + UNSUPPORTED_PLACEMENT,
                "ERROR main.bal:87 " + INVALID_RULE_FORMAT.formatted("http:6"),
                "WARNING main.bal:90 " + UNSUPPORTED_PLACEMENT,
                "WARNING main.bal:92 rule 'ballerina:1' is listed more than once",
                "WARNING main.bal:94 rule 'ballerina:1' is listed more than once",
                "ERROR tests/main_test.bal:19 " + INVALID_RULE_FORMAT.formatted("http:1")));
    }

    @Test(description = "test a suppression without a reason gets an internal hint")
    public void testMissingReasonHints() {
        List<String> hints = loadPackage("valid_suppressions").getCompilation().diagnosticResult().diagnostics()
                .stream()
                .filter(diagnostic -> diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.INTERNAL)
                .map(CompilerPluginTest::describe)
                .sorted()
                .toList();
        Assert.assertEquals(hints, List.of("INTERNAL main.bal:29 a suppression reason can be added",
                "INTERNAL main.bal:37 a suppression reason can be added"));
    }

    private static List<String> scanDiagnostics(String packageName) {
        Package currentPackage = loadPackage(packageName);
        return currentPackage.getCompilation().diagnosticResult().diagnostics().stream()
                .filter(diagnostic -> diagnostic.diagnosticInfo().code().startsWith("SCAN_SUPPRESS_"))
                .filter(diagnostic -> diagnostic.diagnosticInfo().severity() != DiagnosticSeverity.INTERNAL)
                .sorted(Comparator.comparing((Diagnostic diagnostic) -> diagnostic.location().lineRange().fileName())
                        .thenComparing(diagnostic -> diagnostic.location().lineRange().startLine().line())
                        .thenComparing(diagnostic -> diagnostic.location().lineRange().startLine().offset()))
                .map(CompilerPluginTest::describe)
                .toList();
    }

    private static String describe(Diagnostic diagnostic) {
        LineRange lineRange = diagnostic.location().lineRange();
        return diagnostic.diagnosticInfo().severity() + " " + lineRange.fileName() + ":"
                + (lineRange.startLine().line() + 1) + " " + diagnostic.message();
    }

    static Package loadPackage(String packageName) {
        Environment environment = EnvironmentBuilder.getBuilder().setBallerinaHome(DISTRIBUTION_PATH).build();
        ProjectEnvironmentBuilder projectEnvironmentBuilder = ProjectEnvironmentBuilder.getBuilder(environment);
        return BuildProject.load(projectEnvironmentBuilder, RESOURCE_DIRECTORY.resolve(packageName)).currentPackage();
    }
}
