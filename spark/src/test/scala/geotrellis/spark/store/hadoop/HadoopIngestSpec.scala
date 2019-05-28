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

package geotrellis.spark.store.hadoop

import geotrellis.proj4.LatLng
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.spark.testkit._

import org.apache.hadoop.fs.Path
import org.scalatest._

class HadoopIngestSpec
  extends FunSpec
    with Matchers
    with TestEnvironment with TestFiles {

  val layoutScheme = ZoomedLayoutScheme(LatLng, 512)

  it("should allow filtering files in hadoopGeoTiffRDD") {
    val tilesDir = new Path(localFS.getWorkingDirectory,
      "raster/data/one-month-tiles/")
    val source = sc.hadoopGeoTiffRDD(tilesDir)

    // Raises exception if the bogus file isn't properly filtered out
    Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme){ (rdd, level) => {} }
  }

  it("should allow overriding tiff file extensions in hadoopGeoTiffRDD") {
    val tilesDir = new Path(localFS.getWorkingDirectory,
      "raster/data/one-month-tiles-tiff/")
    val source = sc.hadoopGeoTiffRDD(tilesDir, ".tiff")

    // Raises exception if the ".tiff" extension override isn't provided
    Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme){ (rdd, level) => {} }
  }
}

