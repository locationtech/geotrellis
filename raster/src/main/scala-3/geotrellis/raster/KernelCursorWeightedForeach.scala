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


package geotrellis.raster.mapalgebra.focal

import geotrellis.macros.*

import scala.quoted.*

trait KernelCursorWeightedForeach {
  inline def foreachWithWeight(inline f: (Int, Int, Int) => Unit): Unit =
    ${ TileMacros.intForeach_impl('{ this.asInstanceOf[MacroIterableTile] }, 'f) }

  inline def foreachWithWeightDouble(inline f: (Int, Int, Double) => Unit): Unit =
    ${ TileMacros.doubleForeach_impl('{ this.asInstanceOf[MacroIterableTile] }, 'f) }
}
