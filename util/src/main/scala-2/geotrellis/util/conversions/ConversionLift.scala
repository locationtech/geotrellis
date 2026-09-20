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

package geotrellis.util.conversions

/**
 * Scala 2 applies implicit `A => B` values as implicit conversions on its own, so there is
 * nothing to lift. This object exists so the import in shared sources resolves on both Scala
 * versions; see the Scala 3 counterpart in `src/main/scala-3`.
 */
object ConversionLift
