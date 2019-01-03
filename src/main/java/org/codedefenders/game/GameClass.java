/**
 * Copyright (C) 2016-2018 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.game;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.Range;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.singleplayer.NoDummyGameException;
import org.codedefenders.model.Dependency;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.analysis.ClassCodeAnalyser;
import org.codedefenders.util.analysis.CodeAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class represents a class under test. Games will be played with this class by
 * modifying it or creating test cases for it.
 */
public class GameClass {

    private static final Logger logger = LoggerFactory.getLogger(GameClass.class);

    private int id;
    private String name; // fully qualified name
    private String alias;
    private String javaFile;
    private String classFile;

    private boolean isMockingEnabled;

    private Set<String> additionalImports = new HashSet<>();
    // Store begin and end line which corresponds to uncoverable non-initializad fields
    private List<Integer> linesOfCompileTimeConstants = new ArrayList<>();
    private List<Integer> linesOfNonCoverableCode = new ArrayList<>();
    private List<Integer> nonInitializedFields = new ArrayList<>();

    private List<Range<Integer>> linesOfMethods = new ArrayList<>();
    private List<Range<Integer>> linesOfMethodSignatures = new ArrayList<>();
    private List<Range<Integer>> linesOfClosingBrackets = new ArrayList<>();

    /**
     * The source code of this Java class. Used as an instance attribute so the file content only needs to be read once.
     */
    private String sourceCode;

    public GameClass(String name, String alias, String jFile, String cFile) {
        this(name, alias, jFile, cFile, false);
    }

    public GameClass(int id, String name, String alias, String jFile, String cFile, boolean isMockingEnabled) {
        this(name, alias, jFile, cFile, isMockingEnabled);
        this.id = id;
    }

    public GameClass(String name, String alias, String jFile, String cFile, boolean isMockingEnabled) {
        this.name = name;
        this.alias = alias;
        this.javaFile = jFile;
        this.classFile = cFile;
        this.isMockingEnabled = isMockingEnabled;

        visitCode();
    }

    private void visitCode() {
        final CodeAnalysisResult visit = ClassCodeAnalyser.visitCode(this.name, this.getSourceCode());
        this.additionalImports.addAll(visit.getAdditionalImports());
        this.linesOfCompileTimeConstants.addAll(visit.getCompileTimeConstants());
        this.linesOfNonCoverableCode.addAll(visit.getNonCoverableCode());
        this.nonInitializedFields.addAll(visit.getNonInitializedFields());
        this.linesOfMethods.addAll(visit.getMethods());
        this.linesOfMethodSignatures.addAll(visit.getMethodSignatures());
        this.linesOfClosingBrackets.addAll(visit.getClosingBrackets());
    }

