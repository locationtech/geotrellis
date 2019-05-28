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

package geotrellis.spark.store

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.wkt.WKT
import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.layers._
import geotrellis.spark._
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.testkit._

import org.scalatest._

class LayerQuerySpec extends FunSpec
  with TestEnvironment with TestFiles with Matchers {

  def spatialKeyBoundsKeys(kb: KeyBounds[SpatialKey]) = {
    for {
      row <- kb.minKey.row to kb.maxKey.row
      col <- kb.minKey.col to kb.maxKey.col
    } yield SpatialKey(col, row)
  }

  describe("RasterQuerySpec") {
    val keyBounds = KeyBounds(SpatialKey(1, 1), SpatialKey(6, 7))

    val md = TileLayerMetadata(
      FloatConstantNoDataCellType,
      LayoutDefinition(LatLng.worldExtent, TileLayout(8, 8, 3, 4)),
      Extent(-135.00000125, -89.99999, 134.99999125, 67.49999249999999),
      LatLng,
      keyBounds
    )



    it("should be better then Java serialization") {
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(GridBounds(2, 2, 2, 2)))
      val outKeyBounds = query(md)
      info(outKeyBounds.toString)
    }

  }

  describe("LayerFilter Polygon Intersection") {
    import geotrellis.vector.{Point, Polygon, MultiPolygon}

    implicit def polygonToMultiPolygon(polygon: Polygon): MultiPolygon = MultiPolygon(polygon)

    val md = AllOnesTestFile.metadata
    val mt = md.mapTransform
    val kb = KeyBounds[SpatialKey](SpatialKey(0, 0), SpatialKey(6, 7))
    val bounds = GridBounds(1, 1, 3, 2)
    val horizontal = Polygon(List(
      Point(-130.0, 60.0),
      Point(-130.0, 30.0),
      Point(-100.0, 30.0),
      Point(-100.0, 60.0),
      Point(-130.0, 60.0)))
    val vertical = Polygon(List(
      Point(-130.0, 40.0),
      Point(-130.0, 30.0),
      Point(-10.0, 30.0),
      Point(-10.0, 40.0),
      Point(-130.0, 40.0)))
    val diagonal = Polygon(List(
      Point(-125.0, 60.0),
      Point(-130.0, 55.0),
      Point(-15.0, 30.0),
      Point(-10.0, 35.0),
      Point(-125.0, 60.0)))

    def naiveKeys(polygon: MultiPolygon): List[SpatialKey] = {
      (for ((x, y) <- bounds.coordsIter.toSeq
        if polygon.intersects(md.mapTransform(SpatialKey(x, y)))) yield SpatialKey(x, y))
        .toList
    }

    it("should find all keys that intersect appreciably with a horizontal rectangle") {
      val polygon = horizontal
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(polygon))
      val actual = query(md).flatMap(spatialKeyBoundsKeys)
      val expected = naiveKeys(polygon)
      (expected diff actual) should be ('empty)
    }

    it("should find all keys that intersect appreciably with a horizontal rectangle that is in the same projection") {
      val polygon = horizontal
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(polygon -> md.crs))
      val actual = query(md).flatMap(spatialKeyBoundsKeys)
      val expected = naiveKeys(polygon)
      (expected diff actual) should be ('empty)
    }

    it("should find all keys that intersect appreciably with a vertical rectangle") {
      val polygon = vertical
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(polygon))
      val actual = query(md).flatMap(spatialKeyBoundsKeys)
      val expected = naiveKeys(polygon)
      (expected diff actual) should be ('empty)
    }

    it("should find all keys that intersect appreciably with a vertical rectangle that is in a different projection") {
      val polyCRS = CRS.fromEpsgCode(2033)
      val polygon = vertical.reproject(md.crs, polyCRS)
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(polygon -> polyCRS))
      val actual = query(md).flatMap(spatialKeyBoundsKeys)
      val expected = naiveKeys(polygon)
      (expected diff actual) should be ('empty)
    }

    it("should find all keys that intersect appreciably with an L-shaped polygon") {
      val polygon = MultiPolygon(List(horizontal, vertical))
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(polygon))
      val actual = query(md).flatMap(spatialKeyBoundsKeys)
      val expected = naiveKeys(polygon)
      (expected diff actual) should be ('empty)
    }

    it("should find all keys that intersect appreciably with an L-shaped polygon that is in a differet projection") {
      val polyCRS = CRS.fromEpsgCode(2023)
      val polygon = MultiPolygon(List(horizontal, vertical)).reproject(md.crs, polyCRS)
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(polygon -> polyCRS))
      val actual = query(md).flatMap(spatialKeyBoundsKeys)
      val expected = naiveKeys(polygon)
      (expected diff actual) should be ('empty)
    }

    it("should find all keys that intersect appreciably with a diagonal rectangle") {
      val polygon = diagonal
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(polygon))
      val actual = query(md).flatMap(spatialKeyBoundsKeys)
      val expected = naiveKeys(polygon)
      (expected diff actual) should be ('empty)
    }

    it("should cover huc10 polygon fully") {
      val wkt = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/wkt/huc10-conestoga.wkt")).getLines.mkString
      val huc10 = WKT.read(wkt).asInstanceOf[MultiPolygon]
      val huc10LayerMetadata = TileLayerMetadata(
        crs = ConusAlbers,
        cellType = FloatCellType,
        layout = LayoutDefinition(Extent(-2493045.0, 177285.0, 2345355.0, 3310725.0),TileLayout(315,204,512,512)),
        extent = Extent(-2493045.0, 177285.0, 2345355.0, 3310725.0),
        bounds = KeyBounds(SpatialKey(0,0),SpatialKey(314,204))
      )
      val mapTransform = huc10LayerMetadata.mapTransform
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]]
        .where(Intersects(huc10))
      val actual: Seq[SpatialKey] = query(huc10LayerMetadata).flatMap(spatialKeyBoundsKeys)
      val expected =  {
        val bounds = huc10LayerMetadata.bounds.get.toGridBounds
        for {
          (x, y) <- bounds.coordsIter.toSeq
          if huc10.intersects(mapTransform(SpatialKey(x, y)))
        } yield SpatialKey(x, y)
      }
      (expected.toList diff actual) should be ('empty)

      // test specifically for previously missing key
      actual should contain (SpatialKey(272, 79))
    }

    it("should query perimeter of huc10 polygon") {
      val wkt = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/wkt/huc10-conestoga.wkt")).getLines.mkString
      val huc10 = WKT.read(wkt).asInstanceOf[MultiPolygon]
      val ml = MultiLine(huc10.polygons.flatMap { p =>
        p.exterior +: p.holes.toList
      })
      val huc10LayerMetadata = TileLayerMetadata(
        crs = ConusAlbers,
        cellType = FloatCellType,
        layout = LayoutDefinition(Extent(-2493045.0, 177285.0, 2345355.0, 3310725.0),TileLayout(1315,1204,512,512)),
        extent = Extent(-2493045.0, 177285.0, 2345355.0, 3310725.0),
        bounds = KeyBounds(SpatialKey(0,0),SpatialKey(1314,1204))
      )
      val mapTransform = huc10LayerMetadata.mapTransform
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]]
        .where(Intersects(ml))
      val actual: Seq[SpatialKey] = query(huc10LayerMetadata).flatMap(spatialKeyBoundsKeys)
      val expected =  {
        val bounds = huc10LayerMetadata.bounds.get.toGridBounds
        for {
          (x, y) <- bounds.coordsIter.toSeq
          //
          if ml.intersects(mapTransform(SpatialKey(x, y)))
        } yield SpatialKey(x, y)
      }

      (expected.toList diff actual) should be ('empty)
    }
  }

  describe("LayerQuery KeyBounds generation") {
    val md = AllOnesTestFile.metadata
    val kb = KeyBounds[SpatialKey](SpatialKey(0, 0), SpatialKey(6, 7))

    it("should generate KeyBounds for single region") {
      val bounds1 = GridBounds(1, 1, 3, 2)
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(bounds1))
      val expected = for ((x, y) <- bounds1.coordsIter.toSeq) yield SpatialKey(x, y)

      val found = query(md).flatMap(spatialKeyBoundsKeys)
      info(s"missing: ${(expected diff found).toList}")
      info(s"unwanted: ${(found diff expected).toList}")

      found should contain theSameElementsAs expected
    }

    it("should generate KeyBounds for two regions") {
      val bounds1 = GridBounds(1, 1, 3, 3)
      val bounds2 = GridBounds(4, 5, 6, 6)
      val query = new LayerQuery[SpatialKey, TileLayerMetadata[SpatialKey]].where(Intersects(bounds1) or Intersects(bounds2))
      val expected = for ((x, y) <- bounds1.coordsIter.toSeq ++ bounds2.coordsIter.toSeq) yield SpatialKey(x, y)

      val found = query(md).flatMap(spatialKeyBoundsKeys)
      info(s"missing: ${(expected diff found).toList}")
      info(s"unwanted: ${(found diff expected).toList}")

      found should contain theSameElementsAs expected
    }
    // TODO: it would be nice to test SpaceTime too, but since time doesn't have a resolution we can not iterate
  }
}
