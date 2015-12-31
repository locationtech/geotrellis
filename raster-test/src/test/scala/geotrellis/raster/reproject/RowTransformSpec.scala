package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.reproject._
import geotrellis.vector.io.json._
import geotrellis.testkit._
import geotrellis.proj4._
import geotrellis.testkit.vector._
import geotrellis.raster.io.geotiff._

import org.scalatest._

import spire.syntax.cfor._

class RowTransformSpec extends FunSpec
    with TileBuilders
    with RasterMatchers {
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
      val Raster(expected, expectedExtent) = 
        SingleBandGeoTiff("raster-test/data/reproject/slope_wsg84-nearestneighbor.tif").raster

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
      val Raster(expected, expectedExtent) = 
        SingleBandGeoTiff("raster-test/data/reproject/slope_epsg32618.tif").raster

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

    it("should compute each transformed point within 25% of the error threshold in a case where it has points with greater then the error threshold errors in transformed values") {
      val utm = CRS.fromName("EPSG:32618")
      val llToUtm = Transform(LatLng, utm)
      val utmToWebMercator = Transform(utm, WebMercator)

      val extent = Extent(-76.98126799543014, 2.59016990126755, -72.4959201344731, 30.405093700132642)
      val srcX = Array(285746.38182013505, 295285.044609424, 304823.2526249018, 314361.02708198724, 323898.38919137814, 333435.36015926604, 342971.96118758223, 352508.21347421256, 362044.1382132471, 371579.7565952033, 381115.0898072487, 390650.15903345094, 400184.9854549917, 409719.5902504105, 419253.99459582614, 428788.2196651719, 438322.28663043195, 447856.21666185686, 457390.03092821274, 466923.75059700233, 476457.3968346942, 485990.9908069579, 495524.5536788916, 505058.1066152605, 514591.6707807111, 524125.26734002115, 533658.91745832, 543192.6423013181, 552726.4630355417, 562260.4008285613, 571794.4768492277, 581328.7122678943, 590863.1282566526, 600397.7459895669, 609932.5866428972, 619467.671395334, 629003.0214282337, 638538.657925842, 648074.6020755295, 657610.8750680212, 667147.4980976345, 676684.4923624952, 686221.8790647872, 695759.6794109747, 705297.9146120325, 714836.6058836813, 724375.7744466185, 733915.4415267558, 743455.6283554365, 752996.3561696869, 762537.6462124374, 772079.5197327558)
      val srcY = Array(738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361, 738398.5226308361)

      val threshold = 1.7853699014796325

      val destX = Array.ofDim[Double](srcX.size)
      val destY = destX.clone

      val rowTransform = RowTransform.approximate(utmToWebMercator, threshold)
      rowTransform(srcX, srcY, destX, destY)

      val src =  srcX.zip(srcY).map { case (x, y) => Point(x, y) }
      val srcProjected = src.map(_.reproject(utmToWebMercator))
      val dest = destX.zip(destY).map { case (x, y) => Point(x, y) }

      src.zip(srcProjected).zip(dest)
        .foreach { case ((sp1, p1), p2) =>
          val dx = math.abs(p1.x - p2.x)
          val dy = math.abs(p1.y - p2.y)
          val d  = dx + dy
          withClue(s"$p1 $p2 $dx $dy $d ${math.abs(d - threshold)}") { d should be <= (threshold * 1.25) }
        }
    }
  }
}
