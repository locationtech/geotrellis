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

//     it("should whatever") {
//       val extent = Extent(-81.07552633321905,28.354440523676043,-33.95621560847776,63.205709860609815)

//       // val srcX = Array(253789.68630146325, 287823.9397727931, 313747.16529812233, 382439.1686816265, 391914.3332301793, 425074.4818594002, 448794.9885647554, 456211.43139436987, 459207.4074817363, 459453.6676957176, 478347.3311571979, 484025.9764840079, 509304.43173211475, 583504.6307024988, 694914.1556441694, 706518.4764781077, 751353.1912030455, 775106.8293291826, 783358.3878931825, 802784.4768799776, 842848.2912062858, 850987.8967064042, 889251.4589685055, 986996.637964196, 1011526.5104295649, 1041657.349772111, 1071294.841801154, 1133955.8001823684, 1168159.963153659, 1213761.5805790462, 1301086.6886624682, 1325363.0357374374, 1422592.221967804, 1434642.381093814, 1459453.1436424302, 1473105.586430269, 1505407.7969641313, 1524448.6121122104, 1536827.5591484855, 1558843.0906098061, 1559290.6306442101, 1609535.8952319103, 1626259.2899044112, 1639598.8300947617, 1661499.6282190692, 1677699.4128414132, 1681104.4534844432, 1683375.9483315565, 1724093.226595671, 1735178.5089986718, 1736916.8900898586, 1757936.8843681598, 1759631.3619245526, 1762572.8281185457, 1772547.515445175, 1828026.8474904802, 1828882.776604421, 1849418.3371032258, 1852579.4457255418, 1855231.3875195659, 1856499.396291033, 1881196.815672054, 1881307.464157677, 1943088.4268005409, 1976466.6104487, 2035991.1522115036, 2052336.1081589614, 2059781.7435954562, 2066638.593658232, 2108771.8210938144, 2158066.9967097617, 2236548.022242422, 2254252.719998205, 2291174.117094814, 2368424.4718858832, 2385869.998948486, 2435664.0609155083, 2439906.791312957, 2531921.6739708846, 2584201.569504936, 2605459.3579213414, 2617315.263365134, 2655213.4052540893, 2699787.2029086617, 2714423.1690831496, 2720721.41333343, 2740901.9347949643, 2748834.25951623, 2840847.1936243367, 2872546.1264581056, 2927918.4182011182, 2929804.849426171, 2930738.6442946694)

//       val srcY = //(0 until srcX.size).map(x => 5067665.703093847).toArray
// Array(6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825, 6169478.911444825)
//       val re = RasterExtent(extent, srcY.size, srcY.size)
//       val srcX = (0 until srcY.size).map { col => re.gridColToMap(col) }.toArray

//       val threshold = 20.0//1.6262390872049841// 5.0//0.9604552329838351

//       val utm = CRS.fromName("EPSG:32618")
//       val utmToWebMercator = Transform(utm, WebMercator)
//       val destX = Array.ofDim[Double](srcX.size)
//       val destY = destX.clone

//       import geotrellis.vector.io.wkt._
//       val l = Line(srcX.zip(srcY).map { case (x, y) => Point(x, y) })
//       println(l.toWKT)
//       println(l.reproject(utmToWebMercator).toWKT)

//       val rowTransform = RowTransform.approximate(utmToWebMercator, threshold)
//       rowTransform(srcX, srcY, destX, destY)

//       val src =  srcX.zip(srcY).map { case (x, y) => Point(x, y) }
//       val srcProjected = src.map(_.reproject(utmToWebMercator))
//       val dest = destX.zip(destY).map { case (x, y) => Point(x, y) }

//       val s = destX.map(x => if(x == 0.0) { 0 } else if(x == -1.0) { 1 } else { 2 }).mkString("")
//       println(s"${s}")


//       val l2 = Line(dest)
//       println(l2.toWKT)
// //      println(l.reproject(utmToWebMercator).toWKT)


//       srcProjected.zip(dest).zipWithIndex
//         .foreach { case((p1, p2), i) =>
//           println(s"$i of ${dest.size}: $p1 vs $p2")
//           val dx = math.abs(p1.x - p2.x)
//           val dy = math.abs(p1.y - p2.y)
//           if(dx + dy > threshold) {
//             println(s"  $dx $dy")
//           }
//           (dx + dy) should be <= (threshold)
//         }

//     }
  }
}
