package geotrellis.raster.op

import java.io.{File,FileInputStream,FileOutputStream}
import scala.math.{max,min,sqrt}

import geotrellis.geometry.Polygon

import geotrellis._
import geotrellis.statistics._
import geotrellis.process._
import geotrellis.data.ColorBreaks
import geotrellis._
import geotrellis.raster.op.local._
import geotrellis.raster.op.extent.GetRasterExtent
import geotrellis.logic._
import geotrellis.raster.op.transform.{ResampleRaster}
import geotrellis.statistics.op.stat._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class IntSpecX extends FunSpec with MustMatchers with ShouldMatchers {
  val rasterExtent = RasterExtent(Extent(0.0, 0.0, 100.0, 80.0), 20.0, 20.0, 5, 4)

  describe("The operations include") {
    val server = TestServer()

    val nd = NODATA

    val data1 = Array(12, 12, 13, 14, 15,
                      44, 91, nd, 11, 95,
                      12, 13, 56, 66, 66,
                      44, 91, nd, 11, 95)
    val raster1 = Raster(data1, rasterExtent)

    val data2 = Array(nd, nd, nd, 1, 1,
                      nd, nd, nd, 1, 1,
                      nd, nd, nd, 1, 1,
                      nd, nd, nd, 1, 1)
    val raster2 = Raster(data2, rasterExtent)

    val data3 = Array(nd, nd, 33, 1, 1,
                      18, 88, 11, 1, 14,
                      33, 3, 10, 1, 17,
                      18, 12, nd, 34, 77)
    val raster3 = Raster(data3, rasterExtent)

    it("dispatch is not yet implemented") {
      val G = GetRasterExtent(0.0, 0.0, 100.0, 100.0, 100, 100)
      //evaluating { G.dispatch(server) } should produce [Exception];
    }

    it("Literal") {
      val L = Literal(33)
      server.run(L) must be === 33
    }

    it("CreateRaster") {
      val G = GetRasterExtent(0.0, 0.0, 100.0, 100.0, 100, 100)
      val C = CreateRaster(G)
      val raster = server.run(C)
      raster.get(0, 0) must be === NODATA
      raster.get(1, 0) must be === NODATA
      raster.get(0, 1) must be === NODATA
    }

    it("LoadFile") {
      val L = io.LoadFile("src/test/resources/fake.img8.arg")
      val raster = server.run(L)

      raster.get(0, 0) must be === 49 
      raster.get(3, 3) must be === 4 
    }


    it("LoadFile, take 2") {
      val L2 = io.LoadFile("src/test/resources/fake.img8.arg")
      val raster2 = server.run(L2)
      raster2.get(0, 0) must be === 49
      raster2.get(3, 3) must be === 4 
    }

    it("LoadFile, w/ resampling") {
      val G1 = io.LoadRasterExtentFromFile("src/test/resources/fake.img8.arg")
      val geo1 = server.run(G1)

      val G2 = GetRasterExtent( geo1.extent.xmin, geo1.extent.ymin, geo1.extent.xmax, geo1.extent.ymax, 2, 2) 
      val L = io.LoadFile("src/test/resources/fake.img8.arg", G2)
      val raster = server.run(L)
      raster.get(0, 0) must be === 34
      raster.get(1, 0) must be === 36
      raster.get(0, 1) must be === 2
      raster.get(1, 1) must be === 4
    }
    it("GetRasterExtent") {
      val e = Extent(xmin = -90, ymin = 20,
                     xmax = -80, ymax = 40)
      val op = GetRasterExtent(e, 20, 30)
      val re = server.run(op)
      re.extent.xmin must be === -90
      re.cols must be === 20
    }
 
    it("LoadResampledArgFile, take 2") {
      val G1 = io.LoadRasterExtentFromFile("src/test/resources/fake.img8.arg")
      val geo1 = server.run(G1)

      val G2 = GetRasterExtent( geo1.extent.xmin, geo1.extent.ymin, geo1.extent.xmax, geo1.extent.ymax, 2, 2) 
      val L = io.LoadFile("src/test/resources/fake.img8.arg", G2)
      val raster = server.run(L)

      raster.get(0, 0) must be === 34
      raster.get(1, 0) must be === 36
      raster.get(0, 1) must be === 2 
      raster.get(1, 1) must be === 4
    }


    it("ResampleRaster") {
      val L = io.LoadFile("src/test/resources/quad8.arg")
      val F = transform.ResampleRaster(L, 4, 4)
      val raster = server.run(F)

      raster.cols must be === 4
      raster.rows must be === 4

      val d = raster.data.asArray.getOrElse(sys.error("argh"))

      d(0) must be === 1
      d(3) must be === 2
      d(8) must be === 3
      d(11) must be === 4
    }

    it("test Literal implicit") {
      import geotrellis.Literal
      val G1 = io.LoadRasterExtentFromFile("src/test/resources/fake.img8.arg")
      val geo1 = server.run(G1)
      val L = io.LoadFile("src/test/resources/fake.img8.arg", geo1)
    }

    it("should LoadArgFileChunk with subextents that are within the arg extent") {
      // fake2 is 4x4, has a cellwidth of 10, and an origin of x = -100, y = 100
      // load the files, and do the basic constant multiplication for weighting
      val G = GetRasterExtent(xmin = -90, ymin = 20,
                            xmax = -80, ymax = 40,
                            cols = 1, rows = 1)
      val L = io.LoadFile("src/test/resources/fake2.img8.arg", G)
    }

    it("BuildArrayHistogram") {
      val histo = server.run(GetHistogram(raster1, 101))
      //println(histo.toJSON)
      //println(histo.getValues.toList)

      histo.getTotalCount must be === 18
      histo.getItemCount(11) must be === 2
      histo.getItemCount(12) must be === 3

      histo.getQuantileBreaks(4) must be === Array(12, 15, 66, 95)
    }

    it("BuildMapHistogram") {
      val histo = server.run(GetHistogram(raster1))

      //println(histo.toJSON)
      //println(histo.getValues.toList)

      histo.getTotalCount must be === 18
      histo.getItemCount(11) must be === 2
      histo.getItemCount(12) must be === 3

      histo.getQuantileBreaks(4) must be === Array(12, 15, 66, 95)
    }

    it("FindClassBreaks") {
      val H = GetHistogram(Literal(raster1), 101)
      val F = GetClassBreaks(H, 4)
      server.run(F) must be === Array(12, 15, 66, 95)
    }

    it("FindColorBreaks") {
      val H = GetHistogram(Literal(raster1), 101)
      val (g, y, o, r) = (0x00ff00ff, 0xffff00ff, 0xff7f00ff, 0xff0000ff)
      val colors = Array(g, y, o, r)
      val F = GetColorBreaks(H, colors)
      val cb = server.run(F)
      cb.limits must be === Array(12, 15, 66, 95)
      cb.colors must be === Array(g, y, o, r)
    }

    it("GenerateStatistics") {
      val R = io.LoadFile("src/test/resources/quad8.arg")
      val S = GetStatistics(GetHistogram(R))
      val stats = server.run(S)

      val dev = sqrt((2 * (0.5 * 0.5) + 2 * (1.5 * 1.5)) / 4)
      val expected = Statistics(2.5, 3, 1, dev, 1, 4)

      stats must be === expected
    }

    it("StandardDeviation") {
      val newServer = TestServer()
      val R1 = io.LoadFile("src/test/resources/quad8.arg")
      val R2 = io.LoadFile("src/test/resources/quad8.arg")
      val H = GetHistogram(R1)
      val S:GetStandardDeviation = GetStandardDeviation(R2, H, 1000)
     
      val raster = newServer.run(S)

      val d = raster.data.asArray.getOrElse(sys.error("argh"))

      d(0) must be === -1341
      d(10) must be === -447
      d(200) must be === 447
      d(210) must be === 1341
    }

    val f2 = (a:Array[Int], cols:Int, rows:Int, xmin:Double, ymin:Double,
             cellsize:Double, srs:Int) => {
      val g = RasterExtent(Extent(xmin, ymin, xmin + cellsize * cols, ymin + cellsize * rows),
                               cellsize, cellsize, cols, rows)
      Raster(a, g)
    }

    val f = (a:Array[Int], cols:Int, rows:Int, xmin:Double, ymin:Double,
             cellsize:Double) => f2(a, cols, rows, xmin, ymin, cellsize, 999)

    val h = (a:Array[Int], cols:Int, rows:Int) => {
      f(a, cols, rows, 0.0, 0.0, 1.0)
    }

    val a = Array(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val r = Literal(f(a, 3, 3, 0.0, 0.0, 1.0))

    val a2 = Array(2, 2, 2, 2, 2, 2, 2, 2, 2)
    val r2 = Literal(f(a2, 3, 3, 0.0, 0.0, 1.0))

    val a3 = Array(nd, nd, 1, 2, 2, 2, 3, 3, nd)
    val y3 = Array(1, 1, 1, 2, 2, 2, 3, 3, 1)
    val z3 = Array(0, 0, 1, 2, 2, 2, 3, 3, 0)
    val r3 = Literal(f(a3, 3, 3, 0.0, 0.0, 1.0))

    def runArray(op:Op[Raster]) = {
      server.run(op).data.asArray.getOrElse(sys.error("argh")).toArray
    }

    // unary local
    it("Negate") {
      runArray(local.Negate(r)) must be === a.map { _ * -1 }
    }
    it("AddConstant") {
      runArray(local.AddConstant(r, 10)) must be === a.map { _ + 10 }
    }
    it("MultiplyConstant") {
      runArray(local.MultiplyConstant(r, 8)) must be === a.map { _ * 8 }
    }
    it("SubtractConstant") {
      runArray(SubtractConstant(r, 5)) must be === a.map { _ - 5 }
    }
    it("DivideConstant") {
      runArray(DivideConstant(r, 2)) must be === a.map { _ / 2 }
    }
    it("MaxConstant") {
      runArray(MaxConstant(r, 6)) must be === a.map { max(_, 6) }
    }
    it("MinConstant") {
      runArray(MinConstant(r, 6)) must be === a.map { min(_, 6) }
    }

    it("IfCell") {
      runArray(IfCell(r, _ > 6, 6)) must be === a.map { min(_, 6) } 
    }

    it ("IfCell, second test") {
      runArray(IfCell(r, x => x > 3, 3)) must be === a.map { min(_, 3) } 
    }

    it ("IfCell with an else clause (IfElse)") {
      runArray(IfCell(r, _ > 3, 1, 0)) must be === a.map { x:Int => if (x > 3) 1 else 0 } 
    }

    it ("DoCell") {
      runArray(DoCell(r, _ + 1)) must be === a.map { _ + 1 }
    }
  }
}
