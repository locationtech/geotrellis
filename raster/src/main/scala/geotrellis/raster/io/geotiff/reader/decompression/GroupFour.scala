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

package geotrellis.raster.io.geotiff.reader.decompression

import monocle.syntax._
import monocle.Macro._

import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.reader.ImageDirectoryLenses._

case class T6Options(options: Int = 0, fillOrder: Int)

object GroupFourDecompression {

  implicit class GroupFour(matrix: Vector[Vector[Byte]]) {

    def uncompressGroupFour(implicit directory: ImageDirectory): Vector[Vector[Byte]] = {
      implicit val t6Options = directory |-> t6OptionsLens get match {
        case Some(t6OptionsInt) => T6Options(t6OptionsInt,
          directory |-> fillOrderLens get)
        case None => throw new MalformedGeoTiffException("no T6Options")
      }

      matrix.zipWithIndex.par.map{ case(segment, i) =>
        uncompressGroupFourSegment(segment, i) }.toVector
    }

    //Always 2d coding, each segment encoded seperately, all white line first
    private def uncompressGroupFourSegment(segment: Vector[Byte], index: Int)
      (implicit t6Options: T6Options, directory: ImageDirectory) = {

      val length = directory.rowsInSegment(index)
      val width = directory.rowSize

      val decompressor = new TIFFFaxDecoder(t6Options.fillOrder, width, length)

      val inputArray = segment.toArray
      val outputArray = Array.ofDim[Byte]((length * width + 7) / 8)

      decompressor.decodeT6(outputArray, inputArray, 0, length, t6Options.options)

      outputArray.toVector
    }

  }

}
