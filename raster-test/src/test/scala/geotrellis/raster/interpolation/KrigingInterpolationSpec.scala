/*
 * Copyright (c) 2015 Azavea.
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

package geotrellis.raster.interpolation

import geotrellis.vector._
import geotrellis.vector.interpolation._
import geotrellis.vector.io.json._
import geotrellis.testkit._
import geotrellis.raster._

import spray.json.DefaultJsonProtocol._

import org.scalatest._

class KrigingInterpolationSpec extends FunSpec
                               with Matchers
                               with TestEngine
                               with TileBuilders {

  describe("Kriging Simple Interpolate (Raster) : Cobalt") {

    ignore("simple kriging prediction") {

      val extent = Extent(0,0,9,10)
      val re = RasterExtent(extent, 1, 1, 9, 10)
      val path = "raster-test/data/nickel.json"
      val f = scala.io.Source.fromFile(path)
      val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
      f.close()
      val points: Array[PointFeature[Double]] = collection.getAllPointFeatures[Double]().toArray

      //val radius = Some(6)
      val radius: Option[Double] = Some(6)
      val lag = 2
      val chunkSize = 100
      val semivariogram: Semivariogram = NonLinearSemivariogram(points, 0, 0, Spherical)
      val objectPredictor = new KrigingSimple(points, 0, semivariogram)
      val predictValue = objectPredictor.predict(Array(Point(1.0, 1.0)))
      val result = KrigingInterpolation(objectPredictor, points, re, chunkSize, Linear.apply(radius, lag))
      for(col <- 0 until re.cols) {
        for(row <- 0 until re.rows) {
          val actual = result.get(col,row)
          val expected = actual
          //The pointwise Kriging is tested, thus the raster is also by extension correct (Have to put in a suitable stub)
          assert(actual === expected)
        }
      }
    }
  }

  describe("Kriging Ordinary Interpolate (Raster) : Cobalt") {

    ignore("ordniary kriging prediction") {

      val extent = Extent(0,0,9,10)
      val re = RasterExtent(extent, 1, 1, 9, 10)
      val path = "raster-test/data/nickel.json"
      val f = scala.io.Source.fromFile(path)
      val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
      f.close()
      val points: Array[PointFeature[Double]] = collection.getAllPointFeatures[Double]().toArray

      val radius: Option[Double] = Some(6)
      val lag = 2
      val chunkSize = 100
      val semivariogram: Semivariogram = NonLinearSemivariogram(points, 0, 0, Spherical)
      val objectPredictor = new KrigingOrdinary(points, 0, semivariogram)
      val predictValue = objectPredictor.predict(Array(Point(1.0, 1.0)))
      val result = KrigingInterpolation(objectPredictor, points, re, chunkSize, Linear.apply(radius, lag))
      for(col <- 0 until re.cols) {
        for(row <- 0 until re.rows) {
          val actual = result.get(col,row)
          val expected = actual
          //The pointwise Kriging is tested, thus the raster is also by extension correct (Have to put in a suitable stub)
          assert(actual === expected)
        }
      }
    }
  }

  describe("Kriging Universal Interpolate (Raster) : Venice") {

    ignore("universal kriging prediction") {
      //The pointwise Kriging is tested, thus the raster is also by extension correct (Have to put in a suitable stub)
    }
  }

  describe("Kriging Geostatistical Interpolate (Raster) : Venice") {

    ignore("geostatistical kriging prediction") {
      //The pointwise Kriging is tested, thus the raster is also by extension correct (Have to put in a suitable stub)
    }
  }
}
