/*
 * Copyright 2019 Azavea
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

package geotrellis.spark

import geotrellis.raster._
import geotrellis.raster.geotiff._
import geotrellis.raster.io.geotiff._
import geotrellis.proj4._
import geotrellis.layer._

import geotrellis.spark.testkit._
import geotrellis.raster.testkit.{RasterMatchers, Resource}

import org.apache.spark.rdd._
import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.Inspectors._

class RasterRegionSpec extends AnyFunSpec with TestEnvironment with RasterMatchers with LayoutRasterMatchers with GivenWhenThen {
  it("reads RDD of raster refs") {
    // we're going to read these and re-build gradient.tif

    Given("some files and a LayoutDefinition")
    val paths = List(
      Resource.path("vlm/left-to-right.tif"),
      Resource.path("vlm/top-to-bottom.tif"),
      Resource.path("vlm/diagonal.tif"))

    // I might need to discover the layout at a later stage, for now we'll know

    val layout: LayoutDefinition = {
      val scheme = FloatingLayoutScheme(32, 32)
      val tiff = GeoTiff.readSingleband(paths.head)
      scheme.levelFor(tiff.extent, tiff.cellSize).layout
    }
    val crs = LatLng

    When("Generating RDD of RasterRegions")
    val rdd: RDD[(SpatialKey, RasterRegion)] with Metadata[TileLayerMetadata[SpatialKey]] = {
      val srcRdd = sc.parallelize(paths, paths.size).map { uri => GeoTiffRasterSource(uri) }
      srcRdd.cache()

      val (combinedExtent, commonCellType) =
        srcRdd.map { src => (src.extent, src.cellType) }
          .reduce { case ((e1, ct1), (e2, ct2)) => (e1.combine(e2), ct1.union(ct2)) }

      // we know the metadata from the layout, we just need the raster extent
      val tlm = TileLayerMetadata[SpatialKey](
        cellType = commonCellType,
        layout = layout,
        extent = combinedExtent,
        crs = crs,
        bounds = KeyBounds(layout.mapTransform(combinedExtent)))

      val refRdd = srcRdd.flatMap { src =>
        // too easy? whats missing
        val tileSource = LayoutTileSource.spatial(src, layout)
        tileSource.keys.toIterator.map { key => (key, tileSource.rasterRegionForKey(key).get) }
      }

      // TADA! Jobs done.
      ContextRDD(refRdd, tlm)
      // NEXT:
      // - combine the bands to complete the test
      // - make an app for this and run it againt WSEL rasters
      // - run aup on EMR, figure out how to set AWS secret keys?
    }

    Then("get a RasterRegion for each region of each file")
    rdd.count shouldBe (8*8*3) // three 256x256 files split into 32x32 windows

    Then("convert each RasterRegion to a tile")
    val realRdd: MultibandTileLayerRDD[SpatialKey] =
      rdd.withContext(_.flatMap{ case (key, ref) =>
        for {
          raster <- ref.raster
        } yield (key, raster.tile)
      })

    realRdd.count shouldBe (8*8*3) // we shouldn't have lost anything

    Then("Each row matches the layout")
    val rows = realRdd.collect
    forAll(rows) { case (key, tile) =>
      realRdd.metadata.bounds should containKey(key)
      tile should have (
        // dimensions (layout.tileCols, layout.tileRows),
        cellType (realRdd.metadata.cellType)
      )
    }
  }
}
