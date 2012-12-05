package geotrellis.feature

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
import geotrellis.feature.rasterize.Rasterizer

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RasterizeSpec extends FunSuite {
  test("Point Rasterization") {
      val s = TestServer()
      val e = Extent(0.0, 0.0, 10.0, 10.0)
      val g = RasterExtent(e, 1.0, 1.0, 10, 10)

      val data = (0 until 99).toArray
      val raster = Raster(data, g)
      val re = raster.rasterExtent
      
      val p = Point(1.0,2.0,"point one: ")
      val p2 = Point(9.5, 9.5, "point two: ")
      val p3 = Point(0.1, 9.9, "point three: ")
      
      val pointSet1 = PointSet(p :: p2 :: p3 :: Nil)

      var f2output:String = ""
      val f2 = (col:Int, row:Int, feature:Point[String]) => {
        println("in f2, feature is: " + feature)
        val z = raster.get(col,row)
        f2output = f2output + feature.data + z.toString
      }
      Rasterizer.foreachCellByPoint(p, re)(f2)
      assert(f2output === "point one: 81")

      f2output = ""
      Rasterizer.foreachCellByPoint(p2, re)(f2)
      assert( f2output === "point two: 9")
     
      f2output = ""
      Rasterizer.foreachCellByPoint(p3, re)(f2) 
      assert( f2output === "point three: 0")

      f2output = ""
      Rasterizer.foreachCellByPointSet(pointSet1, re)(f2) 
      assert (f2output === "point one: 81point two: 9point three: 0")
      
      var lineOutput = ""
      val f3 = (col:Int, row:Int, feature:LineString[String]) => {
        lineOutput = lineOutput + feature.data + raster.get(col,row) + "\n"
      }
      
      val line = LineString(0,0,9,9,"diagonal line")
      Rasterizer.foreachCellByLineString(line, re)(f3)
      
      // Some examples of immutable/fold interface
      val f1 = (z:Int, d:String, output:String) => output + d + z.toString
      
      val o = rasterize.Rasterizer.aggregrateCellsByPoint(p, raster, "")(f1)
      assert(o === "point one: 81")
      
      val o2 = rasterize.Rasterizer.aggregrateCellsByPoint(p2, raster, "")(f1)
      assert(o2 === "point two: 9")
      
      val o3 = rasterize.Rasterizer.aggregrateCellsByPoint(p3, raster, "")(f1)
      assert(o3 === "point three: 0")

      
      val o4 = rasterize.Rasterizer.aggregrateCellsByPointSet(pointSet1, raster, "")(f1)
      assert(o4 === "point one: 81point two: 9point three: 0")
      println(o4)
      
      
      
      s.shutdown()
    
  }
}
