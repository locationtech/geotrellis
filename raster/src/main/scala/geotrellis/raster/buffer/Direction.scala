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

package geotrellis.raster.buffer

sealed trait Direction

object Direction {
  case object Center extends Direction
  case object Top extends Direction
  case object TopRight extends Direction
  case object Right extends Direction
  case object BottomRight extends Direction
  case object Bottom extends Direction
  case object BottomLeft extends Direction
  case object Left extends Direction
  case object TopLeft extends Direction

  // NOTE: This object is nested to facilitate the common pattern of calling
  // `import geotrellis.layers.buffer.Direction._` without namespace pollution
  object DirectionOp {

    def opp(dir: Direction) = dir match {
      case Center => Center
      case Top => Bottom
      case TopRight => BottomLeft
      case Right => Left
      case BottomRight => TopLeft
      case Bottom => Top
      case BottomLeft => TopRight
      case Left => Right
      case TopLeft => BottomRight
    }

    /** A function to determine the offset of a given neighbor direction in raster
     *  ordering.
     *
     *  Given a SpatialComponent, we generate an offset pair that can be added to
     *  the (row, column) of a SpatialKey to generate the SpatialKey of the
     *  desired neighbor.
     */
    def offsetOf(dir: Direction) = dir match {
      case Center      => ( 0, 0)
      case Top         => ( 0,-1)
      case TopRight    => ( 1,-1)
      case Right       => ( 1, 0)
      case BottomRight => ( 1, 1)
      case Bottom      => ( 0, 1)
      case BottomLeft  => (-1, 1)
      case Left        => (-1, 0)
      case TopLeft     => (-1,-1)
    }
  }
  /** Adapter method until Direction moves fully out of spark package */
  private[geotrellis] def convertDirection(input: Direction): geotrellis.util.Direction =
    input match {
      case TopLeft => geotrellis.util.Direction.TopLeft
      case Top => geotrellis.util.Direction.Top
      case TopRight => geotrellis.util.Direction.TopRight
      case Left => geotrellis.util.Direction.Left
      case Center => geotrellis.util.Direction.Center
      case Right => geotrellis.util.Direction.Right
      case BottomLeft => geotrellis.util.Direction.BottomLeft
      case Bottom => geotrellis.util.Direction.Bottom
      case BottomRight => geotrellis.util.Direction.BottomRight
    }
}
