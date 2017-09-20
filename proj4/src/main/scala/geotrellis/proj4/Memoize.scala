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

package geotrellis.proj4

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Manuri Perera
 */
@deprecated("This will be removed in 2.0", "1.2")
class Memoize[T, R](f: T => R) extends (T => R) {
  val map: ConcurrentHashMap[T, R] = new ConcurrentHashMap()

  def apply(x: T): R = {
    if (map.contains(x)) map.get(x)
    else {
      val y = f(x)
      map.put(x, y)
      y
    }
  }
}

