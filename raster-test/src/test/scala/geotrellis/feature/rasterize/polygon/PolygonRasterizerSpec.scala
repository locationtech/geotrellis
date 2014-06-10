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

package geotrellis.feature.rasterize.polygon

import geotrellis._
import geotrellis.source._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.testkit._
import math.{max,min,round}
import org.scalatest.FunSuite
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.{geom => jts}


class RasterizePolygonSpec extends FunSuite
    with TestServer
    with RasterBuilders {

  test("Polygon Rasterization") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val g = RasterExtent(e, 1.0, 1.0, 10, 10)

    val data = (0 until 99).toArray
    val raster = Raster(data, g)
    val re = raster.rasterExtent

    val square  = Polygon( Line((1,9), (1,6), (4,6), (4,9), (1,9)) )
    val diamond = Polygon( Line((3,7), (6,4), (3,1), (0,4), (3,7)))
    val triangle = Polygon( Line((2,8),(5,5),(6,7), (6,7), (2,8)))

    val outsideSquare = Polygon( Line((51,59), (51,56), (54,56), (54,59), (51,59)) )
    val envelopingSquare = Extent(0.0, 0.0, 10.0, 10.0).toPolygon

    // intersection on cell midpoint
    val square2 = Polygon( Line( (1.0,9.0), (1.0,8.5), (1.0,6.0), (4.0, 6.0), (4.0, 8.5), (4.0, 9.0), (1.0, 9.0) ))

    // val edgeTable = EdgeTable(square, re)
    // // y is flipped in grid coordinates
    // assert( edgeTable.edges === Map( 1 -> List(TestLine(1,3,1,6,1,9,0),TestLine(1,3,4,6,4,9,0))))
    // assert( edgeTable.edges.size === 1)
    // assert( edgeTable.rowMin === 1)
    // assert( edgeTable.rowMax === 3)

    // val diamondTable = EdgeTable(diamond, re)
    // //assert( diamondTable.edges === Map(
    // //  3 -> List(Line(3,6,3,1,1,1,1,1),Line(3,6,0,1,1,1,1,-1)),
    // //  6 -> List(Line(6,9,3,1,1,1,1,-1),Line(6,9,0,1,1,1,1,1))
    // //  ))
    // assert(diamondTable.edges.size === 2)
    // assert( diamondTable.rowMin === 3)
    // assert( diamondTable.rowMax === 8)

    // val triangleTable = EdgeTable(triangle, re)
    // //  assert( triangleTable.edges === Map (
    // //    2 -> List(Line(2,5,2,1), Line(2,3,2,4)),
    // //    3 -> List(Line(3,5,5,-0.5))
    // //  ))
    // assert(triangleTable.edges.size === 2)
    // assert(triangleTable.rowMin === 2)
    // assert(triangleTable.rowMax === 4)


    val r1 = Rasterizer.rasterizeWithValue(square, re, 0x11)
    //      println(r1.asciiDraw)

    // values match gdal
    for ( i <- 1 to 3; j <- 1 to 3) {
      assert(r1.get(i,j) === 0x11)
    }
    
    var sum = 0
    r1.foreach(f => if (isData(f)) sum = sum + 1 )
    assert(sum === 9)
    
    val r2 = Rasterizer.rasterizeWithValue(diamond, re, 0x22)
    //      println(r2.asciiDraw())
    assert(r2.get(3,3) === 0x22)
    for (i <- 2 to 4) { assert(r2.get(i,4) === 0x22) }
    for (i <- 1 to 5) { assert(r2.get(i,5) === 0x22) }
    for (i <- 1 to 5) { assert(r2.get(i,6) === 0x22) }
    for (i <- 2 to 4) { assert(r2.get(i,7) === 0x22) }
    assert(r2.get(3,8) === 0x22)
    
    sum = 0
    r2.foreach(f => if (isData(f)) sum = sum + 1 )
    assert(sum === 18)

    val r3 = Rasterizer.rasterizeWithValue(triangle, re, 0x33)
    //      println(r3.asciiDraw())
    
    assert(r3.get(3,2) === 0x33)
    assert(r3.get(4,3) === 0x33)
    assert(r3.get(5,3) === 0x33)
    sum = 0
    r3.foreach(f => if (isData(f)) sum = sum + 1 )
    assert(sum === 3)

    val r4 = Rasterizer.rasterizeWithValue(square2, re, 0x44)
    //      println(r4.asciiDraw())

    val r5 = Rasterizer.rasterizeWithValue(outsideSquare, re, 0x55)
    //      println(r5.asciiDraw())

    val r6 = Rasterizer.rasterizeWithValue(envelopingSquare, re, 0x66)
    //      println(r6.asciiDraw())

    val emptyGeom = outsideSquare.intersection(envelopingSquare)
    // LoadWKT()
  }

  test("failing example should work") {
    val p = Polygon(Line((-9510600.807354769, 4176519.1962707597), (-9511212.30358105,4172238.854275199), (-9503568.600752532,4175602.1747499597), (-9510600.807354769,4176519.1962707597)))
    val re = RasterExtent(Extent(-9509377.814902207,4174073.2405969054,-9508766.318675926,4174684.736823185),2.3886571339098737,2.3886571339044167,256,256)
    val r = Rasterizer.rasterizeWithValue(p, re, 1 )
    var sum = 0
    r.foreach(v => if (isData(v)) sum = sum + 1)
    assert(sum === 65536)
  }

  test("polygon rasterization: more complex polygons") {
    val p1 = Polygon (Line((-74.6229572569999, 41.5930024740001),
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
      (-74.6229572569999, 41.5930024740001)))
    
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
    
    val r1 = VectorToRaster.rasterize(p1, rasterExtent, 0x55).get
    var sum = 0
    r1.foreach(f => if (isData(f)) sum = sum + 1 )
    assert(sum === 3)
    assert(r1.get(389,332) === 0x55)
    assert(r1.get(390,332) === 0x55)
    assert(r1.get(390,333) === 0x55)
    //      println("sum: " + sum)
  }

  test("Rasterization tests from directory of WKT files") {
    // This test loads WKT text files from core-test/data/feature which are
    // named xxx_nnn.wkt, where nnn is the number of cells we expect rasterization
    // to determine to be within the defined feature in the raster extent defined
    // below.
    
    // To create a new test, create a .wkt and a GeoJSON file of the polygon
    // in the directory, with the same name (except for extension).
    // For example, one quick way to do this: http://openlayers.org/dev/examples/vector-formats.html

    // Run the gdaltest.sh script with the name of the two files (without extension).
    // The final line is the number of cells included by GDAL (center of cell mode).
    // Rename the .wkt file to end with _N (where N is the number).

    def countRasterizedCells( wktFile:java.io.File, re:RasterExtent ) = {
      val json = scala.io.Source.fromFile(wktFile).mkString
      val filename = wktFile.getName()
      //      println("Testing rasterization: " + filename)
      val g1 = new WKTReader().read(json).asInstanceOf[jts.Polygon]

      if (g1.isValid){
        val count =
        Integer.parseInt(wktFile.getName()
          .subSequence(0, filename.length - 4)
          .toString
          .split("_")
          .last)

        val p1 = Polygon(g1)
        var sum = 0
        val re = RasterExtent( Extent(0, 0, 300, 300), 1, 1, 300, 300)
        val r = PolygonRasterizer.foreachCellByPolygon(p1, re)(
          new Callback {
            def apply(x:Int, y:Int) {
              sum = sum + 1
            }
          })

        (sum, count)
      }else{
        (0,0)
      }

    }

    val f = new java.io.File("core-test/data/feature/")
    val fs = f.listFiles.filter(_.getPath().endsWith(".wkt"))

    val re = RasterExtent( Extent(0, 0, 300, 300), 1, 1, 300, 300)
    fs.foreach( f => {
      val (sum, count) = countRasterizedCells(f, re)
      assert ( sum === count )
    })
  }

  test("Rasterization of a polygon with a hole in it") {
    val p = Polygon(
      Line( (0,0), (4, 0), (4, 4), (0, 4), (0, 0) ),
      Line( (1, 1), (3, 1), (3, 3), (1, 3), (1, 1) )
    )

    val re = RasterExtent(Extent(-1, -1, 5, 5), 6, 6)

    var sum = 0
    PolygonRasterizer.foreachCellByPolygon(p, re)(
      new Callback {
        def apply(col: Int, row: Int) {
          sum = sum + 1
        }
      })

    sum should be (12)
  }
}
