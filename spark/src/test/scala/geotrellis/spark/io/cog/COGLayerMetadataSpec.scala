package geotrellis.spark.io.cog

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.tiling._
import geotrellis.vector._

import org.scalatest._
import spray.json._

class COGLayerMetadataSpec extends FunSpec with Matchers {
  def generateLCMetadata(maxZoom: Int = 13, minZoom: Int = 0): COGLayerMetadata[SpatialKey] = {
    val cellType = IntConstantNoDataCellType
    val extent = Extent(1.5454921707580032E7, 4146985.8273180854, 1.5762771754498206E7, 4454374.804079672)
    val crs = WebMercator
    val keyBounds = KeyBounds(SpatialKey(7255,3185), SpatialKey(7318,3248))
    val layoutScheme = ZoomedLayoutScheme(WebMercator)
    val maxTileSize = 4096

    COGLayerMetadata(cellType, extent, crs, keyBounds, layoutScheme, maxZoom, minZoom, maxTileSize)
  }

  describe("COGLayerMetadata.apply") {
    it("should produce correct metadata for landsat scene example, build all partial pyramids correct") {
      val expectedZoomRanges = Vector(ZoomRange(0,0), ZoomRange(1,1), ZoomRange(2,2), ZoomRange(3,3), ZoomRange(4,4), ZoomRange(5,5), ZoomRange(6,6), ZoomRange(7,8), ZoomRange(9,13))

      val md = generateLCMetadata()
      // println(md.toJson.prettyPrint)
      md.zoomRanges should contain theSameElementsAs (expectedZoomRanges)
    }

    it("should produce correct metadata for landsat scene example, build until zoom level that is not a lower zoom level of some partial pyramid") {
      val expectedZoomRanges = Vector(ZoomRange(7,8), ZoomRange(9,13))

      val md = generateLCMetadata(minZoom = 8)
      // println(md.toJson.prettyPrint)
      md.zoomRanges should contain theSameElementsAs (expectedZoomRanges)
    }

    it("should produce correct metadata for landsat scene example, build until zoom level where this level is both min and max level of this partial pyramid") {
      val expectedZoomRanges = Vector(ZoomRange(6,6), ZoomRange(7,8), ZoomRange(9,13))

      val md = generateLCMetadata(minZoom = 6)
      // println(md.toJson.prettyPrint)
      md.zoomRanges should contain theSameElementsAs (expectedZoomRanges)
    }
  }

  describe("COGLayerMetadata.zoomRangeInfoFor") {
    it("should return the requested zoomRangeInfo, regardless of whether zoomRangeInfos are sorted or not") {

      val md = generateLCMetadata()
      val mdUnsorted = md.copy(zoomRangeInfos = md.zoomRangeInfos.reverse)

      mdUnsorted.zoomRangeFor(5) should equal (ZoomRange(5,5))
    }
  }

  describe("COGLayerMetadata.combine") {
    it("should produce combined metadata with zoomRangeInfos sorted") {

      val md = generateLCMetadata()
      val mdOther = generateLCMetadata()
      val mdCombined = md.combine(mdOther)

      implicit val ordering: Ordering[(ZoomRange, KeyBounds[SpatialKey])] = Ordering.by(_._1)
      mdCombined.zoomRangeInfos should be (sorted)
    }
  }
}
