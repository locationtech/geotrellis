/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.feature

import geotrellis._
import geotrellis.feature.op.geometry.{Buffer,GetCentroid}
import geotrellis.feature._
import geotrellis.testkit._
import math.{max,min,round}
import org.scalatest.FunSuite
import org.scalatest.matchers._
import geotrellis.feature.op.geometry.GetEnvelope
import geotrellis.feature.op.geometry.Intersect
import geotrellis.feature.rasterize.Rasterizer

import geotrellis.testkit._
import scala.collection.mutable

class RasterizeSpec extends FunSuite with TestServer 
                                     with ShouldMatchers {
   test("Point Rasterization") {
      val e = Extent(0.0, 0.0, 10.0, 10.0)
      val g = RasterExtent(e, 1.0, 1.0, 10, 10)

      val data = (0 until 99).toArray
      val raster = Raster(data, g)
      val re = raster.rasterExtent
      
      val p = Point(1.0,2.0,"point one: ")
      val p2 = Point(9.5, 9.5, "point two: ")
      val p3 = Point(0.1, 9.9, "point three: ")
      

      var f2output:String = ""
      val f2 = new geotrellis.feature.rasterize.Callback[Point,String] {
          def apply(col:Int, row:Int, feature:Point[String]) {
            val z = raster.get(col,row)
            f2output = f2output + feature.data + z.toString
          }
        }

      Rasterizer.foreachCellByPoint(p, re)(f2)
      assert(f2output === "point one: 81")

      f2output = ""
      Rasterizer.foreachCellByPoint(p2, re)(f2)
      assert( f2output === "point two: 9")
     
      f2output = ""
      Rasterizer.foreachCellByPoint(p3, re)(f2) 
      assert( f2output === "point three: 0")

      var lineOutput = ""
      val f3 = new geotrellis.feature.rasterize.Callback[LineString,String] {
          def apply(col:Int, row:Int, feature:LineString[String]) {
            lineOutput = lineOutput + feature.data + raster.get(col,row) + "\n"
          }
        }
      
      val line = LineString(0,0,9,9,"diagonal line")
      Rasterizer.foreachCellByLineString(line, re)(f3)
  }

  test("linestring rasterization") {
    // setup test objects
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val g = RasterExtent(e, 1.0, 1.0, 10, 10)

    val data = (0 until 99).toArray
    val raster = Raster(data, g)
    val re = raster.rasterExtent

    val line1 = LineString( 1.0,3.5,1.0,8.5, "line" ) 
    var lineOutput:String = ""
    val l1 = new geotrellis.feature.rasterize.Callback[LineString,String] {
      def apply(col:Int, row:Int, feature:LineString[String]) {
        lineOutput = lineOutput + raster.get(col,row) + ","
      }
    }
    Rasterizer.foreachCellByLineString(line1, re)(l1)
    assert(lineOutput === "61,51,41,31,21,11,")
  }

  test("linestring rasterization with multiple points") {
    // setup test objects
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val g = RasterExtent(e, 1.0, 1.0, 10, 10)

    val data = (0 until 99).toArray
    val raster = Raster(data, g)
    val re = raster.rasterExtent

    val line1 = LineString(Seq((1.0,3.5),(1.0,8.5), (5.0, 9.0)), "line" ) 
    val result = mutable.ListBuffer[(Int,Int)]()
    
    val l1 = new geotrellis.feature.rasterize.Callback[LineString,String] {
      def apply(col:Int, row:Int, feature:LineString[String]) {
        result += ((col,row))
      }
    }
    Rasterizer.foreachCellByLineString(line1, re)(l1)
    result.toSeq should be (Seq( (1,6),
                                 (1,5),
                                 (1,4),
                                 (1,3),
                                 (1,2),
                                 (1,1),(2,1),(3,1),(4,1),(5,1)))
  }

  test("linestring rasterization with multiple points in diagonal") {
    // setup test objects
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val g = RasterExtent(e, 1.0, 1.0, 10, 10)

    val data = (0 until 99).toArray
    val raster = Raster(data, g)
    val re = raster.rasterExtent

    val line1 = LineString(Seq((1.0,3.5),(1.0,8.5), (5.0, 9.0),(1.0,4.5)), "line" ) 
    val result = mutable.ListBuffer[(Int,Int)]()
    
    val l1 = new geotrellis.feature.rasterize.Callback[LineString,String] {
      def apply(col:Int, row:Int, feature:LineString[String]) {
        result += ((col,row))
      }
    }
    Rasterizer.foreachCellByLineString(line1, re)(l1)
    result.toSeq should be (Seq( (1,6),
                                 (1,5),
                                 (1,4),
                                 (1,3),
                                 (1,2),
                                 (1,1),(2,1),(3,1),(4,1),(5,1),
                                                   (4,2),
                                             (3,3),
                                       (2,4),
                                  (1,5)))
  }
}
