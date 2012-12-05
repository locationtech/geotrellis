package geotrellis.raster.op

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.feature._

import geotrellis.raster.op.zonal._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class TiledPolygonalZonalSummarySpec extends FunSpec with ShouldMatchers {
  val server = TestServer()

  sealed trait TestResultTile

  case class FullTile(re: RasterExtent) extends TestResultTile
  case class PartialTile(re: RasterExtent, poly: Geometry[_]) extends TestResultTile
  case object NoDataTile extends TestResultTile

  object PartialTile {
    def norm(re: RasterExtent, poly: Geometry[_]) = new PartialTile(re, poly.mapGeom(z => {z.normalize; z}))
  }
 
  case class MockTiledPolygonalZonalSummary[DD](
    r:Op[Raster], zonePolygon:Op[Polygon[DD]])(
    implicit val mB: Manifest[TestResultTile], val mD: Manifest[DD])
       extends TiledPolygonalZonalSummary[Seq[TestResultTile]] {

    type B = TestResultTile
    type D = DD

    def handlePartialTileIntersection(r: Op[Raster], p: Op[Geometry[D]]) = {
      logic.Do(r,p)( (r,p) => PartialTile.norm(r.rasterExtent, p))
    }

    def handleFullTile(r: Op[Raster]) = {
      logic.Do(r)( r => FullTile(r.rasterExtent))
    }

    def handleNoDataTile = NoDataTile

    def reducer(mapResults: List[B]):Seq[B] = mapResults
  }

  describe("ZonalSummary") {
    it("zonal summary summarizes one raster") {
      val rData = new IntArrayRasterData(Array.ofDim[Int](9), 3, 3).map { i => 1 }
      val extent = Extent(0,0,90,90)
      val rasterExtent = RasterExtent(extent,30,30,3,3)
      val r = Raster(rData, rasterExtent)

      val zone = extent.asFeature(())

      val summaryOp = MockTiledPolygonalZonalSummary(r,zone)

      val result = server.run(summaryOp)

      result should equal (List(FullTile(rasterExtent)))
    }

    it("should handle tiled stuff") {      
      def getTile(i: Int, data: Int) = {
        val rData = new IntArrayRasterData(Array.fill[Int](100)(data), 10, 10)
        val row = i % 4
        val col = i - (row * 4)
        val extent = Extent(row*10, col*10, (row+1)*10, (col+1)*10)
        val rasterExtent = RasterExtent(extent, 1, 1, 10, 10)
        Raster(rData, rasterExtent)
      }

      val tiles = Array[Int](1,2,3,4,5,6,7,8,9,1,2,3,4,5,6,7)
        .zipWithIndex
        .map { case (data, i) => getTile(i, data) }

      val tileLayout = TileLayout(4, 4, 10, 10)

      val rasterExtent = RasterExtent(Extent(0,0,40,40),1,1,40,40)
      val rData = new TileArrayRasterData(tiles, tileLayout, rasterExtent)

      val raster = new Raster(rData, rasterExtent)

      val zone = Extent(15,10,30,20).asFeature(())
      
      val summaryOp = MockTiledPolygonalZonalSummary(raster, zone)
      val result = server.run(summaryOp).toSet

      val e = Extent(15,10,20,20).asFeature(()).mapGeom(a => {a.normalize(); a})

      Set(FullTile(RasterExtent(Extent(20,10,30,20),1,1,10,10)),
          PartialTile(RasterExtent(Extent(10,10,20,20),1,1,10,10),e)) should equal (result)

      val tileSums = zonal.Sum.createTileResults(rData, rasterExtent) 
      val sumOp = zonal.Sum(raster, zone, tileSums)
      val sumResult = server.run(sumOp)
      sumResult should equal (250)

      val tileSumsD = zonal.SumDouble.createTileResults(rData, rasterExtent) 
      val sumDOp = zonal.SumDouble(raster, zone, tileSumsD)
      val sumDResult = server.run(sumDOp)
      sumDResult should equal (250.0)

      val tileMins = zonal.Min.createTileResults(rData, rasterExtent)
      val minOp = zonal.Min(raster, zone, tileMins)
      val minResult = server.run(minOp)
      minResult should equal (1)

      val tileMaxes = zonal.Max.createTileResults(rData, rasterExtent)
      val maxOp = zonal.Max(raster, zone, tileMaxes)
      val maxResult = server.run(maxOp)
      maxResult should equal (2)

      val tileHistograms = zonal.Histogram.createTileResults(rData, rasterExtent)
      val histOp = zonal.Histogram(raster, zone, tileHistograms)
      val h = server.run(histOp)
      h.getItemCount(1) should equal (50)
      h.getItemCount(2) should equal (100)
    }
  }
}
