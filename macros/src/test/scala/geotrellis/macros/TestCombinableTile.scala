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

package geotrellis.macros

class TestCombinableTile
    extends MacroCombinableMultibandTile[Int]
    with MacroCombineFunctions[Int, TestCombinableTile] {

  def combineIntTileCombiner(combiner: IntTileCombiner3): Int = combiner(1, 1, 1)
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner3): Int = combiner(1.0, 1.0, 1.0).toInt
  def combineIntTileCombiner(combiner: IntTileCombiner4): Int = combiner(1, 1, 1, 1)
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner4): Int = combiner(1.0, 1.0, 1.0, 1.0).toInt
  def combineIntTileCombiner(combiner: IntTileCombiner5): Int = combiner(1, 1, 1, 1, 1)
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner5): Int = combiner(1.0, 1.0, 1.0, 1.0, 1.0).toInt
  def combineIntTileCombiner(combiner: IntTileCombiner6): Int = combiner(1, 1, 1, 1, 1, 1)
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner6): Int = combiner(1.0, 1.0, 1.0, 1.0, 1.0, 1.0).toInt
  def combineIntTileCombiner(combiner: IntTileCombiner7): Int = combiner(1, 1, 1, 1, 1, 1, 1)
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner7): Int = combiner(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0).toInt
  def combineIntTileCombiner(combiner: IntTileCombiner8): Int = combiner(1, 1, 1, 1, 1, 1, 1, 1)
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner8): Int = combiner(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0).toInt
  def combineIntTileCombiner(combiner: IntTileCombiner9): Int = combiner(1, 1, 1, 1, 1, 1, 1, 1, 1)
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner9): Int = combiner(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0).toInt
  def combineIntTileCombiner(combiner: IntTileCombiner10): Int = combiner(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner10): Int = combiner(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0).toInt
}
