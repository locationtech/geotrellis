package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.reproject._
import geotrellis.vector.json._
import geotrellis.testkit._
import geotrellis.proj4._
import geotrellis.testkit.vector._
import geotrellis.raster.io.geotiff.reader._

import org.scalatest._

import spire.syntax.cfor._

class RowTransformSpec extends FunSpec
                          with TileBuilders
                          with TestEngine {
  val path = "raster-test/data/schoolgeo.json"

  val LLtoWM = Transform(LatLng, WebMercator)
  val WMtoLL = Transform(WebMercator, LatLng)
  val pointsWM = GeoJson.fromFile[JsonFeatureCollection](path).getAllPoints
  val pointsLL = pointsWM.map(_.reproject(WMtoLL))

  val srcX = pointsLL.map(_.x).toArray
  val srcY = pointsLL.map(_.y).toArray

  def getDestX = Array.ofDim[Double](srcX.size)
  def getDestY = Array.ofDim[Double](srcX.size)

  describe("RowTransform.approximate") {
    it("should remain within 1.0 threshold for points in an WSG84 -> WebMercator") {
      val (destX, destY) = (getDestX, getDestY)
      val rowTransform = RowTransform.approximate(LLtoWM, 1.0)
      rowTransform(srcX, srcY, destX, destY)
      val expectedPoints = pointsLL.map(_.reproject(LLtoWM))
      val actualPoints = destX.zip(destY).map { case (x,y) => Point(x, y) }
      for((expected, actual) <- expectedPoints.zip(actualPoints)) {
        actual should matchGeom(expected, 1.0)
      }
    }

    it("should remain within .0125 threshold for points in an WSG84 -> WebMercator") {
      val (destX, destY) = (getDestX, getDestY)
      val rowTransform = RowTransform.approximate(LLtoWM, 0.0125)
      rowTransform(srcX, srcY, destX, destY)
      val actualPoints = destX.zip(destY).map { case(x,y) => Point(x, y) }
      for((expected, actual) <- pointsLL.map(_.reproject(LLtoWM)).zip(actualPoints)) {
        actual should matchGeom(expected, 0.0125)
      }
    }

    it("should remain within 10.0 threshold for points in an WSG84 -> WebMercator") {
      val (destX, destY) = (getDestX, getDestY)
      val rowTransform = RowTransform.approximate(LLtoWM, 10.0)
      rowTransform(srcX, srcY, destX, destY)
      val actualPoints = destX.zip(destY).map { case(x,y) => Point(x, y) }
      for((expected, actual) <- pointsLL.map(_.reproject(LLtoWM)).zip(actualPoints)) {
        actual should matchGeom(expected, 10.0)
      }
    }

    it("should approximate WebMercator -> LatLng for raster extent") {
      val (expected, expectedExtent, _) =
        GeoTiffReader("raster-test/data/reproject/slope_wsg84-nearestneighbor.tif").read.imageDirectories.head.toRaster
      val re = RasterExtent(expected, expectedExtent)

      val threshold = ReprojectOptions.DEFAULT.errorThreshold

      val rowTransform = RowTransform.approximate(WMtoLL, threshold)

      for(row <- 0 until re.rows) {
        val srcX = (0 until re.cols).map { i => re.gridColToMap(i) }.toArray
        val srcY = (0 until re.cols).map { i => re.gridRowToMap(row) }.toArray
        val destX = Array.ofDim[Double](srcX.size)
        val destY = Array.ofDim[Double](srcX.size)

        rowTransform(srcX, srcY, destX, destY)

        val actualPoints = srcX.zip(srcY).map { case (x, y) => Point(x, y) }
        val actualProjected = actualPoints.map(_.reproject(WMtoLL))
        val actualDestX = actualProjected.map(_.x).toArray
        val actualDestY = actualProjected.map(_.y).toArray

        for(col <- 0 until re.cols) {
          destX(col) should be (actualDestX(col) +- threshold)
          destY(col) should be (actualDestY(col) +- threshold)
        }
      }
    }

    it("should approximate EPSG:32618 -> WebMercator for raster extent") {
      val (expected, expectedExtent, _) =
        GeoTiffReader("raster-test/data/reproject/slope_epsg32618.tif").read.imageDirectories.head.toRaster
      val re = RasterExtent(expected, expectedExtent)

      val threshold = ReprojectOptions.DEFAULT.errorThreshold

      val transform = Transform(CRS.fromName("EPSG:32618"), WebMercator)
      val rowTransform = RowTransform.approximate(transform, threshold)

      for(row <- 0 until re.rows) {
        val srcX = (0 until re.cols).map { i => re.gridColToMap(i) }.toArray
        val srcY = (0 until re.cols).map { i => re.gridRowToMap(row) }.toArray
        val destX = Array.ofDim[Double](srcX.size)
        val destY = Array.ofDim[Double](srcX.size)

        rowTransform(srcX, srcY, destX, destY)

        val actualPoints = srcX.zip(srcY).map { case (x, y) => Point(x, y) }
        val actualProjected = actualPoints.map(_.reproject(transform))
        val actualDestX = actualProjected.map(_.x).toArray
        val actualDestY = actualProjected.map(_.y).toArray

        for(col <- 0 until re.cols) {
          destX(col) should be (actualDestX(col) +- threshold)
          destY(col) should be (actualDestY(col) +- threshold)
        }
      }
    }
  }
}
