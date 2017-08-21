/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.rasterize

import org.scalatest._
import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.wkt._
import geotrellis.vector.io.json._

import java.nio.file.Files;
import java.nio.file.Paths;

class RasterizeFeatureRDDSpec extends FunSpec with Matchers
    with TestEnvironment {

  val polygon0 = Polygon(
    Point(4,4),
    Point(5,4),
    Point(5,5),
    Point(4,5),
    Point(4,4)
  )
  val polygon1 = Polygon(
    Point(0,0),
    Point(7,0),
    Point(7,10),
    Point(0,10),
    Point(0,0)
  )
  val polygon2 = Polygon(
    Point(3,0),
    Point(10,0),
    Point(10,10),
    Point(3,10),
    Point(3,0)
  )

  val e = Extent(0, 0, 10, 10)
  val tl = TileLayout(1, 1, 16, 16)
  val ld = LayoutDefinition(e, tl)
  val ct = DoubleConstantNoDataCellType

  it("z-buffer 1"){
    val features = sc.parallelize(List(
      Feature(polygon0, RasterizeFeaturesRDD.FeatureInfo(value = 1000, priority = 0.5)),
      Feature(polygon1, RasterizeFeaturesRDD.FeatureInfo(value = 1, priority = 1)),
      Feature(polygon2, RasterizeFeaturesRDD.FeatureInfo(value = 2, priority = 2)),
      Feature(polygon0, RasterizeFeaturesRDD.FeatureInfo(value = 2000, priority = 0.5)),
      Feature(polygon0, RasterizeFeaturesRDD.FeatureInfo(value = 3000, priority = 0.5))
    ))
    val tile = RasterizeFeaturesRDD
      .fromFeaturePriority(features, ct, ld, usePriority = true)
      .collect().head._2

    tile.toArray.sum should be (432)
  }

  it("z-buffer 2"){
    val features = sc.parallelize(List(
      Feature(polygon0, RasterizeFeaturesRDD.FeatureInfo(value = 1000, priority = 0.5)),
      Feature(polygon1, RasterizeFeaturesRDD.FeatureInfo(value = 1, priority = 3)),
      Feature(polygon2, RasterizeFeaturesRDD.FeatureInfo(value = 2, priority = 2)),
      Feature(polygon0, RasterizeFeaturesRDD.FeatureInfo(value = 2000, priority = 0.5)),
      Feature(polygon0, RasterizeFeaturesRDD.FeatureInfo(value = 3000, priority = 0.5))
    ))
    val tile = RasterizeFeaturesRDD
      .fromFeaturePriority(features, ct, ld, usePriority = true)
      .collect().head._2

    tile.toArray.sum should be (336)
  }

}
