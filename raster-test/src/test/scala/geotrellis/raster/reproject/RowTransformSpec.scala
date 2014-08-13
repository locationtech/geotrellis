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
  val f = scala.io.Source.fromFile(path)
  val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
  f.close

  val points = collection.getAllPoints

  val srcX = points.map(_.x).toArray
  val srcY = points.map(_.y).toArray

  def getDestX = Array.ofDim[Double](srcX.size)
  def getDestY = Array.ofDim[Double](srcX.size)

  describe("RowTransform.approximate") {
    it("should remain within 1.0 threshold for points in an WSG84 -> WebMercator") {
      val (destX, destY) = (getDestX, getDestY)
      val transform = Transform(LatLng, WebMercator)
      val rowTransform = RowTransform.approximate(transform, 1.0)
      rowTransform(srcX, srcY, destX, destY)
      val actualPoints = destX.zip(destY).map { case(x,y) => Point(x, y) }
      for((expected, actual) <- points.map(_.reproject(transform)).zip(actualPoints)) {
        actual should matchGeom(expected, 1.0)
      }
    }

    it("should remain within .0125 threshold for points in an WSG84 -> WebMercator") {
      val (destX, destY) = (getDestX, getDestY)
      val transform = Transform(LatLng, WebMercator)
      val rowTransform = RowTransform.approximate(transform, 0.0125)
      rowTransform(srcX, srcY, destX, destY)
      val actualPoints = destX.zip(destY).map { case(x,y) => Point(x, y) }
      for((expected, actual) <- points.map(_.reproject(transform)).zip(actualPoints)) {
        actual should matchGeom(expected, 0.0125)
      }
    }

    it("should remain within 10.0 threshold for points in an WSG84 -> WebMercator") {
      val (destX, destY) = (getDestX, getDestY)
      val transform = Transform(LatLng, WebMercator)
      val rowTransform = RowTransform.approximate(transform, 10.0)
      rowTransform(srcX, srcY, destX, destY)
      val actualPoints = destX.zip(destY).map { case(x,y) => Point(x, y) }
      for((expected, actual) <- points.map(_.reproject(transform)).zip(actualPoints)) {
        actual should matchGeom(expected, 10.0)
      }
    }

    it("should approximate WebMercator -> LatLng for raster extent") {
      val (expected, expectedExtent) = 
        GeoTiffReader("raster-test/data/reproject/slope_wsg84-nearestneighbor.tif").read.imageDirectories.head.toRaster
      val re = RasterExtent(expected, expectedExtent)

      val threshold = ReprojectOptions.DEFAULT.errorThreshold

      val transform = Transform(LatLng, WebMercator)
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
