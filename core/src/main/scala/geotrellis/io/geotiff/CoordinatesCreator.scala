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

package geotrellis.io.geotiff

import monocle.syntax._
import monocle.Macro._

import geotrellis.io.geotiff.utils.MatrixUtils._
import geotrellis.io.geotiff.CommonPublicValues._
import geotrellis.io.geotiff.GeoKeys._

object CoordinatesCreator {

  implicit class CoordinatesCreatorImplicit(directory: ImageDirectory) {

    import GeoKeyDirectoryLenses._
    import ImageDirectoryLenses._

    lazy val geoKeyDirectory = directory.geoTiffTags.geoKeyDirectory.getOrElse {
      throw new IllegalAccessException("no geo key directory present")
    }

    def setCoordinates: ImageDirectory = {
      val coordinates = (directory |-> modelTransformationLens get) match {
        case Some(trans) if (trans.validateAsMatrix && trans.size == 4
            && trans(0).size == 4) => transformationModelSpace(trans)
        case None => (directory |-> modelTiePointsLens get) match {
          case Some(tiePoints) if (tiePoints.size % 6 == 0 && !tiePoints.isEmpty)  =>
            tiePointsModelSpace(tiePoints, directory |-> modelPixelScaleLens get)
          case _ => throw new MalformedGeoTiffException(
            "neither model transformation nor tiepoints, or malformed ones."
          )
        }
      }

      directory |-> coordinatesLens set(coordinates)
    }

    private def transformationModelSpace(modelTransformation: Vector[Vector[Double]]) = {
      def matrixMult(pixel: Pixel3D) = Pixel3D.fromVector((modelTransformation *
        Vector(Vector(pixel.x, pixel.y, pixel.z, 1))).flatten.take(3))

      coordinatesFromModelFunction(matrixMult)
    }

    private def tiePointsModelSpace(tiePoints: Vector[(Pixel3D, Pixel3D)],
      pixelScaleOption: Option[(Double, Double, Double)]) =
      pixelScaleOption match {
        case Some(pixelScales) => {
          def modelFunc(pixel: Pixel3D) = {
            val (first, second) = tiePoints.head

            val scaleX = (pixel.x - first.x) * pixelScales._1
            val scaleY = (pixel.y - first.y) * pixelScales._2
            val scaleZ = (pixel.z - first.z) * pixelScales._3

            Pixel3D(scaleX * second.x, scaleY * second.y, scaleZ * second.z)
          }

          coordinatesFromModelFunction(modelFunc)
        }
        case None => {
          val imageWidth = (directory |-> imageWidthLens get).toInt
          val imageLength = (directory |-> imageLengthLens get).toInt

          var minX = 0.0
          var minY = 0.0
          var maxX = 0.0
          var maxY = 0.0

          for (i <- 0 until 4) {
            val xt = if (i % 2 == 1) imageWidth - 1 else 0
            val yt = if (i >= 2) imageLength - 1 else 0

            val optPixel = tiePoints.filter(pixel => pixel._1.x == xt &&
              pixel._1.y == yt).map(_._2).headOption

            if (!optPixel.isEmpty) {
              val pixel = optPixel.get
              if (i == 0 || i == 1) maxY = pixel.y
              if (i == 0 || i == 2) minX = pixel.x
              if (i == 1 || i == 3) maxX = pixel.x
              if (i == 2 || i == 3) minY = pixel.y
            }
          }

          ???
        }
      }

    // if pixel is point then problemo
    private def coordinatesFromModelFunction(func: Pixel3D => Pixel3D) = {
      val modelCoordinates = directory.getRasterBoundaries.map(func)

      ???
    }

    private def hasPixelArea(): Boolean =
      (geoKeyDirectory |-> gtRasterTypeLens get) match {
        case Some(UndefinedCPV) => throw new MalformedGeoTiffException(
          "the raster type must be present."
        )
        case Some(UserDefinedCPV) => throw new GeoTiffReaderLimitationException(
          "this reader doesn't support user defined raster types."
        )
        case Some(v) => v == 1
        case None => false
      }

  }

}
