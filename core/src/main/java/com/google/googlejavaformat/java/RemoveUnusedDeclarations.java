package com.google.googlejavaformat.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;

import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Removes unused declarations from Java source code, including:
 * - Redundant modifiers in interfaces (public, static, final, abstract)
 * - Redundant modifiers in classes, enums, and annotations
 * - Redundant final modifiers on method parameters (preserved now)
 */
public class RemoveUnusedDeclarations {
    public static String removeUnusedDeclarations(String source) throws FormatterException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        var task = JavacTool.create().getTask(
                null,
                new JavacFileManager(new Context(), true, null),
                diagnostics,
                ImmutableList.of("-Xlint:-processing"),
                null,
                ImmutableList.of((JavaFileObject) new SimpleJavaFileObject(URI.create("source"),
                        JavaFileObject.Kind.SOURCE) {
                    @Override
                    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                        return source;
                    }
                }));

        try {
            Iterable<? extends CompilationUnitTree> units = task.parse();
            if (!units.iterator().hasNext()) {
                throw new FormatterException("No compilation units found");
            }

            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    throw new FormatterException("Syntax error in source: " + diagnostic.getMessage(null));
                }
            }

            var scanner = new UnusedDeclarationScanner(task);
            scanner.scan(units.iterator().next(), null);

            return applyReplacements(source, scanner.getReplacements());
        } catch (IOException e) {
            throw new FormatterException("Error processing source file: " + e.getMessage());
        }
    }

    private static class UnusedDeclarationScanner extends TreePathScanner<Void, Void> {
        private final RangeMap<Integer, String> replacements = TreeRangeMap.create();
        private final SourcePositions sourcePositions;
        private final Trees trees;

        private static final ImmutableList<Modifier> CANONICAL_MODIFIER_ORDER = ImmutableList.of(
                Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE,
                Modifier.ABSTRACT, Modifier.STATIC, Modifier.FINAL,
                Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.SYNCHRONIZED,
                Modifier.NATIVE, Modifier.STRICTFP
        );

        private UnusedDeclarationScanner(JavacTask task) {
            this.sourcePositions = Trees.instance(task).getSourcePositions();
            this.trees = Trees.instance(task);
        }

        public RangeMap<Integer, String> getReplacements() {
            return replacements;
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            var parentPath = getCurrentPath().getParentPath();
            if (node.getKind() == Tree.Kind.INTERFACE) {
                checkForRedundantModifiers(node, Set.of(Modifier.PUBLIC, Modifier.ABSTRACT));
            } else if ((parentPath != null ? parentPath.getLeaf().getKind() : null) == Tree.Kind.INTERFACE) {
                checkForRedundantModifiers(node, Set.of(Modifier.PUBLIC, Modifier.STATIC));
            } else if (node.getKind() == Tree.Kind.ANNOTATION_TYPE) {
                checkForRedundantModifiers(node, Set.of(Modifier.ABSTRACT));
            } else {
                checkForRedundantModifiers(node, Set.of()); // Always sort
            }

            return super.visitClass(node, unused);
        }

        @Override
        public Void visitMethod(MethodTree node, Void unused) {
            var parentPath = getCurrentPath().getParentPath();
            var parentKind = parentPath != null ? parentPath.getLeaf().getKind() : null;

            if (parentKind == Tree.Kind.INTERFACE) {
                if (!node.getModifiers().getFlags().contains(Modifier.DEFAULT) &&
                        !node.getModifiers().getFlags().contains(Modifier.STATIC)) {
                    checkForRedundantModifiers(node, Set.of(Modifier.PUBLIC, Modifier.ABSTRACT));
                } else {
                    checkForRedundantModifiers(node, Set.of());
                }
            } else if (parentKind == Tree.Kind.ANNOTATION_TYPE) {
                checkForRedundantModifiers(node, Set.of(Modifier.ABSTRACT));
            } else {
                checkForRedundantModifiers(node, Set.of()); // Always sort
            }

            return super.visitMethod(node, unused);
        }

        @Override
        public Void visitVariable(VariableTree node, Void unused) {
            var parentPath = getCurrentPath().getParentPath();
            var parentKind = parentPath != null ? parentPath.getLeaf().getKind() : null;

            if (node.getKind() == Tree.Kind.ENUM) {
                return super.visitVariable(node, unused);
            }

            if (parentKind == Tree.Kind.INTERFACE || parentKind == Tree.Kind.ANNOTATION_TYPE) {
                checkForRedundantModifiers(node, Set.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL));
            } else {
                checkForRedundantModifiers(node, Set.of()); // Always sort
            }

            return super.visitVariable(node, unused);
        }

        private void checkForRedundantModifiers(Tree node, Set<Modifier> redundantModifiers) {
            var modifiers = getModifiers(node);
            if (modifiers == null) return;
            try {
                addReplacementForModifiers(node, new LinkedHashSet<>(modifiers.getFlags()).stream()
                        .filter(redundantModifiers::contains)
                        .collect(Collectors.toSet()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private ModifiersTree getModifiers(Tree node) {
            if (node instanceof ClassTree) return ((ClassTree) node).getModifiers();
            if (node instanceof MethodTree) return ((MethodTree) node).getModifiers();
            if (node instanceof VariableTree) return ((VariableTree) node).getModifiers();
            return null;
        }

        private void addReplacementForModifiers(Tree node, Set<Modifier> toRemove) throws IOException {
            TreePath path = trees.getPath(getCurrentPath().getCompilationUnit(), node);
            if (path == null) return;

            CompilationUnitTree unit = path.getCompilationUnit();
            String source = unit.getSourceFile().getCharContent(true).toString();

            ModifiersTree modifiers = getModifiers(node);
            if (modifiers == null) return;

            long modifiersStart = sourcePositions.getStartPosition(unit, modifiers);
            long modifiersEnd = sourcePositions.getEndPosition(unit, modifiers);
            if (modifiersStart == -1 || modifiersEnd == -1) return;

            String newModifiersText = modifiers.getFlags().stream()
                    .filter(m -> !toRemove.contains(m))
                    .collect(Collectors.toCollection(LinkedHashSet::new)).stream()
                    .sorted(Comparator.comparingInt(mod -> {
                        int idx = CANONICAL_MODIFIER_ORDER.indexOf(mod);
                        return idx == -1 ? Integer.MAX_VALUE : idx;
                    }))
                    .map(Modifier::toString)
                    .collect(Collectors.joining(" "));

            long annotationsEnd = modifiersStart;
            for (AnnotationTree annotation : modifiers.getAnnotations()) {
                long end = sourcePositions.getEndPosition(unit, annotation);
                if (end > annotationsEnd) annotationsEnd = end;
            }

            int effectiveStart = (int) annotationsEnd;
            while (effectiveStart < modifiersEnd && Character.isWhitespace(source.charAt(effectiveStart))) {
                effectiveStart++;
            }

            String current = source.substring(effectiveStart, (int) modifiersEnd);
            if (!newModifiersText.trim().equals(current.trim())) {
                int globalEnd = (int) modifiersEnd;
                if (newModifiersText.isEmpty()) {
                    while (globalEnd < source.length() && Character.isWhitespace(source.charAt(globalEnd))) {
                        globalEnd++;
                    }
                }
                replacements.put(Range.closedOpen(effectiveStart, globalEnd), newModifiersText);
            }
        }
    }

    private static String applyReplacements(String source, RangeMap<Integer, String> replacements) {
        StringBuilder sb = new StringBuilder(source);
        for (Map.Entry<Range<Integer>, String> entry : replacements.asDescendingMapOfRanges().entrySet()) {
            Range<Integer> range = entry.getKey();
            sb.replace(range.lowerEndpoint(), range.upperEndpoint(), entry.getValue());
        }
        return sb.toString();
    }
}
