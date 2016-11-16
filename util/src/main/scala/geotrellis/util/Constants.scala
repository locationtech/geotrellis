/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.util

object Constants {
  /**
   * Defines machine epsilon for double-precision floating point ops.  This is,
   * roughly speaking, the minimum distance between distinct floating point numbers.
   * Double values closer than DOUBLE_EPSILON should be considered identical.
   */
  val DOUBLE_EPSILON = 1.11e-16

  /**
   * Defines machine epsilon for single-precision floating point ops.  This is,
   * roughly speaking, the minimum distance between distinct floating point numbers.
   * Float values closer than FLOAT_EPSILON should be considered identical.
   */
  val FLOAT_EPSILON = 5.96e-8
}
