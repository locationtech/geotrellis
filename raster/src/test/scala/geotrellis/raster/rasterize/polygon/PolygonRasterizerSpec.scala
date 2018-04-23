/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.rasterize.polygon

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.rasterize._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.raster.testkit._
import geotrellis.vector._
import geotrellis.vector.io.wkt.WKT

import math.{max,min,round}

import org.locationtech.jts.{geom => jts}
import org.locationtech.jts.io.WKTReader
import org.scalatest.FunSuite
import scala.collection.mutable


class PolygonRasterizerSpec extends FunSuite
    with RasterMatchers
    with TileBuilders {

  test("Polygon Rasterization") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val re = RasterExtent(e, 1.0, 1.0, 10, 10)

    val square  = Polygon( Line((1,9), (1,6), (4,6), (4,9), (1,9)) )
    val diamond = Polygon( Line((3,7), (6,4), (3,1), (0,4), (3,7)))
    val triangle = Polygon( Line((2,8),(5,5),(6,7), (6,7), (2,8)))

    val outsideSquare = Polygon( Line((51,59), (51,56), (54,56), (54,59), (51,59)) )
    val envelopingSquare = Extent(0.0, 0.0, 10.0, 10.0).toPolygon

    // intersection on cell midpoint
    val square2 = Polygon( Line( (1.0,9.0), (1.0,8.5), (1.0,6.0), (4.0, 6.0), (4.0, 8.5), (4.0, 9.0), (1.0, 9.0) ))

    val r1 = Rasterizer.rasterizeWithValue(square, re, 0x11)
    // println(r1.tile.asciiDraw)

    // values match gdal
    for ( i <- 1 to 3; j <- 1 to 3) {
      assert(r1.get(i,j) === 0x11)
    }

    var sum = 0
    r1.foreach(f => if (isData(f)) sum = sum + 1 )
    assert(sum === 9)

    val r2 = Rasterizer.rasterizeWithValue(diamond, re, 0x22)
    // println(r2.tile.asciiDraw())
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
    // println(r3.tile.asciiDraw())

    assert(r3.get(3,2) === 0x33)
    assert(r3.get(4,3) === 0x33)
    assert(r3.get(5,3) === 0x33)
    sum = 0
    r3.foreach(f => if (isData(f)) sum = sum + 1 )
    assert(sum === 3)

    val r4 = Rasterizer.rasterizeWithValue(square2, re, 0x44)
    // println(r4.tile.asciiDraw())

    val r5 = Rasterizer.rasterizeWithValue(outsideSquare, re, 0x55)
    // println(r5.tile.asciiDraw())

    val r6 = Rasterizer.rasterizeWithValue(envelopingSquare, re, 0x66)
    // println(r6.tile.asciiDraw())

    val emptyGeom = outsideSquare.intersection(envelopingSquare)
    // LoadWKT()
  }

  test("previously-failing example should work") {
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

    val r1 = p1.rasterizeWithValue(rasterExtent, 0x55)
    var sum = 0
    r1.foreach(f => if (isData(f)) sum = sum + 1 )
    assert(sum === 3)
    assert(r1.get(389,332) === 0x55)
    assert(r1.get(390,332) === 0x55)
    assert(r1.get(390,333) === 0x55)
    // println("sum: " + sum)
  }

  test("Rasterization tests from directory of WKT files") {
    // This test loads WKT text files from raster/data/feature which are
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
        val r = PolygonRasterizer.foreachCellByPolygon(p1, re) { (x:Int, y:Int) =>
          sum = sum + 1
        }

        (sum, count)
      }else{
        (0,0)
      }

    }

    val f = new java.io.File("raster/data/vector/")
    val fs = f.listFiles.filter(_.getPath().endsWith(".wkt"))

    val re = RasterExtent( Extent(0, 0, 300, 300), 1, 1, 300, 300)
    fs.foreach( f => {
      val (sum, count) = countRasterizedCells(f, re)
      assert ( sum === count )
    })
  }

  test("Diamond polygon w/ point pixels, scanline through corner") {
    val extent = Extent(0, 0, 11, 11)
    val rasterExtent = RasterExtent(extent, 11, 11)
    val diamond = Polygon(List( Point(0,5.5), Point(5.5,11), Point(11,5.5), Point(0,5.5) ))

    val s = mutable.Set[(Int, Int)]()
    PolygonRasterizer.foreachCellByPolygon(diamond, rasterExtent) { (col, row) =>
      s += ((col, row))
    }

    s.size should be (36)
  }

  test("Flipped-diamond polygon w/ point pixels, scanline through corner") {
    val extent = Extent(0, 0, 11, 11)
    val rasterExtent = RasterExtent(extent, 11, 11)
    val diamond = Polygon(List( Point(5.5,0), Point(11,5.5), Point(5.5,11), Point(5.5,0) ))

    val s = mutable.Set[(Int, Int)]()
    PolygonRasterizer.foreachCellByPolygon(diamond, rasterExtent) { (col, row) =>
      s += ((col, row))
    }

    s.size should be (25)
  }

  test("Polygon w/ non-point pixels, w/o partial cells, not containing border center cells") {
    val extent = Extent(0, 0, 10, 10)
    val rasterExtent = RasterExtent(extent, 10, 10)
    val extent2 = Extent(0.7, 0.7, 9.3, 9.3)

    val s = mutable.Set[(Int, Int)]()
    PolygonRasterizer.foreachCellByPolygon(extent2, rasterExtent) { (col, row) =>
      s += ((col, row))
    }

    assert(s.size < 100)
  }

  test("Polygon w/ non-point pixels and partial cells, not containing border center cells") {
    val extent = Extent(0, 0, 10, 10)
    val rasterExtent = RasterExtent(extent, 10, 10)
    val extent2 = Extent(0.7, 0.7, 9.3, 9.3)
    val options = Options(includePartial = true, sampleType = PixelIsArea)

    val s = mutable.Set[(Int, Int)]()
    PolygonRasterizer.foreachCellByPolygon(extent2, rasterExtent, options) { (col, row) =>
      s += ((col, row))
    }

    s.size should be (100)
  }

  test("Polygon w/ non-point pixels and w/ partial cells") {
    val p = Polygon(Line((0.0,0.0), (0.0,1.0), (0.5,1.5), (0.0,2.0), (0.0,3.0), (3.0,3.0), (3.0,0.0), (0.0,0.0)))
    val rasterExtent = RasterExtent(Extent(0, 0, 3, 3), 3, 3)
    val options = Options(includePartial = true, sampleType = PixelIsArea)
    val s = mutable.Set.empty[(Int, Int)]

    PolygonRasterizer.foreachCellByPolygon(p, rasterExtent, options) { (col, row) =>
      s += ((col, row))
    }

    s.size should be (9)
  }

  test("Polygon w/ non-point pixels and w/o partial cells") {
    val p = Polygon(Line((0.0,0.0), (0.0,1.0), (0.5,1.5), (0.0,2.0), (0.0,3.0), (3.0,3.0), (3.0,0.0), (0.0,0.0)))
    val rasterExtent = RasterExtent(Extent(0, 0, 3, 3), 3, 3)
    val options = Options(includePartial = false, sampleType = PixelIsArea)
    val s = mutable.Set[(Int, Int)]()

    PolygonRasterizer.foreachCellByPolygon(p, rasterExtent, options) { (col, row) =>
      s += ((col, row))
    }

    s.size should be (8)
  }

  test("Smaller polygon w/ non-point pixels and w/o partial cells") {
    val p = Polygon(Line((0.01,0.01), (0.01,1.0), (0.5,1.5), (0.01,2.0), (0.01,2.99), (2.99,2.99), (2.99,0.01), (0.01,0.01)))
    val rasterExtent = RasterExtent(Extent(0, 0, 3, 3), 3, 3)
    val options = Options(includePartial = false, sampleType = PixelIsArea)
    val s = mutable.Set[(Int, Int)]()

    PolygonRasterizer.foreachCellByPolygon(p, rasterExtent, options) { (col, row) =>
      s += ((col, row))
    }

    s.size should be (1)
  }

  test("Rasterization of a polygon with a hole in it") {
    val p = Polygon(
      Line( (0,0), (4, 0), (4, 4), (0, 4), (0, 0) ),
      Line( (1, 1), (3, 1), (3, 3), (1, 3), (1, 1) )
    )

    val re = RasterExtent(Extent(-1, -1, 5, 5), 6, 6)

    val tile = IntArrayTile.empty(6, 6)

    var sum = 0
    PolygonRasterizer.foreachCellByPolygon(p, re) { (col: Int, row: Int) =>
      tile.set(col, row, 1)
      sum = sum + 1
    }
    // println(tile.tile.asciiDraw)

    sum should be (12)
  }

  val triangle2 = Polygon(Point(0, 0), Point(0, 100), Point(100, 50), Point(0, 0))

  test("Triangle w/ point pixels") {
    /*
     1    ND    ND    ND    ND    ND    ND    ND    ND    ND
     1     1     1    ND    ND    ND    ND    ND    ND    ND
     1     1     1     1     1    ND    ND    ND    ND    ND
     1     1     1     1     1     1     1    ND    ND    ND
     1     1     1     1     1     1     1     1     1    ND
     1     1     1     1     1     1     1     1     1    ND
     1     1     1     1     1     1     1    ND    ND    ND
     1     1     1     1     1    ND    ND    ND    ND    ND
     1     1     1    ND    ND    ND    ND    ND    ND    ND
     1    ND    ND    ND    ND    ND    ND    ND    ND    ND
     */
    val e = Extent(0, 0, 100, 100)
    val re = RasterExtent(e, 10, 10)
    val ro = Options(includePartial = true, sampleType = PixelIsPoint)
    val tile = IntArrayTile.empty(10, 10)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(triangle2, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (50)
//    println(GeoTiff(tile, e, LatLng).tile.asciiDraw)
  }

  test("Triangle w/ non-point pixels and w/o partial cells") {
    /*
     ND    ND    ND    ND    ND    ND    ND    ND    ND    ND
      1     1    ND    ND    ND    ND    ND    ND    ND    ND
      1     1     1     1    ND    ND    ND    ND    ND    ND
      1     1     1     1     1     1    ND    ND    ND    ND
      1     1     1     1     1     1     1     1    ND    ND
      1     1     1     1     1     1     1     1    ND    ND
      1     1     1     1     1     1    ND    ND    ND    ND
      1     1     1     1    ND    ND    ND    ND    ND    ND
      1     1    ND    ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND    ND    ND    ND
     */
    val e = Extent(0, 0, 100, 100)
    val re = RasterExtent(e, 10, 10)
    val ro = Options(includePartial = false, sampleType = PixelIsArea)
    val tile = IntArrayTile.empty(10, 10)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(triangle2, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (40)
    // println(GeoTiff(tile, e, LatLng).tile.asciiDraw)
  }

  test("Triangle w/ non-point pixels and w/ partial cells") {
    /*
     1     1    ND    ND    ND    ND    ND    ND    ND    ND
     1     1     1     1    ND    ND    ND    ND    ND    ND
     1     1     1     1     1     1    ND    ND    ND    ND
     1     1     1     1     1     1     1     1    ND    ND
     1     1     1     1     1     1     1     1     1     1
     1     1     1     1     1     1     1     1     1     1
     1     1     1     1     1     1     1     1    ND    ND
     1     1     1     1     1     1    ND    ND    ND    ND
     1     1     1     1    ND    ND    ND    ND    ND    ND
     1     1    ND    ND    ND    ND    ND    ND    ND    ND
     */
    val e = Extent(0, 0, 100, 100)
    val re = RasterExtent(e, 10, 10)
    val ro = Options(includePartial = true, sampleType = PixelIsArea)
    val tile = IntArrayTile.empty(10, 10)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(triangle2, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (60)
    // println(GeoTiff(tile, e, LatLng).tile.asciiDraw)
  }

  val tiny = Polygon(Point(40, 40), Point(40, 59), Point(59, 50), Point(40, 40))

  test("Sub-Pixel Geometry w/ point pixels") {
    /*
     ND    ND    ND
     ND     1    ND
     ND    ND    ND
     */
    val e = Extent(0, 0, 100, 100)
    val re = RasterExtent(e, 3, 3)
    val ro = Options(includePartial = true, sampleType = PixelIsPoint)
    val tile = IntArrayTile.empty(3, 3)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(tiny, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (1)
    // println(GeoTiff(tile, e, LatLng).tile.asciiDraw)
  }

  test("Sub-Pixel Geometry w/ non-point pixels and w/ partial cells") {
    /*
     ND    ND    ND
     ND     1    ND
     ND    ND    ND
     */
    val e = Extent(0, 0, 100, 100)
    val re = RasterExtent(e, 3, 3)
    val ro = Options(includePartial = true, sampleType = PixelIsArea)
    val tile = IntArrayTile.empty(3, 3)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(tiny, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (1)
    // println(GeoTiff(tile, e, LatLng).tile.asciiDraw)
  }

  test("Sub-Pixel Geometry w/ non-point pixels and w/o partial cells") {
    /*
     ND    ND    ND
     ND    ND    ND
     ND    ND    ND
     */
    val e = Extent(0, 0, 100, 100)
    val re = RasterExtent(e, 3, 3)
    val ro = Options(includePartial = false, sampleType = PixelIsArea)
    val tile = IntArrayTile.empty(3, 3)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(tiny, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (0)
    // println(GeoTiff(tile, e, LatLng).tile.asciiDraw)
  }

  val tiny2 = Polygon(Point(40, 40), Point(40, 42), Point(42, 41), Point(40, 40))

  test("More Sub-Pixel Geometry w/ point pixels") {
    /*
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     */
    val e = Extent(0, 0, 100, 100)
    val re = RasterExtent(e, 7, 7)
    val ro = Options(includePartial = true, sampleType = PixelIsPoint)
    val tile = IntArrayTile.empty(7, 7)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(tiny2, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (0)
    // println(GeoTiff(tile, e, LatLng).tile.asciiDraw)
  }

  test("More Sub-Pixel Geometry w/ non-point pixels and w/ partial cells") {
    /*
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND     1    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     */
    val e = Extent(0, 0, 100, 100)
    val re = RasterExtent(e, 7, 7)
    val ro = Options(includePartial = true, sampleType = PixelIsArea)
    val tile = IntArrayTile.empty(7, 7)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(tiny2, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (1)
    // println(GeoTiff(tile, e, LatLng).tile.asciiDraw)
  }

  test("More Sub-Pixel Geometry w/ non-point pixels and w/o partial cells") {
    /*
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     ND    ND    ND    ND    ND    ND    ND
     */
    val e = Extent(0, 0, 100, 100)
    val re = RasterExtent(e, 7, 7)
    val ro = Options(includePartial = false, sampleType = PixelIsArea)
    val tile = IntArrayTile.empty(7, 7)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(tiny2, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (0)
    // println(GeoTiff(tile, e, LatLng).tile.asciiDraw)
  }

  test("Small triangle w/ non-point pixels (1/4)") {
    /*
      ND ND  1
      ND  1  1
      ND ND  1
     */
    val tiny3 = Polygon(Point(50, 48), Point(99, 32), Point(99, 68), Point(50,48))
    val e = Extent(0,0,100,100)
    val re = RasterExtent(e, 3, 3)
    val ro = Options(includePartial = true, sampleType=PixelIsArea)
    val tile = IntArrayTile.empty(3,3)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(tiny3, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (4)
  }

  test("Small triangle w/ non-point pixels (2/4)") {
    /*
      1  ND  ND
      1   1  ND
      1  ND  ND
     */
    val tiny3 = Polygon(Point(50, 48), Point(1, 32), Point(1, 68), Point(50, 48))
    val e = Extent(0,0,100,100)
    val re = RasterExtent(e, 3, 3)
    val ro = Options(includePartial = true, sampleType=PixelIsArea)
    val tile = IntArrayTile.empty(3,3)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(tiny3, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (4)
  }

  test("Small triangle w/ non-point pixels (3/4)") {
    /*
       1  1  1
      ND  1 ND
      ND ND ND
     */
    val tiny3 = Polygon(Point(48, 50), Point(32, 99), Point(68, 99), Point(48, 50))
    val e = Extent(0,0,100,100)
    val re = RasterExtent(e, 3, 3)
    val ro = Options(includePartial = true, sampleType=PixelIsArea)
    val tile = IntArrayTile.empty(3,3)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(tiny3, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (4)
  }

  test("Small triangle w/ non-point pixels (4/4)") {
    /*
      ND  ND  ND
      ND  1   ND
      1   1   1
     */
    val tiny3 = Polygon(Point(48, 50), Point(32, 1), Point(68, 1), Point(48, 50))
    val e = Extent(0,0,100,100)
    val re = RasterExtent(e, 3, 3)
    val ro = Options(includePartial = true, sampleType=PixelIsArea)
    val tile = IntArrayTile.empty(3,3)
    var count = 0

    PolygonRasterizer.foreachCellByPolygon(tiny3, re, ro)({ (col, row) =>
      tile.set(col, row, 1)
      count += 1
    })

    count should be (4)
  }

  test("Should correctly handle horizontal segments (1/2)") {
    val polygonStr = "POLYGON ((0.7754 -0.6775, 0.774 -0.6779, 0.7773 -0.6783, 0.7768 -0.678, 0.7761 -0.678, 0.7754 -0.6775))"
    val polygon = WKT.read(polygonStr)
    val extent = Extent(0.0, -0.904, 0.9188, 0.0)
    val rasterExtent = RasterExtent(extent, 10, 10)

    Rasterizer.rasterizeWithValue(polygon, rasterExtent, 1)
  }

  test("Should correctly handle horizontal segments (2/2)") {
    val polygonStr = "POLYGON ((0.7754 -0.6775, 0.7758 -0.6768, 0.776 -0.6766, 0.7763 -0.6762, 0.7765 -0.6761, 0.7772 -0.6762, 0.7777 -0.6763, 0.7782 -0.6765, 0.779 -0.677, 0.7791 -0.6773, 0.7792 -0.6779, 0.779 -0.6785, 0.7787 -0.6787, 0.7781 -0.6786, 0.7773 -0.6783, 0.7768 -0.678, 0.7761 -0.678, 0.7755 -0.6779, 0.7754 -0.6775))"
    val polygon = WKT.read(polygonStr)
    val extent = Extent(0.0, -0.904, 0.9188, 0.0)
    val rasterExtent = RasterExtent(extent, 10, 10)

    Rasterizer.rasterizeWithValue(polygon, rasterExtent, 1)
  }

  test("Should correctly handle difficult polygon (1/3)") {
    val polygonStr = "POLYGON ((0.007754 -0.006775, 0.007758 -0.006768, 0.00776 -0.006766, 0.007763 -0.006762, 0.007765 -0.006761, 0.007772 -0.006762, 0.007777 -0.006763, 0.007782 -0.006765, 0.00779 -0.00677, 0.007791 -0.006773, 0.007792 -0.006779, 0.00779 -0.006785, 0.007787 -0.006787, 0.007781 -0.006786, 0.007773 -0.006783, 0.007768 -0.00678, 0.007761 -0.00678, 0.007755 -0.006779, 0.007754 -0.006775))"
    val polygon = WKT.read(polygonStr)
    val extent = Extent(0.0, -0.00904, 0.009188, 0.0)
    val rasterExtent = RasterExtent(extent, 10, 10)

    Rasterizer.rasterizeWithValue(polygon, rasterExtent, 1)
  }

  test("Should correctly handle difficult polygon (2/3)") {
    val polygonStr = "POLYGON ((0.00498 -0.004533, 0.004975 -0.004582, 0.004975 -0.004583, 0.004974 -0.004594, 0.004968 -0.004605, 0.004959 -0.00461, 0.004955 -0.004611, 0.00495 -0.004611, 0.004913 -0.004595, 0.004903 -0.004587, 0.004902 -0.004584, 0.004886 -0.004542, 0.004884 -0.004535, 0.004885 -0.004507, 0.004889 -0.004495, 0.004897 -0.004489, 0.004904 -0.004486, 0.004916 -0.004484, 0.004917 -0.004485, 0.004927 -0.004487, 0.004938 -0.004494, 0.004939 -0.004497, 0.004943 -0.004502, 0.004951 -0.00451, 0.004966 -0.004518, 0.004967 -0.004519, 0.004971 -0.004521, 0.004972 -0.004521, 0.004976 -0.004524, 0.00498 -0.004532, 0.00498 -0.004533))"
    val polygon = WKT.read(polygonStr)
    val extent = Extent(0.0, -0.009042, 0.009171, 0.0)
    val rasterExtent = RasterExtent(extent, 10, 10)

    Rasterizer.rasterizeWithValue(polygon, rasterExtent, 1)
  }

  test("Should correctly handle difficult polygon (3/3)") {
    val polygonStr = "POLYGON ((0.003045 -0.004534, 0.003045 -0.004538, 0.003044 -0.004539, 0.003042 -0.004542, 0.003042 -0.004544, 0.003041 -0.004544, 0.003041 -0.004545, 0.00304 -0.004547, 0.003039 -0.004547, 0.003038 -0.004548, 0.003037 -0.004549, 0.003037 -0.00455, 0.003036 -0.00455, 0.003035 -0.004551, 0.003035 -0.004552, 0.003034 -0.004553, 0.003033 -0.004554, 0.003032 -0.004555, 0.003032 -0.004556, 0.00303 -0.004556, 0.003029 -0.004557, 0.003027 -0.004558, 0.003026 -0.004558, 0.003026 -0.004559, 0.003025 -0.004559, 0.003024 -0.004559, 0.003023 -0.00456, 0.003022 -0.00456, 0.00302 -0.004561, 0.00302 -0.004562, 0.003017 -0.004562, 0.003015 -0.004563, 0.003009 -0.004563, 0.003008 -0.00456, 0.003007 -0.004559, 0.003007 -0.004558, 0.003007 -0.004557, 0.003006 -0.004555, 0.003006 -0.004553, 0.003005 -0.004551, 0.003005 -0.004536, 0.003006 -0.004535, 0.003007 -0.004534, 0.003007 -0.004532, 0.003008 -0.004532, 0.003008 -0.004531, 0.003009 -0.00453, 0.003009 -0.004528, 0.00301 -0.004528, 0.00301 -0.004527, 0.003011 -0.004526, 0.003012 -0.004525, 0.003013 -0.004524, 0.003014 -0.004523, 0.003015 -0.004523, 0.003015 -0.004522, 0.003016 -0.004522, 0.003018 -0.004521, 0.003019 -0.004521, 0.00302 -0.00452, 0.00302 -0.004519, 0.003021 -0.004518, 0.003022 -0.004518, 0.003023 -0.004517, 0.003024 -0.004517, 0.003026 -0.004515, 0.003027 -0.004515, 0.003029 -0.004514, 0.00303 -0.004514, 0.003031 -0.004513, 0.003032 -0.004512, 0.003035 -0.004512, 0.003036 -0.004513, 0.003037 -0.004514, 0.003038 -0.004516, 0.003038 -0.004519, 0.003039 -0.00452, 0.00304 -0.004521, 0.00304 -0.004522, 0.003041 -0.004524, 0.003041 -0.004525, 0.003042 -0.004526, 0.003043 -0.004527, 0.003045 -0.004534))"
    val polygon = WKT.read(polygonStr)
    val extent = Extent(0.0, -0.009042, 0.009156, 0.0)
    val rasterExtent = RasterExtent(extent, 10, 10)

    Rasterizer.rasterizeWithValue(polygon, rasterExtent, 1)
  }
}
