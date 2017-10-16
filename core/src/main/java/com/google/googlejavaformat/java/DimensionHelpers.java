/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.googlejavaformat.java;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import org.openjdk.source.tree.AnnotatedTypeTree;
import org.openjdk.source.tree.AnnotationTree;
import org.openjdk.source.tree.ArrayTypeTree;
import org.openjdk.source.tree.Tree;
import org.openjdk.tools.javac.tree.JCTree;

/**
 * Utilities for working with array dimensions.
 *
 * <p>javac's parser does not preserve concrete syntax for mixed-notation arrays, so we have to
 * re-lex the input to extra it.
 *
 * <p>For example, {@code int [] a;} cannot be distinguished from {@code int [] a [];} in the AST.
 */
class DimensionHelpers {

  /** The array dimension specifiers (including any type annotations) associated with a type. */
  static class TypeWithDims {
    final Tree node;
    final ImmutableList<List<AnnotationTree>> dims;

    public TypeWithDims(Tree node, ImmutableList<List<AnnotationTree>> dims) {
      this.node = node;
      this.dims = dims;
    }
  }

  enum SortedDims {
    YES,
    NO
  }

  /** Returns a (possibly re-ordered) {@link TypeWithDims} for the given type. */
  static TypeWithDims extractDims(Tree node, SortedDims sorted) {
    Deque<List<AnnotationTree>> builder = new ArrayDeque<>();
    node = extractDims(builder, node);
    Iterable<List<AnnotationTree>> dims;
    if (sorted == SortedDims.YES) {
      dims = reorderBySourcePosition(builder);
    } else {
      dims = builder;
    }
    return new TypeWithDims(node, ImmutableList.copyOf(dims));
  }

  /**
   * Rotate the list of dimension specifiers until all dimensions with type annotations appear in
   * source order.
   *
   * <p>javac reorders dimension specifiers in method declarations with mixed-array notation, which
   * means that any type annotations don't appear in source order.
   *
   * <p>For example, the type of {@code int @A [] f() @B [] {}} is parsed as {@code @B [] @A []}.
   *
   * <p>This doesn't handle cases with un-annotated dimension specifiers, so the formatting logic
   * checks the token stream to figure out which side of the method name they appear on.
   */
  private static Iterable<List<AnnotationTree>> reorderBySourcePosition(
      Deque<List<AnnotationTree>> dims) {
    int lastAnnotation = -1;
    int lastPos = -1;
    int idx = 0;
    for (List<AnnotationTree> dim : dims) {
      if (!dim.isEmpty()) {
        int pos = ((JCTree) dim.get(0)).getStartPosition();
        if (pos < lastPos) {
          List<List<AnnotationTree>> list = new ArrayList<>(dims);
          Collections.rotate(list, -(lastAnnotation + 1));
          return list;
        }
        lastPos = pos;
        lastAnnotation = idx;
      }
      idx++;
    }
    return dims;
  }

  /**
   * Accumulates a flattened list of array dimensions specifiers with type annotations, and returns
   * the base type.
   *
   * <p>Given {@code int @A @B [][] @C []}, adds {@code [[@A, @B], [@C]]} to dims and returns {@code
   * int}.
   */
  private static Tree extractDims(Deque<List<AnnotationTree>> dims, Tree node) {
    switch (node.getKind()) {
      case ARRAY_TYPE:
        return extractDims(dims, ((ArrayTypeTree) node).getType());
      case ANNOTATED_TYPE:
        AnnotatedTypeTree annotatedTypeTree = (AnnotatedTypeTree) node;
        if (annotatedTypeTree.getUnderlyingType().getKind() != Tree.Kind.ARRAY_TYPE) {
          return node;
        }
        node = extractDims(dims, annotatedTypeTree.getUnderlyingType());
        dims.addFirst(ImmutableList.copyOf(annotatedTypeTree.getAnnotations()));
        return node;
      default:
        return node;
    }
  }
}
