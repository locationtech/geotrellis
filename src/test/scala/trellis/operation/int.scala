package trellis.operation

import java.io.{File,FileInputStream,FileOutputStream}
import scala.math.{max,min,sqrt}

import trellis.geometry.Polygon

import trellis.data.ColorBreaks
import trellis.raster.IntRaster
import trellis.{Extent,RasterExtent}

import trellis.stat._
import trellis.process._
import trellis.constant._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class IntSpecX extends Spec with MustMatchers with ShouldMatchers {
  val rasterExtent = RasterExtent(Extent(0.0, 0.0, 100.0, 80.0), 20.0, 20.0, 5, 4)

  describe("The operations include") {
    val server = TestServer()

    val nd = NODATA

    val data1 = Array(12, 12, 13, 14, 15,
                      44, 91, nd, 11, 95,
                      12, 13, 56, 66, 66,
                      44, 91, nd, 11, 95)
    val raster1 = IntRaster(data1, 4, 5, rasterExtent)

    val data2 = Array(nd, nd, nd, 1, 1,
                      nd, nd, nd, 1, 1,
                      nd, nd, nd, 1, 1,
                      nd, nd, nd, 1, 1)
    val raster2 = IntRaster(data2, 4, 5, rasterExtent)

    val data3 = Array(nd, nd, 33, 1, 1,
                      18, 88, 11, 1, 14,
                      33, 3, 10, 1, 17,
                      18, 12, nd, 34, 77)
    val raster3 = IntRaster(data3, 4, 5, rasterExtent)

    it("dispatch is not yet implemented") {
      val G = BuildRasterExtent(0.0, 0.0, 100.0, 100.0, 100, 100)
      //evaluating { G.dispatch(server) } should produce [Exception];
    }

    it("Literal") {
      val L = Literal(33)
      server.run(L) must be === 33
    }

    it("CreateRaster") {
      val G = BuildRasterExtent(0.0, 0.0, 100.0, 100.0, 100, 100)
      val C = CreateRaster(G)
      val raster = server.run(C)
      raster.get(0, 0) must be === NODATA
      raster.get(1, 0) must be === NODATA
      raster.get(0, 1) must be === NODATA
    }

    it("LoadFile") {
      val L = LoadFile("src/test/resources/fake.img.arg")
      //println("abc")
      val raster = server.run(L)
      //println("xyz")

      raster.get(0, 0) must be === 1
      raster.get(3, 3) must be === 52
    }


    it("LoadFile, take 2") {
      val L2 = LoadFile("src/test/resources/fake.img.arg")
      val raster2 = server.run(L2)
      raster2.get(0, 0) must be === 1
      raster2.get(3, 3) must be === 52
    }

    it("LoadFile, w/ resampling") {
      //println("AAAAAA")
      val G1 = LoadRasterExtentFromFile("src/test/resources/fake.img.arg")
      val geo1 = server.run(G1)

      //println("BBBBBBBB")
      val G2 = BuildRasterExtent( geo1.extent.xmin, geo1.extent.ymin, geo1.extent.xmax, geo1.extent.ymax, 2, 2) 
      val L = LoadFile("src/test/resources/fake.img.arg", G2)
      val raster = server.run(L)
      //println("CCC %s cols=%d rows=%d (len=%d)".format(raster, raster.cols, raster.rows, raster.data.length))
      //println(raster.asciiDraw)
      raster.get(0, 0) must be === 18
      //println("D1")
      raster.get(1, 0) must be === 20
      //println("D2")
      raster.get(0, 1) must be === 50
      //println("D3")
      raster.get(1, 1) must be === 52
      //println("D4")
    }
  
    it("LoadResampledArgFile, take 2") {
      val G1 = LoadRasterExtentFromFile("src/test/resources/fake.img.arg")
      val geo1 = server.run(G1)

      val G2 = BuildRasterExtent( geo1.extent.xmin, geo1.extent.ymin, geo1.extent.xmax, geo1.extent.ymax, 2, 2) 
      val L = LoadFile("src/test/resources/fake.img.arg", G2)
      val raster = server.run(L)

      raster.get(0, 0) must be === 18
      raster.get(1, 0) must be === 20
      raster.get(0, 1) must be === 50
      raster.get(1, 1) must be === 52
    }

    it("ResampleRaster") {
      val L = LoadFile("src/test/resources/quad.arg")
      val F = ResampleRaster(L, 4, 4)
      val raster = server.run(F)

      raster.cols must be === 4
      raster.rows must be === 4
      raster.data(0) must be === 1
      raster.data(3) must be === 2
      raster.data(8) must be === 3
      raster.data(11) must be === 4
    }

    it("test Literal implicit") {
      import trellis.operation.Literal._
      val G1 = LoadRasterExtentFromFile("src/test/resources/fake.img.arg")
      val geo1 = server.run(G1)
      val L = LoadFile("src/test/resources/fake.img.arg", geo1)
    }

    it("should LoadArgFileChunk with subextents that are within the arg extent") {
      // fake2 is 4x4, has a cellwidth of 10, and an origin of x = -100, y = 100
      // load the files, and do the basic constant multiplication for weighting
      val G = BuildRasterExtent(xmin = -90, ymin = 20,
                            xmax = -80, ymax = 40,
                            cols = 1, rows = 1)
      val L = LoadFile("src/test/resources/fake2.img.arg", G)
      //println(L.toString)
    }

    it("CopyRaster") {
      val W = Literal(raster1)
      val R = CopyRaster(W)

      val orig = server.run(W)
      val copy = server.run(R)

      copy must be === orig
    }

    it("BuildArrayHistogram") {
      val H = BuildArrayHistogram(Literal(raster1), 101)
      val histo = server.run(H)
      //println(histo.toJSON)
      //println(histo.getValues.toList)

      histo.getTotalCount must be === 18
      histo.getItemCount(11) must be === 2
      histo.getItemCount(12) must be === 3

      histo.getQuantileBreaks(4) must be === Array(12, 15, 66, 95)
    }

    it("BuildCompressedArrayHistogram") {
      val H = BuildCompressedArrayHistogram(Literal(raster1), 1, 101, 50)
      val histo = server.run(H)
      //println(histo.toJSON)
      //println(histo.getValues.toList)
      //println(histo.getTotalCount)

      histo.getTotalCount must be === 18
      histo.getItemCount(11) must be === 5

      histo.getQuantileBreaks(4) must be === Array(11, 15, 65, 95)
    }

    it("BuildMapHistogram") {
      //println(raster1)
      val H = BuildMapHistogram(Literal(raster1))
      val histo = server.run(H)

      //println(histo.toJSON)
      //println(histo.getValues.toList)

      histo.getTotalCount must be === 18
      histo.getItemCount(11) must be === 2
      histo.getItemCount(12) must be === 3

      histo.getQuantileBreaks(4) must be === Array(12, 15, 66, 95)
    }

    it("FindClassBreaks") {
      val H = BuildArrayHistogram(Literal(raster1), 101)
      val F = FindClassBreaks(H, 4)
      server.run(F) must be === Array(12, 15, 66, 95)
    }

    it("FindColorBreaks") {
      val H = BuildArrayHistogram(Literal(raster1), 101)
      val (g, y, o, r) = (0x00FF00, 0xFFFF00, 0xFF7F00, 0xFF0000)
      val colors = Array(g, y, o, r)
      val F = FindColorBreaks(H, colors)
      val cb = server.run(F)
      cb.breaks.toList must be === List((12, g), (15, y), (66, o), (95, r))
    }

    it("GenerateStatistics") {
      val R = LoadFile("src/test/resources/quad.arg")
      val S = GenerateStatistics(BuildMapHistogram(R))
      val stats = server.run(S)
      println(stats)

      val dev = sqrt((2 * (0.5 * 0.5) + 2 * (1.5 * 1.5)) / 4)
      val expected = Statistics(2.5, 3, 1, dev, 1, 4)

      stats must be === expected
    }

    it("DeviationRaster") {
      val newServer = TestServer()
      val R1 = LoadFile("src/test/resources/quad.arg")
      val R2 = LoadFile("src/test/resources/quad.arg")
      val H = BuildMapHistogram(R1)
      val S = DeviationRaster(R2, H, 1000)
      val raster = newServer.run(S)

      raster.data(0) must be === -1341
      raster.data(10) must be === -447
      raster.data(200) must be === 447
      raster.data(210) must be === 1341
    }


/*
    it("FindMinMax") {
      val R = Literal(raster1)
      val F = FindMinMax(R)
      F.run(server) must be === (11, 95)
    }

*/
/*
    it("CsvIntMap") {
      val C   = CsvIntMap("src/test/resources/foo.csv", ",")
      val map = C.run(server)

      List(("alpha", 0), ("beta", 3), ("gamma", 6)).foreach {
        rtpl => List(("foo", 1), ("bar", 2), ("duh", 3)).foreach {
          ctpl => map((rtpl._1, ctpl._1)) must be === rtpl._2 + ctpl._2
        }
      }

      evaluating {
        CsvIntMap("src/test/resources/bad.csv", ",").run(server)
      } should produce [Exception];

    }

    it("BuildRasterExtent") {
      val G = BuildRasterExtent(0.0, 0.0, 100.0, 100.0, 100, 100, 102100)
      val geo = G.run(server)
      geo.extent.xmin must be === 0.0
      geo.extent.ymax must be === 100.0
      geo.cellwidth must be === 1.0
      geo.cellheight must be === 1.0
    }

    it("CropRasterExtent") {
      val G = BuildRasterExtent(0.0, 0.0, 10000.0, 10000.0, 100, 100, 102100)
      val C = CropRasterExtent(G, 2303.0, 4305.0, 5288.0, 5790.0)
      val geo = C.run(server)
      geo.extent.xmin must be === 2300.0
      geo.extent.ymin must be === 4300.0
      geo.extent.xmax must be === 5300.0
      geo.extent.ymax must be === 5800.0
    }

    it("CropRaster") {
      val h = 100
      val w = 100
      val geo = new RasterExtent(0.0, 0.0, 100.0, 100.0, 1.0, 1.0, w, h)
      val data = Array.ofDim[Int](h * w)
      for (i <- 0 until h * w) { data(i) = i % w }

      val R = Literal(IntRaster(data, w, h, geo))
      val C = CropRaster(R, 23.4, 49.3, 88.3, 93.2)
      val raster = C.run(server)

      println(raster.rasterExtent)
      raster.rasterExtent.extent.xmin must be === 23.0
      raster.rasterExtent.extent.ymin must be === 49.0
      raster.rasterExtent.extent.xmax must be === 89.0
      raster.rasterExtent.extent.ymax must be === 94.0

      raster.data(0) must be === 23
      raster.data(67) must be === 24
    }

    it("ChunkRasterExtent") {
      val G = BuildRasterExtent(0.0, 0.0, 100.0, 100.0, 100, 100, 102100)
      val C = ChunkRasterExtent(G, 2, 4)

      val chunks = C.run(server)
      val len = chunks.length
      len must be === 8

      for(i <- 0 until 8) {
        val G2  = chunks(i)
        val geo = G2.run(server)
        val col = i % 2
        val row = i / 2
        geo.cols must be === 50
        geo.rows must be === 25
        geo.extent.xmin must be === col * 50.0
        geo.extent.ymin must be === row * 25.0
      }

    }
*/
    val f2 = (a:Array[Int], cols:Int, rows:Int, xmin:Double, ymin:Double,
             cellsize:Double, srs:Int) => {
      val g = RasterExtent(Extent(xmin, ymin, xmin + cellsize * cols, ymin + cellsize * rows),
                               cellsize, cellsize, cols, rows)
      IntRaster(a, cols, rows, g)
    }

    val f = (a:Array[Int], cols:Int, rows:Int, xmin:Double, ymin:Double,
             cellsize:Double) => f2(a, cols, rows, xmin, ymin, cellsize, 999)

    val h = (a:Array[Int], cols:Int, rows:Int) => {
      f(a, cols, rows, 0.0, 0.0, 1.0)
    }

/*
    val testNormalizer = (a1:Array[Int], a2:Array[Int], cols:Int, rows:Int,
                          zMin:Int, zMax:Int) => {
      val given  = h(a1, cols, rows)
      val expect = h(a2, cols, rows)

      val W = CopyRaster(Literal(given))
      val r = W.run(server)

      val result1 = Normalize(W, zMin, zMax).run(server)
      val ok1 = expect.equal(result1)
      if(!ok1) {
        println(expect)
        println(result1)
      }
      ok1 must be === true

      val (zzmin, zzmax) = r.findMinMax
      //val result2 = Normalize2(W, (1, 100), (zMin, zMax)).run(server)
      val result2 = Normalize2(W, (zzmin, zzmax), (zMin, zMax)).run(server)
      val ok2 = expect.equal(result2)
      if(!ok2) {
        println(expect)
        println(result2)
      }
      ok2 must be === true
    }

    it("should normalize with alternate min/max") {
      val r  = f(Array(1, 10, 5, 20), 2, 2, 0.0, 0.0, 3.0)
      val r2 = Normalize(CopyRaster(Literal(r)), 1, 100).run(server)
      val r3 = Normalize2(CopyRaster(Literal(r)), (1, 40), (1, 100)).run(server)

      r2.data must be === Array(1, 47, 21, 100)
      r3.data must be === Array(1, 23, 11, 49)
    }

    it("should normalize 0 through 1") {
      testNormalizer(Array(1, nd, nd, 1),
                     Array(100, nd, nd, 100),
                     2, 2, 1, 100)
    }

    it("should normalize -1 through +1") {
      testNormalizer(Array(-1, nd, 1, nd),
                     Array(1, nd, 100, nd),
                     2, 2, 1, 100)
    }

    it("should normalize across negative numbers") {
      testNormalizer(Array(-9, nd, 4, 2, 2, 7, -2, -1, nd),
                     Array(1, nd, 81, 69, 69, 100, 44, 50, nd),
                     3, 3, 1, 100)
    }

    it("should normalize an empty raster") {
      testNormalizer(Array(nd, nd, nd, nd),
                     Array(nd, nd, nd, nd),
                     2, 2, 1, 100)
    }

    it("should be able to stitch rasters together") {
      val r1 = f(Array(1, 1,
                       1, 1), 2, 2, 0.0, 0.0, 3.0)
      val r2 = f(Array(2, 2, 2,
                       2, 2, 2), 3, 2, 6.0, 0.0, 3.0)
      val r3 = f(Array(3, 3,
                       3, 3,
                       3, 3), 2, 3, 0.0, 6.0, 3.0)
      val r4 = f(Array(4, 4, 4,
                       4, 4, 4,
                       4, 4, 4), 3, 3, 6.0, 6.0, 3.0)

      val rG = f(Array(1, 1, 2, 2, 2,
                       1, 1, 2, 2, 2,
                       3, 3, 4, 4, 4,
                       3, 3, 4, 4, 4,
                       3, 3, 4, 4, 4), 5, 5, 0.0, 0.0, 3.0)
      
      val Rs = Array(r1, r2, r3, r4).map { Literal(_) }
      val S  = StitchIntRasters(Rs)
      val rOut = S.run(server)
      rOut.equal(rG) must be === true
    }

    it("should explode when stitching at different resolutions") {
      val r1 = f(Array(1), 1, 1, 0.0, 0.0, 3.0)
      val r2 = f(Array(2), 1, 1, 2.0, 0.0, 4.0)
      
      val S = StitchIntRasters(Array(r1, r2).map { Literal(_) })
      evaluating { S.run(server) } should produce [Exception];
    }

    it("should explode when stitching of different projections") {
      val r1 = f2(Array(1), 1, 1, 0.0, 0.0, 4.0, 999)
      val r2 = f2(Array(2), 1, 1, 2.0, 0.0, 4.0, 998)
      
      val S = StitchIntRasters(Array(r1, r2).map { Literal(_) })
      evaluating { S.run(server) } should produce [Exception];
    }

    it("should be able to stitch rasters sparsely") {
      val r1 = f(Array(1, 1,
                       1, 1), 2, 2, 3.0, 1.0, 1.0)
      var r2 = f(Array(2, 2, 2,
                       2, 2, 2), 3, 2, 0.0, 8.0, 1.0)
      var r3 = f(Array(3, 3, 3, 3,
                       3, 3, 3, 3,
                       3, 3, 3, 3), 4, 3, 5.0, 5.0, 1.0)

      val rG = f(Array(nd, nd, nd, 1, 1, nd, nd, nd, nd,
                       nd, nd, nd, 1, 1, nd, nd, nd, nd,
                       nd, nd, nd, nd, nd, nd, nd, nd, nd,
                       nd, nd, nd, nd, nd, nd, nd, nd, nd,
                       nd, nd, nd, nd, nd, 3, 3, 3, 3,
                       nd, nd, nd, nd, nd, 3, 3, 3, 3,
                       nd, nd, nd, nd, nd, 3, 3, 3, 3,
                       2, 2, 2, nd, nd, nd, nd, nd, nd,
                       2, 2, 2, nd, nd, nd, nd, nd, nd), 9, 9, 0.0, 1.0, 1.0)
      
      val S = StitchIntRasters(Array(r1, r2, r3).map { Literal(_) })
      val rOut = S.run(server)
      rOut.equal(rG) must be === true
    }

    it("should write PNG data") {
      val R = CopyRaster(Literal(f(Array(1, 2, 3,
                                            4, 5, 6,
                                            7, 8, 9),
                                      3, 3, 0.0, 0.0, 1.0)))
      val colorBreaks = Array((2, 0xFF0000), (5, 0x00FF00), (10, 0x0000FF))
      val noDataColor = 0x000000
      val transparent = true

      val P1 = RenderPNG(R, colorBreaks, noDataColor, transparent)
      val bytes  = P1.run(server)

      val fyle = File.createTempFile("flarg", ".png");
      val fos  = new FileOutputStream(fyle)
      fos.write(bytes)
      fos.close

      val file   = File.createTempFile("blarg", ".png")
      val path = file.getPath

      val P2 = WritePNGFile(R, path, colorBreaks,
                            noDataColor, transparent)
      P2.run(server)
      val fis    = new FileInputStream(file)
      val bytes2 = Array.ofDim[Byte](file.length.toInt)
      fis.read(bytes2)

      bytes.length must be === bytes2.length
      bytes.toList must be === bytes2.toList
    }

*/
    val a = Array(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val R = CopyRaster(Literal(f(a, 3, 3, 0.0, 0.0, 1.0)))

    val a2 = Array(2, 2, 2, 2, 2, 2, 2, 2, 2)
    val R2 = CopyRaster(Literal(f(a2, 3, 3, 0.0, 0.0, 1.0)))

    val a3 = Array(nd, nd, 1, 2, 2, 2, 3, 3, nd)
    val y3 = Array(1, 1, 1, 2, 2, 2, 3, 3, 1)
    val z3 = Array(0, 0, 1, 2, 2, 2, 3, 3, 0)
    val R3 = CopyRaster(Literal(f(a3, 3, 3, 0.0, 0.0, 1.0)))

/*
    it("should recover from weird color breaks") {
      val breaks1 = Array((2, 0xFF0000), (5, 0x00FF00), (10, 0x0000FF))
      val breaks2 = Array((2, 0xFF0000), (5, 0x00FF00), (8, 0x0000FF))
      val noDataColor = 0x000000
      val transparent = true

      val P1 = RenderPNG(R, breaks1, 0, true)
      val P2 = RenderPNG(R, breaks2, 0, true)

      P1.run(server) must be === P2.run(server)
    }

*/
    // unary local
    it("Identity") {
      server.run(Identity(R)).data.asArray must be === a
    }
    it("Negate") {
      server.run(Negate(R)).data.asArray must be === a.map { _ * -1 }
    }
    it("AddConstant") {
      server.run(AddConstant(R, 10)).data.asArray must be === a.map { _ + 10 }
    }
    it("MultiplyConstant") {
      server.run(MultiplyConstant(R, 8)).data.asArray must be === a.map { _ * 8 }
    }
    it("SubtractConstant") {
      server.run(SubtractConstant(R, 5)).data.asArray must be === a.map { _ - 5 }
    }
    it("DivideConstant") {
      server.run(DivideConstant(R, 2)).data.asArray must be === a.map { _ / 2 }
    }
    it("MaxConstant") {
      server.run(MaxConstant(R, 6)).data.asArray must be === a.map { max(_, 6) }
    }
    it("MinConstant") {
      server.run(MinConstant(R, 6)).data.asArray must be === a.map { min(_, 6) }
    }

    it("IfCell") {
      server.run(IfCell(R, _ > 6, 6)).data.asArray must be === a.map { min(_, 6) } 
    }

    it ("IfCell, second test") {
      server.run(IfCell(R, x => x > 3, 3)).data.asArray must be === a.map { min(_, 3) } 
    }

    it ("IfCell with an else clause (IfElse)") {
      server.run(IfCell(R, _ > 3, 1, 0)).data.asArray must be === a.map { x:Int => if (x > 3) 1 else 0 } 
    }

    it ("DoCell") {
      server.run(DoCell(R, _ + 1)).data.asArray must be === a.map { _ + 1 }
    }

    // binary local


/*
    it("Subtract") {
      Subtract(R, R2).run(server).data must be === a.zip(a2).map { a => a._1 - a._2 }
      Subtract(R, R3).run(server).data must be === a.zip(z3).map { a => a._1 - a._2 }
      Subtract(R3, R).run(server).data must be === z3.zip(a).map { a => a._1 - a._2 }
    }
    it("Divide") {
      Divide(R, R2).run(server).data must be === a.zip(a2).map { a => a._1 / a._2 }
    }
    it("Mask") {
      Mask(R, R2, 2, 0).run(server).data must be === a.map { a => 0 }
      Mask(R, R2, 3, 0).run(server).data must be === a

      val M = Mask(R, R2, 3, 0)
      M.getRasters.toList must be === List(R, R2)
    }
    it("InverseMask") {
      InverseMask(R, R2, 2, 0).run(server).data must be === a
      InverseMask(R, R2, 3, 0).run(server).data must be === a.map { a => 0 }
    }

    it("IfCell invocation of BinaryIfCell") {
      IfCell(R, R2, (z1,z2) => z1 > z2, 0, 1 ).run(server).data must be === a.zip(a2).map { a => if (a._1 > a._2) 0 else 1 }
        
    }

    it ("BinaryDoCell") {
      val B = BinaryDoCell(R, R2, (z1, z2) => z1 + z2)
      B.run(server).data must be === a.zip(a2).map { a => a._1 + a._2 }
    }

    it ("BinaryIfCell") {
      val B = BinaryIfCell(R, R2, (z1, z2) => z1 > z2, 0)
      B.run(server).data must be === a.zip(a2).map { a => if (a._1 > a._2) 0 else a._1 }
    }


    // multi local
    it("Add") {
      Add(R, R).run(server).data must be === a.map { a => a + a }
      Add(R, R, R).run(server).data must be === a.map { a => a + a + a }
      Add(R, R, R, R).run(server).data must be === a.map { a => a + a + a + a }
      Add(R, R, R, R, R).run(server).data must be === a.map { a => a + a + a + a + a}
      Add(R, R, R, R, R).run(server).data must be === a.map { a => a + a + a + a + a}
      Add(R, R, R, R, R, R).run(server).data must be === a.map { a => a + a + a + a + a + a}
    }

    it("Add(ND)") {
      Add(R3, R).run(server).data must be === z3.zip(a).map { a => a._1 + a._2 }
      Add(R3, R, R).run(server).data must be === z3.zip(a).map { a => a._1 + a._2 + a._2 }
      Add(R3, R, R, R).run(server).data must be === z3.zip(a).map { a => a._1 + a._2 + a._2 + a._2 }
      Add(R3, R, R, R, R).run(server).data must be === z3.zip(a).map { a => a._1 + a._2 + a._2 + a._2 + a._2}
      Add(R3, R, R, R, R, R).run(server).data must be === z3.zip(a).map { a => a._1 + a._2 + a._2 + a._2 + a._2 + a._2}

      val q = Array(nd, nd, nd, nd)
      val Rq = CopyRaster(Literal(f(q, 2, 2, 0.0, 0.0, 1.0)))
      Add(Rq, Rq, Rq, Rq, Rq, Rq).run(server).data must be === q
    }

    it("Multiply") {
      Multiply(R, R).run(server).data must be === a.map { a => a * a }
      Multiply(R, R, R).run(server).data must be === a.map { a => a * a * a }
      Multiply(R, R, R, R).run(server).data must be === a.map { a => a * a * a * a }
      Multiply(R, R, R, R, R).run(server).data must be === a.map { a => a * a * a * a * a }
    }

    it("Multiply(ND)") {
      Multiply(R, R3).run(server).data must be === a.zip(y3).map { a => a._1 * a._2 }
      Multiply(R, R3, R3).run(server).data must be === a.zip(y3).map { a => a._1 * a._2 * a._2 }
      Multiply(R, R3, R3, R3).run(server).data must be === a.zip(y3).map { a => a._1 * a._2 * a._2 * a._2 }
      Multiply(R, R3, R3, R3, R3).run(server).data must be === a.zip(y3).map { a => a._1 * a._2 * a._2 * a._2 * a._2 }
    }
*/
/*
    it("CreateSimplePolygon") {
      val tpls = Array((0.0, 0.0), (10.0, 0.0), (0.0, 10.0), (0.0, 0.0))

      val P      = CreateSimplePolygon(tpls, 13)
      val result = P.run(server)
      val goal   = Polygon(tpls, 13, null)
      result.value must be === goal.value
      result.getCoordTuples must be === goal.getCoordTuples

    }

    it("BurnPolygon") {
      val ax = Array.ofDim[Int](400)
      for(i <- 0 until ax.length) { ax(i) = NODATA }
      val Rx = CopyRaster(Literal(f(ax, 20, 20, 13.1, 19.6, 5.0)))

      val tpls = Array((20.9, 26.1), (84.2, 44.1), (64.2, 102.4), (20.9, 26.1))
      val P    = CreateSimplePolygon(tpls, 13)

      val B = BurnPolygon(Rx, P)
  
      val output = B.run(server)

      Console.printf(output.asciiDraw)

      // NOTE that we're adding a leading newline to make the image easier to "see"
      val expected = """
........................................
........................................
....................0D..................
....................0D..................
..................0D0D0D................
..................0D0D0D................
................0D0D0D0D................
................0D0D0D0D0D..............
..............0D0D0D0D0D0D..............
..............0D0D0D0D0D0D..............
............0D0D0D0D0D0D0D0D............
............0D0D0D0D0D0D0D0D............
..........0D0D0D0D0D0D0D0D0D............
..........0D0D0D0D0D0D0D0D0D0D..........
........0D0D0D0D0D0D0D0D0D0D0D..........
........0D0D0D0D0D0D0D0D................
......0D0D0D0D0D0D......................
......0D0D0D............................
....0D..................................
........................................
"""

      ("\n" + output.asciiDraw) must be === expected
    }

    val pf = (z:Int, pts:Array[(Double, Double)]) => {
      CreateSimplePolygon(pts, z)
    }
    it("BurnPolygons") {
      // min =   13.1    19.6
      // max = 1013.1, 1019.6
      val h = 1000
      val w = 1000
      val ax = Array.fill[Int](h * w)(NODATA)
      val Rx = CopyRaster(Literal(f(ax, w, h, 13.1, 19.6, 1.0)))

      // build the polygons
      val P1 = pf(13, Array((40.0, 40.0), (600.0, 100.0), (300.0, 400.0), (40.0, 40.0)))
      val P2 = pf(56, Array((700.0, 100.0), (500.0, 500.0), (700.0, 600.0), (700.0, 100.0)))
      val P3 = pf(90, Array((900.0, 300.0), (200.0, 700.0), (600.0, 950.0), (900.0, 300.0)))

      // build the burn operation
      val B = BurnPolygons(Rx, Array(P1, P2, P3))

      // generate a PNG
      val breaks1 = Array((30, 0xFF0000), (60, 0x00FF00), (100, 0x0000FF))
      val cb = ColorBreaks(breaks1)
      //val G = RenderPNG(B, breaks1, 0x000000, true)
      val L = Literal(cb)
      val G = RenderPNG(B, L, 0x000000, true)
      val bytes = G.run(server)

      // write the output
      val file = File.createTempFile("triangles", ".png")
      val fos  = new FileOutputStream(file)
      fos.write(bytes)
      fos.close
    }
*/

/*
    it("PolygonalZonalHistograms") {
      // min =   13.1    19.6
      // max = 1013.1, 1019.6
      val rows = 100
      val cols = 100
      val ax   = Array.fill[Int](rows * cols)(NODATA)
      val Rx   = CopyRaster(Literal(f(ax, cols, rows, 13.1, 19.6, 10.0)))

      for(row <- 0 until rows) {
        val d = (2347 * row) % 13 + 1
        val y = row * cols
        for(col <- 0 until cols) {
          ax(y + col) = (col * 23) % d
        }
      }

      // build the polygons
      val P1 = pf(13, Array((40.0, 40.0), (600.0, 100.0), (300.0, 400.0), (40.0, 40.0)))
      val P2 = pf(56, Array((700.0, 100.0), (500.0, 500.0), (700.0, 600.0), (700.0, 100.0)))
      val P3 = pf(90, Array((900.0, 300.0), (200.0, 700.0), (600.0, 950.0), (900.0, 300.0)))

      val Z = PolygonalZonalHistograms(Array(P1, P2, P3), Rx, 101)

      val histmap = Z.run(server)

      val h13 = histmap(13)
      h13.getItemCount(0) must be === 222
      h13.getItemCount(1) must be === 135
      h13.getItemCount(2) must be === 109
      h13.getItemCount(3) must be === 88

      val h56 = histmap(56)
      h56.getItemCount(4) must be === 52
      h56.getItemCount(5) must be === 42
      h56.getItemCount(6) must be === 29
      h56.getItemCount(7) must be === 27

      val h90 = histmap(90)
      h90.getItemCount(8) must be === 57
      h90.getItemCount(9) must be === 41
      h90.getItemCount(10) must be === 29
      h90.getItemCount(11) must be === 19

    }
*/
  }
}
