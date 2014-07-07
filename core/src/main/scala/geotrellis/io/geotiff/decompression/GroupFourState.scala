/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.io.geotiff.decompression

import monocle.Macro._
import monocle.syntax._

import geotrellis.io.geotiff._

import geotrellis.io.geotiff.decompression.HuffmanColor._

case class GroupFourState(
  white: Boolean,
  width: Int,
  a0: Int,
  b1: Int,
  runLength: Int,
  longRun: Int,
  referenceIndex: Int,
  reference: Array[Int]
) {

  import GroupFourStateLenses._

  def detectB1(rowIndex: Int): GroupFourState = if (rowIndex != 0) {
    var state = this
    while (state.b1 <= state.a0 && state.b1 < state.width) {
      val r = state.reference(state.referenceIndex) + state.reference(
        state.referenceIndex + 1)

      if (r == 0) state = state |-> b1Lens set (state.width)
      else state = state |-> b1Lens modify (_ + r)

      if (state.referenceIndex + 2 < state.reference.size)
        state = state |-> referenceIndexLens modify (_ + 2)
      else
        throw new MalformedGeoTiffException("Bad detection of b1!")
    }

    state
  } else this

  def decodePass(rowIndex: Int): GroupFourState = {
    var state = detectB1(rowIndex)
    state = state |-> b1Lens modify (_ + state.reference(state.referenceIndex))
    state = state |-> referenceIndexLens modify (_ + 1)
    state = state |-> runLengthLens modify (_ + state.b1 - state.a0)
    state = state |-> a0Lens set (state.b1)
    state = state |-> b1Lens modify (_ + state.reference(state.referenceIndex))
    state |-> referenceIndexLens modify (_ + 1)
  }

  def getColor() = if (white) White else Black

}

object GroupFourState {

  def apply(width: Int): GroupFourState = {
    val groupFourState = GroupFourState(
      true,
      width,
      0,
      width,
      0,
      0,
      1,
      Array.ofDim[Int](width + 1)
    )

    groupFourState.reference(0) = width
    groupFourState.reference(1) = 0

    groupFourState
  }

}

object GroupFourStateLenses {

  val whiteLens = mkLens[GroupFourState, Boolean]("white")
  val widthLens = mkLens[GroupFourState, Int]("width")
  val a0Lens = mkLens[GroupFourState, Int]("a0")
  val b1Lens = mkLens[GroupFourState, Int]("b1")
  val runLengthLens = mkLens[GroupFourState, Int]("runLength")
  val longRunLens = mkLens[GroupFourState, Int]("longRun")
  val referenceIndexLens = mkLens[GroupFourState, Int]("referenceIndex")
  val referenceLens = mkLens[GroupFourState, Array[Int]]("reference")

}
