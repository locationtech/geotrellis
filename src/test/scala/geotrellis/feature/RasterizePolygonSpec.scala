package geotrellis.feature.rasterize

import geotrellis._
import geotrellis.feature.op.geometry.{Buffer,GetCentroid}
import geotrellis.process._
import geotrellis.feature._
import math.{max,min,round}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import geotrellis.feature.op.geometry.GetEnvelope
import geotrellis.feature.op.geometry.Intersect
import PolygonRasterizer._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RasterizePolygonSpec extends FunSuite {
  test("Polygon Rasterization") {
      val e = Extent(0.0, 0.0, 10.0, 10.0)
      val g = RasterExtent(e, 1.0, 1.0, 10, 10)

      val data = (0 until 99).toArray
      val raster = Raster(data, g)
      val re = raster.rasterExtent

      val square  = Polygon( (1,9) :: (1,6) :: (4,6) :: (4,9) :: (1,9) :: Nil, () ) 
      val diamond = Polygon( List((3,7), (6,4), (3,1), (0,4), (3,7)), ())
      val triangle = Polygon( List((2,8),(5,5),(6,7), (6,7), (2,8)),() ) 

      // intersection on cell midpoint
      val square2 = Polygon( (1.0,9.0) :: (1.0,8.5) :: (1.0,6.0) :: (4.0, 6.0) :: (4.0, 8.5) :: (4.0, 9.0) :: (1.0, 9.0) :: Nil, () )

      val edgeTable = PolygonRasterizer.buildEdgeTable(square, re)       
      // y is flipped in grid coordinates
      assert( edgeTable.edges === Map( 1 -> List(Line(1,3,1,6,1,9,0),Line(1,3,4,6,4,9,0)))) 
      assert( edgeTable.edges.size === 1)
      assert( edgeTable.rowMin === 1)
      assert( edgeTable.rowMax === 3)

      val diamondTable = PolygonRasterizer.buildEdgeTable(diamond, re)
      //assert( diamondTable.edges === Map( 
      //  3 -> List(Line(3,6,3,1,1,1,1,1),Line(3,6,0,1,1,1,1,-1)),
      //  6 -> List(Line(6,9,3,1,1,1,1,-1),Line(6,9,0,1,1,1,1,1))
      //  )) 
      assert(diamondTable.edges.size === 2)
      assert( diamondTable.rowMin === 3)
      assert( diamondTable.rowMax === 8)

      val triangleTable = PolygonRasterizer.buildEdgeTable(triangle, re)
    //  assert( triangleTable.edges === Map (
    //    2 -> List(Line(2,5,2,1), Line(2,3,2,4)),
    //    3 -> List(Line(3,5,5,-0.5))
    //  ))
      assert(triangleTable.edges.size === 2)
      assert(triangleTable.rowMin === 2)
      assert(triangleTable.rowMax === 4)


      val f1 = (col:Int, row:Int, p:Polygon[Unit]) => 
        println("xx col: %d, row: %d".format(col,row))

      val r1 = Rasterizer.rasterize(square,re, (a:Unit) => 0x11) 
      println(r1.asciiDraw)

      // values match gdal
      for ( i <- 1 to 3; j <- 1 to 3) { 
        assert(r1.get(i,j) === 0x11) 
      }
    
      var sum = 0
      r1.foreach(f => if (f != NODATA) sum = sum + 1 )
      assert(sum === 9)
      
      val r2 = Rasterizer.rasterize(diamond, re, (a:Unit) => 0x22)
      println(r2.asciiDraw())
      assert(r2.get(3,3) === 0x22)
      for (i <- 2 to 4) { assert(r2.get(i,4) === 0x22) }
      for (i <- 1 to 5) { assert(r2.get(i,5) === 0x22) }
      for (i <- 1 to 5) { assert(r2.get(i,6) === 0x22) }
      for (i <- 2 to 4) { assert(r2.get(i,7) === 0x22) }
      assert(r2.get(3,8) === 0x22)
      
      sum = 0
      r2.foreach(f => if (f != NODATA) sum = sum + 1 )
      assert(sum === 18)

      val r3 = Rasterizer.rasterize(triangle, re, (a:Unit) => 0x33)  
      println(r3.asciiDraw())
      
      assert(r3.get(3,2) === 0x33)
      assert(r3.get(4,3) === 0x33)
      assert(r3.get(5,3) === 0x33)
      sum = 0
      r3.foreach(f => if (f != NODATA) sum = sum + 1 )
      assert(sum === 3)

      val r4 = Rasterizer.rasterize(square2, re, (a:Unit) => 0x44)
      println(r4.asciiDraw())
      
     // LoadWKT()
  }
  
  test("polygon rasterization: more complex polygons") {
	  val p1 = Polygon (List((-74.6229572569999, 41.5930024740001),
          (-74.6249086829999, 41.5854607480001),
          (-74.6087045219999, 41.572877582),
          (-74.6396698609999, 41.5479203780001),
          (-74.6134071899999, 41.5304959030001),
          (-74.6248611209999, 41.5210940920001),
          (-74.6080037309999, 41.510192955),
          (-74.61917188, 41.500007054),
          (-74.6868377089999, 41.5507426980001),
          (-74.6752089579999, 41.5646628200001),
          (-74.6776005779999, 41.573316585),
          (-74.6637320329999, 41.5691605160001),
          (-74.6623717069999, 41.5770289280001),
          (-74.6558314389999, 41.552671724),
          (-74.6494842519999, 41.5467347190001),
          (-74.6459184919999, 41.565179846),
          (-74.6344289929999, 41.5694043560001),
          (-74.6229572569999, 41.5930024740001)), ())
          
       val tileExtent = Extent( -88.57589314970001, 35.15178531379998, -70.29017892250002, 53.43749954099997)
       // val rasterExtent = RasterExtent(tileExtent, 0.008929,0.008929, 2048, 2048 )
       // val rasterExtent = RasterExtent(tileExtent, 0.008929,0.008929, 2048, 2048 )
       
       val cw1024 = 0.0178571428
       val cw = cw1024 * 2
       val rasterExtent = RasterExtent(tileExtent, cw, cw, 512, 512)
       // at 512, cells are:
       // 389, 332
       // 390, 332
       // 390, 333
       
       val r1 = Rasterizer.rasterize(p1, rasterExtent, (a:Unit) => 0x55)
      var sum = 0
      r1.foreach(f => if (f != NODATA) sum = sum + 1 ) 
      assert(sum === 3)
      assert(r1.get(389,332) === 0x55)
      assert(r1.get(390,332) === 0x55)
      assert(r1.get(390,333) === 0x55)
      println("sum: " + sum)
  }
  
  test("Rasterization tests from directory of WKT files") {
    val f = new java.io.File("src/test/resources/feature/")
    val fs = f.listFiles.filter(_.getPath().endsWith(".wkt"))
    //.foreach {
      
    fs.foreach( f => {
      val json = scala.io.Source.fromFile(f).mkString
      val filename = f.getName()
      println("Testing rasterization: " + filename)
      val count = Integer.parseInt(f.getName().subSequence(0, filename.length - 4).toString.split("_").last)
      println("count: " + count)
      val g1 = TestServer().run(io.LoadWkt(json))
      val p1 = Polygon(g1.geom, ())
      var sum = 0
      val re = RasterExtent( Extent(0, 0, 300, 300), 1, 1, 300, 300)
      val r = foreachCellByPolygon(p1, re, (x:Int, y:Int, p:Polygon[Unit]) => ( sum = sum + 1 ) )
      assert(sum === count)
    } )
    //val data1 = scala.io.Source.fromFile("src/test/resources/feature/polygon1.wkt").mkString
    //val g1 = TestServer().run(io.LoadWkt(data1))
    //val p1 = Polygon(g1.geom, ())
    //println(p1)
    //var sum = 0
    //val re = RasterExtent( Extent(0, 0, 300, 300), 1, 1, 300, 300)
    //val r = foreachCellByPolygon(p1, re, (x:Int, y:Int, p:Polygon[Unit]) => ( sum = sum + 1 ) )
    //assert(sum === 6722)
  }
}
