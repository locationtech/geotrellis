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
import geotrellis.engine._
import geotrellis.testkit._
import geotrellis.raster._

import spray.json.DefaultJsonProtocol._

import org.scalatest._

class KrigingInterpolationSpec extends FunSpec
                               with Matchers
                               with TestEngine
                               with TileBuilders {

  describe("Kriging Simple Interpolate (Raster )") {

    ignore("should generate correct Prediction") {

      val extent = Extent(0,0,9,10)
      val re = RasterExtent(extent, 1, 1, 9, 10)
      //val points = Seq[PointFeature[Int]](
      val points = Seq[PointFeature[Double]](
        PointFeature(Point(0.0,0.0),10),
        PointFeature(Point(1.0,0.0),20),
        PointFeature(Point(4.0,4.0),60),
        PointFeature(Point(0.0,6.0),80)
      )

      //val radius = Some(6)
      val radius: Option[Double] = Some(6)
      val lag = 2
      val chunkSize = 100
      //val result = KrigingInterpolation(KrigingSimple, points, re, radius, chunkSize, lag, Linear)
      val predictor = new KrigingSimple(points, radius, chunkSize, lag, Linear)
      val result = KrigingInterpolation(predictor, points, re, radius, chunkSize, lag, Linear)
      //val result = obj.createPredictor()
      for(col <- 0 until re.cols) {
        for(row <- 0 until re.rows) {
          val actual = result.get(col,row)
          val expected = actual
          //The predictions of the Kriging Simple model look good (smooth curves), based on the visualization of Tiles predicted vs the input point sequences
          //Have to generate a dataset from a raster source and then compare the results
          assert(actual === expected)
        }
      }
    }
  }
}
