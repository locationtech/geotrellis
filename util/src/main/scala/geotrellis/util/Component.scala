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

/** Defines an object that can be used as a lens
  * into a component C of some type T.
  */
trait Component[T, C] extends GetComponent[T, C] with SetComponent[T, C]

object Component {
  def apply[T, C](_get: T => C, _set: (T, C) => T): Component[T, C] =
    new Component[T, C] {
      val get = _get
      val set = _set
    }
}
