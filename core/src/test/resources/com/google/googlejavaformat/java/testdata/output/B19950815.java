/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

class B19950815 {
  void m() {
    checkArgument(
        truncationLength >= 0,
        "maxLength (%s) must be >= length of the truncation indicator (%s)",
        maxLength,
        truncationIndicator.length());
  }

  private String finishCollapseFrom(
      CharSequence sequence,
      int start,
      int end,
      char replacement,
      StringBuilder builder,
      boolean inMatchingGroup) {}
}
