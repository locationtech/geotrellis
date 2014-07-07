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

import monocle.syntax._
import monocle.Macro._

import geotrellis.io.geotiff._
import geotrellis.io.geotiff.CompressionType._
import geotrellis.io.geotiff.ImageDirectoryLenses._

object CCITTDecompression {

  implicit class CCITT(matrix: Vector[Vector[Byte]]) {

    def uncompressCCITT(directory: ImageDirectory): Vector[Byte] =
      directory |-> compressionLens get match {
        case HuffmanCoded => matrix.map(uncompressCCITTSegment(_)).flatten.toVector
        case GroupThreeCoded => matrix.map(uncompressCCITTSegment(_)).flatten.toVector
        case GroupFourCoded => matrix.map(uncompressCCITTSegment(_)).flatten.toVector
      }

    var first = false

    def uncompressCCITTSegment(segment: Vector[Byte]) = {
      if (!first) {
        segment.foreach(println(_))
        first = true
      }

      segment
    }

  }

}