    /**
     * Calls {@link GameClassDAO} to insert this {@link GameClass} instance into the database.
     * <p>
     * Updates the identifier of the called instance.
     *
     * @return {@code true} if insertion was successful, {@code false} otherwise.
     */
    public boolean insert() {
        try {
            this.id = GameClassDAO.storeClass(this);
            return true;
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store game class to database.", e);
            return false;
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getJavaFile() {
        return javaFile;
    }

    public String getClassFile() {
        return classFile;
    }

    public boolean isMockingEnabled() {
        return this.isMockingEnabled;
    }

    public String getBaseName() {
        String[] tokens = name.split("\\.");
        return tokens[tokens.length - 1];
    }

    public String getPackage() {
        return (name.contains(".")) ? name.substring(0, name.lastIndexOf('.')) : "";
    }

    public String getAlias() {
        return alias;
    }

    public String getSourceCode() {
        if (sourceCode != null) {
            return sourceCode;
        }
        return sourceCode = FileUtils.readJavaFileWithDefault(Paths.get(javaFile));
    }

    public String getAsHTMLEscapedString() {
        return StringEscapeUtils.escapeHtml(getSourceCode());
    }

    /**
     * Returns a mapping of dependency class names and its HTML escaped class content.
     */
    public Map<String, String> getHTMLEscapedDependencyCode() {
        return GameClassDAO.getMappedDependenciesForClassId(id)
                .stream()
                .map(Dependency::getJavaFile)
                .map(Paths::get)
                .collect(Collectors.toMap(FileUtils::extractFileNameNoExtension, FileUtils::readJavaFileWithDefaultHTMLEscaped));
    }

    /**
     * Generates and returns a template for a JUnit test.
     * <p>
     * Be aware that this template is not HTML escaped.
     * Please use {@link #getHTMLEscapedTestTemplate()}.
     *
     * @return template for a JUnit test as a {@link String}.
     * @see #getHTMLEscapedTestTemplate()
     */
    private String getTestTemplate() {
        final StringBuilder bob = new StringBuilder();
        final String classPackage = getPackage();
        if (!classPackage.isEmpty()) {
            bob.append(String.format("package %s;\n\n", classPackage));
        }

        bob.append("import org.junit.*;\n");

        for (String additionalImport : this.additionalImports) {
            // Additional import are already in the form of 'import X.Y.Z;\n'
            bob.append(additionalImport); // no \n required
        }
        bob.append("\n");

        bob.append("import static org.junit.Assert.*;\n");
        if (this.isMockingEnabled) {
            bob.append("import static org.mockito.Mockito.*;\n");
        }
        bob.append("\n");

        bob.append(String.format("public class Test%s {\n", getBaseName()))
                .append("    @Test(timeout = 4000)\n")
                .append("    public void test() throws Throwable {\n")
                .append("        // test here!\n")
                .append("    }\n")
                .append("}");
        return bob.toString();
    }

    /**
     * @return a HTML escaped test template for a Junit Test as a {@link String}.
     */
    public String getHTMLEscapedTestTemplate() {
        return StringEscapeUtils.escapeHtml(getTestTemplate());
    }

    /**
     * @return the first editable line of this class test template.
     * @see #getTestTemplate()
     */
    public Integer getTestTemplateFirstEditLine() {
        int out = 8 + this.additionalImports.size();
        if (!getPackage().isEmpty()) {
            out += 2;
        }
        if (this.isMockingEnabled) {
            out++;
        }
        return out;
    }

    public DuelGame getDummyGame() throws NoDummyGameException {
        DuelGame dg = DatabaseAccess.getAiDummyGameForClass(this.getId());
        if (dg == null) {
            throw new NoDummyGameException("No dummy game found.");
        }
        return dg;
    }

    /**
     * @return All lines of not initialized fields as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getNonInitializedFields() {
        Collections.sort(this.nonInitializedFields);
        return Collections.unmodifiableList(this.nonInitializedFields);
    }

    /**
     * @return All lines of method signatures as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getMethodSignatures() {
        return this.linesOfMethodSignatures
                .stream()
                .flatMap(range -> IntStream.rangeClosed(range.getMinimum(), range.getMaximum()).boxed())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * @return All lines which are not coverable as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getNonCoverableCode() {
        return linesOfNonCoverableCode;
    }

    /**
     * Return the lines which correspond to Compile Time Constants. Mutation of those lines requires tests
     * to be recompiled against the mutant.
     *
     * @return All lines of compile time constants as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getCompileTimeConstants() {
        return Collections.unmodifiableList(linesOfCompileTimeConstants);
    }

    /**
     * Return the lines of the method signature for the method which contains a given line.
     *
     * @param line the line the method signature is returned for.
     * @return All lines of the method signature a given line resides as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getMethodSignaturesForLine(Integer line) {
        final List<Integer> collect = linesOfMethods
                .stream()
                .filter(method -> method.contains(line))
                .flatMap(methodRange -> linesOfMethodSignatures.stream()
                        .filter(methodSignature -> methodSignature.contains(methodRange.getMinimum()))
                        .flatMap(msRange -> IntStream.rangeClosed(msRange.getMinimum(), msRange.getMaximum()).boxed()))
                .collect(Collectors.toList());
        return Collections.unmodifiableList(collect);
    }

    /**
     * Return the lines of closing brackets from the if-statements which contain a given line.
     *
     * @param line the line closing brackets are returned for.
     * @return All lines of closing brackets from if-statements a given line resides as a {@link List} of
     * {@link Integer Integers}. Can be empty, but never {@code null}.
     */
    public List<Integer> getClosingBracketForLine(Integer line) {
        final List<Integer> collect = linesOfClosingBrackets
                .stream()
                .filter(integerRange -> integerRange.contains(line))
                .map(Range::getMaximum)
                .sorted()
                .collect(Collectors.toList());
        return Collections.unmodifiableList(collect);
    }

    @Override
    public String toString() {
        return "[id=" + id + ",name=" + name + ",alias=" + alias + "]";
    }
}