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

package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.mapalgebra.local._
import geotrellis.util.{ByteReader, Filesystem}
import geotrellis.vector.Extent

import geotrellis.proj4._

import geotrellis.raster.testkit._
import java.nio.ByteBuffer
import org.scalatest._

class GeoTiffMultibandTileSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with RasterMatchers
    with GeoTiffTestUtils
    with TileBuilders {

  override def afterAll = purge

  describe ("GeoTiffMultibandTile creation") {

    it("should create GeoTiffMultibandTile from ArrayMultibandTile") {
      val original =
        ArrayMultibandTile(
          ArrayTile(Array.ofDim[Int](15*10).fill(1), 15, 10),
          ArrayTile(Array.ofDim[Int](15*10).fill(2), 15, 10),
          ArrayTile(Array.ofDim[Int](15*10).fill(3), 15, 10)
        )

      val gtm = GeoTiffMultibandTile(original)

      assertEqual(gtm.band(0), original.band(0))
      assertEqual(gtm.band(1), original.band(1))
      assertEqual(gtm.band(2), original.band(2))
    }

    it("should create GeoTiffMultibandTile from large Float32 ArrayMultibandTile for Striped") {
      val original =
        ArrayMultibandTile(
          ArrayTile(Array.ofDim[Float](150*140).fill(1.0f), 150, 140),
          ArrayTile(Array.ofDim[Float](150*140).fill(2.0f), 150, 140),
          ArrayTile(Array.ofDim[Float](150*140).fill(3.0f), 150, 140)
        )

      val gtm = GeoTiffMultibandTile(original)

      assertEqual(gtm.band(0), original.band(0))
      assertEqual(gtm.band(1), original.band(1))
      assertEqual(gtm.band(2), original.band(2))
    }

    it("should create GeoTiffMultibandTile from large Float32 ArrayMultibandTile for Tiled") {
      val original =
        ArrayMultibandTile(
          ArrayTile(Array.ofDim[Float](150*140).fill(1.0f), 150, 140),
          ArrayTile(Array.ofDim[Float](150*140).fill(2.0f), 150, 140),
          ArrayTile(Array.ofDim[Float](150*140).fill(3.0f), 150, 140)
        )

      val gtm = GeoTiffMultibandTile(original, GeoTiffOptions(Tiled(16, 16)))

      assertEqual(gtm.band(0), original.band(0))
      assertEqual(gtm.band(1), original.band(1))
      assertEqual(gtm.band(2), original.band(2))
    }

    it("should create GeoTiffMultibandTile from large Short ArrayMultibandTile for Tiled") {
      val original =
        ArrayMultibandTile(
          RawArrayTile(Array.ofDim[Short](150*140).fill(1.toShort), 150, 140),
          RawArrayTile(Array.ofDim[Short](150*140).fill(2.toShort), 150, 140),
          RawArrayTile(Array.ofDim[Short](150*140).fill(3.toShort), 150, 140)
        )

      val gtm = GeoTiffMultibandTile(original, GeoTiffOptions(Tiled(32, 32)))

      assertEqual(gtm.band(0), original.band(0))
      assertEqual(gtm.band(1), original.band(1))
      assertEqual(gtm.band(2), original.band(2))
    }

    it("should create GeoTiffMultibandTile from Double ArrayMultibandTile for Tiled, write and read and match") {
      val path = "/tmp/geotiff-writer.tif"

      val band1 = ArrayTile( (0 until (3000)).map(_.toDouble).toArray, 50, 60)
      val band2 = ArrayTile( (3000 until (6000)).map(_.toDouble).toArray, 50, 60)
      val band3 = ArrayTile( (6000 until (9000)).map(_.toDouble).toArray, 50, 60)
      val original =
        ArrayMultibandTile(
          band1,
          band2,
          band3
        )

      val options = GeoTiffOptions(Tiled(16, 16))
      val gtm = GeoTiffMultibandTile(original, options)
      val geoTiff = MultibandGeoTiff(gtm, Extent(100.0, 40.0, 120.0, 42.0), LatLng, options = options)
      geoTiff.write(path)

      addToPurge(path)

      val actual = MultibandGeoTiff(path).tile

      assertEqual(actual.band(0), band1)
      assertEqual(actual.band(1), band2)
      assertEqual(actual.band(2), band3)
    }
  }

  describe("Multiband cellType conversion") {
    val tiff = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-pixel.tif"))

    it("should convert the cellType with convert") {
      val actual =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.convert(UShortCellType)

      val expected =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile.convert(UShortCellType)

      assertEqual(expected, actual)
    }

    it("should convert to the ShortCellType correctly") {
      val expected = MultibandGeoTiff(geoTiffPath("3bands/int16/3bands-tiled-pixel.tif"))
      val actual = tiff.tile.convert(ShortCellType)

      assertEqual(expected.tile, actual.tile)
    }

    it("should convert to the UShortCellType correctly") {
      val expected = MultibandGeoTiff(geoTiffPath("3bands/uint16/3bands-tiled-pixel.tif"))
      val actual = tiff.tile.convert(UShortCellType)

      assertEqual(expected.tile, actual.tile)
    }

    it("should convert the cellType with interpretAs") {
      val actual =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.interpretAs(UShortCellType)

      val expected =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.interpretAs(UShortCellType)

      assertEqual(expected, actual)
    }
  }

  describe("Multiband subset combine methods") {
    it("should work the same on integer-valued GeoTiff tiles as Array tiles") {
      val actual = {
        val tiles = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile
        tiles.combine(List(0,2))({ seq: Seq[Int] => seq.sum })
      }
      val expected = {
        val tiles = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile
        tiles.combine(List(0,2))({ seq: Seq[Int] => seq.sum })
      }

      assertEqual(actual, expected)
    }

    it("should work correctly on integer-valued tiles") {
      val tiles = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile
      val band0 = tiles.band(0)
      val band2 = tiles.band(2)
      val actual = tiles.combine(List(0,2))({ seq: Seq[Int] => seq.sum })
      val expected = band0 + band2

      assertEqual(actual, expected)
    }

    it("should work correctly on double-valued tiles") {
      val original =
        ArrayMultibandTile(
          ArrayTile(Array.ofDim[Float](150*140).fill(1.5f), 150, 140),
          ArrayTile(Array.ofDim[Float](150*140).fill(2.5f), 150, 140),
          ArrayTile(Array.ofDim[Float](150*140).fill(3.5f), 150, 140))
      val tiles = GeoTiffMultibandTile(original)
      val band0 = tiles.band(0).toArrayDouble
      val band2 = tiles.band(2).toArrayDouble
      val actual = tiles.combineDouble(List(0,2))({ seq: Seq[Double] => seq.sum }).toArray
      val expected = band0.zip(band2).map({ pair => pair._1 + pair._2 })

      (actual.zip(expected)).foreach({ pair =>
        assert(pair._1 == pair._2, "actual should equal expected")
      })
    }
  }

  describe("Multiband subset map methods") {

    it("should work correctly on integer-valued tiles") {
      val tiles = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile
      val actual = tiles.map(List(0,2))({ (band,z) => z + band + 3 })
      val expectedBand0 = tiles.band(0).map({ z => z + 0 + 3 }).toArray
      val expectedBand1 = tiles.band(1).toArray
      val expectedBand2 = tiles.band(2).map({ z => z + 2 + 3 }).toArray

      (actual.band(0).toArray.zip(expectedBand0)).foreach({ pair =>
        assert(pair._1 == pair._2, "actual should equal expected in band 0")
      })

      (actual.band(1).toArray.zip(expectedBand1)).foreach({ pair =>
        assert(pair._1 == pair._2, "actual should equal expected in band 1")
      })

      (actual.band(2).toArray.zip(expectedBand2)).foreach({ pair =>
        assert(pair._1 == pair._2, "actual should equal expected in band 2")
      })
    }

    it("should work correctly on double-valued tiles") {
      val original =
        ArrayMultibandTile(
          ArrayTile(Array.ofDim[Float](150*140).fill(1.5f), 150, 140),
          ArrayTile(Array.ofDim[Float](150*140).fill(2.5f), 150, 140),
          ArrayTile(Array.ofDim[Float](150*140).fill(3.5f), 150, 140))
      val tiles = GeoTiffMultibandTile(original)
      val actual = tiles.mapDouble(List(0,2))({ (band,z) => z + band + 3.5 })
      val expectedBand0 = tiles.band(0).mapDouble({ z => z + 0 + 3.5 }).toArrayDouble
      val expectedBand1 = tiles.band(1).toArrayDouble
      val expectedBand2 = tiles.band(2).mapDouble({ z => z + 2 + 3.5 }).toArrayDouble

      (actual.band(0).toArrayDouble.zip(expectedBand0)).foreach({ pair =>
        assert(pair._1 == pair._2, "actual should equal expected in band 0")
      })

      (actual.band(1).toArrayDouble.zip(expectedBand1)).foreach({ pair =>
        assert(pair._1 == pair._2, "actual should equal expected in band 1")
      })

      (actual.band(2).toArrayDouble.zip(expectedBand2)).foreach({ pair =>
        assert(pair._1 == pair._2, "actual should equal expected in band 2")
      })
    }
  }

  describe("Multiband bands (reorder) method") {

    it("should be inexpensive") {
      val tile0 = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile
      val tile1 = tile0.subsetBands(List(1, 2, 0))

      tile0.band(0) should be theSameInstanceAs tile1.band(2)
      tile0.band(1) should be theSameInstanceAs tile1.band(0)
      tile0.band(2) should be theSameInstanceAs tile1.band(1)
    }

    it("result should have correct bandCount") {
      val tile0 = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile
      val tile1 = tile0.subsetBands(List(1, 2, 0))
      val tile2 = tile0.subsetBands(List(1, 2))

      tile1.bandCount should be (3)
      tile2.bandCount should be (2)
    }

    it("result should work properly with foreach") {
      val tile0 = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile
      val tile1 = tile0.subsetBands(List(1, 2, 0))
      val tile2 = tile1.subsetBands(List(1, 2, 0))

      tile0.band(0).foreach { z => z should be (1) }
      tile0.band(1).foreach { z => z should be (2) }
      tile0.band(2).foreach { z => z should be (3) }
      tile1.band(2).foreach { z => z should be (1) }
      tile1.band(0).foreach { z => z should be (2) }
      tile1.band(1).foreach { z => z should be (3) }
      tile2.band(0).foreach { z => z should be (3) }
      tile2.band(1).foreach { z => z should be (1) }
      tile2.band(2).foreach { z => z should be (2) }
    }

    it("should disallow \"invalid\" bandSequences") {
      val tile0 = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile
      an [IllegalArgumentException] should be thrownBy {
        tile0.subsetBands(0,1,2,3) // There are only 3 bands
      }
    }
  }

  describe("GeoTiffMultibandTile map") {

    it("should map a single band, striped, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile.map(1)(_ + 3)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (5) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("should map a single band, tiled, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-pixel.tif")).tile.toArrayTile.map(1)(_ + 3)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (5) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("should map a single band, striped, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-band.tif")).tile.toArrayTile.map(1)(_ + 3)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (5) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("should map a single band, tiled, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-band.tif")).tile.toArrayTile.map(1)(_ + 3)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (5) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("should map over all bands, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile.map { (b, z) => b * 10 + z }

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (12) }
      tile.band(2).foreach { z => z should be (23) }
    }

    it("should map over all bands, tiled") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-pixel.tif")).tile.toArrayTile.map { (b, z) => ((b+1) * 10) + z }

      tile.band(0).foreach { z => z should be (11) }
      tile.band(1).foreach { z => z should be (22) }
      tile.band(2).foreach { z => z should be (33) }
    }

    it("should mapDouble a single band, striped, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile.convert(DoubleConstantNoDataCellType).mapDouble(1)(_ + 3.3)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (5) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("should mapDouble a single band, tiled, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-band.tif")).tile.toArrayTile.convert(DoubleConstantNoDataCellType).mapDouble(1)(_ + 3.3)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (5) }
      tile.band(2).foreach { z => z should be (3) }
    }

  }

  describe("GeoTiffMultibandTile foreach") {

    it("should foreach a single band, striped, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile

      val cellCount = tile.band(1).toArray.size

      var count = 0
      tile.foreach(1) { z =>
        z should be (2)
        count += 1
      }
      count should be (cellCount)
    }

    it("should foreach a single band, tiled, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-pixel.tif")).tile.toArrayTile

      val cellCount = tile.band(1).toArray.size

      var count = 0
      tile.foreach(1) { z =>
        z should be (2)
        count += 1
      }
      count should be (cellCount)
    }

    it("should foreach a single band, striped, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-band.tif")).tile.toArrayTile

      val cellCount = tile.band(1).toArray.size

      var count = 0
      tile.foreach(1) { z =>
        z should be (2)
        count += 1
      }
      count should be (cellCount)
    }

    it("should foreach a single band, tiled, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-band.tif")).tile.toArrayTile

      val cellCount = tile.band(1).toArray.size

      var count = 0
      tile.foreach(1) { z =>
        z should be (2)
        count += 1
      }
      count should be (cellCount)
    }

    it("should foreachDouble all bands, striped, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile

      val cellCount = tile.band(1).toArray.size

      val counts = Array.ofDim[Int](3)
      tile.foreachDouble { (b, z) =>
        z should be (b + 1.0)
        counts(b) += 1
      }

      counts(0)  should be (cellCount)
      counts(1)  should be (cellCount)
      counts(2)  should be (cellCount)
    }

    it("should foreachDouble all bands, tiled, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-pixel.tif")).tile.toArrayTile

      val cellCount = tile.band(1).toArray.size

      val counts = Array.ofDim[Int](3)
      tile.foreachDouble { (b, z) =>
        z should be (b + 1.0)
        counts(b) += 1
      }

      counts(0)  should be (cellCount)
      counts(1)  should be (cellCount)
      counts(2)  should be (cellCount)
    }

    it("should foreachDouble all bands, striped, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-band.tif")).tile.toArrayTile

      val cellCount = tile.band(1).toArray.size

      val counts = Array.ofDim[Int](3)
      tile.foreachDouble { (b, z) =>
        z should be (b + 1.0)
        counts(b) += 1
      }

      counts(0)  should be (cellCount)
      counts(1)  should be (cellCount)
      counts(2)  should be (cellCount)
    }

    it("should foreachDouble all bands, tiled, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-band.tif")).tile.toArrayTile

      val cellCount = tile.band(1).toArray.size

      val counts = Array.ofDim[Int](3)
      tile.foreachDouble { (b, z) =>
        z should be (b + 1.0)
        counts(b) += 1
      }

      counts(0)  should be (cellCount)
      counts(1)  should be (cellCount)
      counts(2)  should be (cellCount)
    }
  }

  describe("GeoTiffMultibandTile multiband foreach") {
    it("should multiband foreach all values, striped, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile

      val bandCount = tile.bandCount
      val cellCount = tile.rows * tile.cols

      var counts = 0
      tile.foreach { value =>
        value.length should be(bandCount)
        counts += 1
      }

      counts should be(cellCount)
    }

    it("should multiband foreach all values, tiled, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-pixel.tif")).tile.toArrayTile

      val bandCount = tile.bandCount
      val cellCount = tile.rows * tile.cols

      var counts = 0
      tile.foreach { value =>
        value.length should be(bandCount)
        counts += 1
      }

      counts should be(cellCount)
    }

    it("should multiband foreach all values, striped, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-band.tif")).tile.toArrayTile

      val bandCount = tile.bandCount
      val cellCount = tile.rows * tile.cols

      var counts = 0
      tile.foreach { value =>
        value.length should be(bandCount)
        counts += 1
      }

      counts should be(cellCount)
    }

    it("should multiband foreach all values, tiled, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-band.tif")).tile.toArrayTile

      val bandCount = tile.bandCount
      val cellCount = tile.rows * tile.cols

      var counts = 0
      tile.foreach { value =>
        value.length should be(bandCount)
        counts += 1
      }

      counts should be(cellCount)
    }

    it("should multiband foreachDouble all values, striped, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile.toArrayTile

      val bandCount = tile.bandCount
      val cellCount = tile.rows * tile.cols

      var counts = 0
      tile.foreachDouble { value =>
        value.length should be(bandCount)
        counts += 1
      }

      counts should be(cellCount)
    }

    it("should multiband foreachDouble all values, tiled, pixel interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-pixel.tif")).tile.toArrayTile

      val bandCount = tile.bandCount
      val cellCount = tile.rows * tile.cols

      var counts = 0
      tile.foreachDouble { value =>
        value.length should be(bandCount)
        counts += 1
      }

      counts should be(cellCount)
    }

    it("should multiband foreachDouble all values, striped, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-band.tif")).tile.toArrayTile

      val bandCount = tile.bandCount
      val cellCount = tile.rows * tile.cols

      var counts = 0
      tile.foreachDouble { value =>
        value.length should be(bandCount)
        counts += 1
      }

      counts should be(cellCount)
    }

    it("should multiband foreachDouble all values, tiled, band interleave") {

      val tile =
        MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-tiled-band.tif")).tile.toArrayTile

      val bandCount = tile.bandCount
      val cellCount = tile.rows * tile.cols

      var counts = 0
      tile.foreachDouble { value =>
        value.length should be(bandCount)
        counts += 1
      }

      counts should be(cellCount)
    }
  }

  describe("GeoTiffMultibandTile combine") {
    val original = IntConstantTile(199, 4, 4)

    def mkGeotiffMultibandTile(arity: Int) =
      GeoTiffMultibandTile(ArrayMultibandTile((0 to arity) map (_ => original)))

    def combineAssert(combined: Tile, arity: Int) = {
      val expected = IntConstantTile(199 * arity, 4, 4)
      assertEqual(combined, expected)
    }

    it("GeoTiffMultibandTile combine function test: arity 2") {
      val arity = 2
      val combined = mkGeotiffMultibandTile(arity).combine(0, 1) { case (b0, b1) => b0 + b1 }
      combineAssert(combined, arity)
    }

    it("GeoTiffMultibandTile combine function test: arity 3") {
      val arity = 3
      val combined = mkGeotiffMultibandTile(arity).combine(0, 1, 2) { case (b0, b1, b2) => b0 + b1 + b2 }
      combineAssert(combined, arity)
    }

    it("GeoTiffMultibandTile combine function test: arity 4") {
      val arity = 4
      val combined = mkGeotiffMultibandTile(arity).combine(0, 1, 2, 3) {
        case (b0, b1, b2, b3) => b0 + b1 + b2 + b3
      }
      combineAssert(combined, arity)
    }

    it("GeoTiffMultibandTile combine function test: arity 5") {
      val arity = 5
      val combined = mkGeotiffMultibandTile(arity).combine(0, 1, 2, 3, 4) {
        case (b0, b1, b2, b3, b4) => b0 + b1 + b2 + b3 + b4
      }
      combineAssert(combined, arity)
    }

    it("GeoTiffMultibandTile combine function test: arity 6") {
      val arity = 6
      val combined = mkGeotiffMultibandTile(arity).combine(0, 1, 2, 3, 4, 5) {
        case (b0, b1, b2, b3, b4, b5) => b0 + b1 + b2 + b3 + b4 + b5
      }
      combineAssert(combined, arity)
    }

    it("GeoTiffMultibandTile combine function test: arity 7") {
      val arity = 7
      val combined = mkGeotiffMultibandTile(arity).combine(0, 1, 2, 3, 4, 5, 6) {
        case (b0, b1, b2, b3, b4, b5, b6) => b0 + b1 + b2 + b3 + b4 + b5 + b6
      }
      combineAssert(combined, arity)
    }

    it("GeoTiffMultibandTile combine function test: arity 8") {
      val arity = 8
      val combined = mkGeotiffMultibandTile(arity).combine(0, 1, 2, 3, 4, 5, 6, 7) {
        case (b0, b1, b2, b3, b4, b5, b6, b7) => b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7
      }
      combineAssert(combined, arity)
    }

    it("GeoTiffMultibandTile combine function test: arity 9") {
      val arity = 9
      val combined = mkGeotiffMultibandTile(arity).combine(0, 1, 2, 3, 4, 5, 6, 7, 8) {
        case (b0, b1, b2, b3, b4, b5, b6, b7, b8) => b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7 + b8
      }
      combineAssert(combined, arity)
    }

    it("GeoTiffMultibandTile combine function test: arity 10") {
      val arity = 10
      val combined = mkGeotiffMultibandTile(arity).combine(0, 1, 2, 3, 4, 5, 6, 7, 8, 9) {
        case (b0, b1, b2, b3, b4, b5, b6, b7, b8, b9) => b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7 + b8 + b9
      }
      combineAssert(combined, arity)
    }
  }

  describe("GeoTiffMultibandTile crop") {
    val pixelStripedGeoTiff = GeoTiffReader.readMultiband(geoTiffPath("3bands/int32/3bands-striped-pixel.tif"))
    val pixelTiledGeoTiff = GeoTiffReader.readMultiband(geoTiffPath("3bands/int32/3bands-tiled-pixel.tif"))

    val bandStripedGeoTiff = GeoTiffReader.readMultiband(geoTiffPath("3bands/int32/3bands-striped-band.tif"))
    val bandTiledGeoTiff = GeoTiffReader.readMultiband(geoTiffPath("3bands/int32/3bands-tiled-band.tif"))

    val pixelStripedRasterExtent = RasterExtent(pixelStripedGeoTiff.extent, pixelStripedGeoTiff.cols, pixelStripedGeoTiff.rows)
    val bandStripedRasterExtent = RasterExtent(bandStripedGeoTiff.extent, bandStripedGeoTiff.cols, bandStripedGeoTiff.rows)

    it("should have the correct number of subset bands - pixel") {
      val bounds = GridBounds(pixelStripedRasterExtent.dimensions)
      val cropped = pixelStripedGeoTiff.tile.cropBands(bounds, Array(1, 0))

      cropped.bands.size should be (2)
    }

    it("should have the correct number of subset bands - band") {
      val bounds = GridBounds(bandStripedRasterExtent.dimensions)
      val cropped = bandStripedGeoTiff.tile.cropBands(bounds, Array(1))

      cropped.bands.size should be (1)
    }

    it("should have the crop the correct area - pixel striped") {
      val bounds = GridBounds(pixelStripedRasterExtent.dimensions)
      val actual = pixelStripedGeoTiff.tile.cropBands(bounds, Array(1, 0, 2))
      val expected = pixelStripedGeoTiff.crop(bounds).tile.subsetBands(1, 0, 2)

      actual should be (expected)
    }

    it("should have the crop the correct area - pixel tiled") {
      val bounds = GridBounds(pixelStripedRasterExtent.dimensions)
      val actual = pixelTiledGeoTiff.tile.cropBands(bounds, Array(1, 0, 2))
      val expected = pixelTiledGeoTiff.crop(bounds).tile.subsetBands(1, 0, 2)

      actual should be (expected)
    }

    it("should have the crop the correct area - band striped") {
      val bounds = GridBounds(bandStripedRasterExtent.dimensions)
      val actual = bandStripedGeoTiff.tile.cropBands(bounds, Array(1, 2, 0))
      val expected = bandStripedGeoTiff.crop(bounds).tile.subsetBands(1, 2, 0)

      actual should be (expected)
    }

    it("should have the crop the correct area - band tiled") {
      val bounds = GridBounds(bandStripedRasterExtent.dimensions)
      val actual = bandTiledGeoTiff.tile.cropBands(bounds, Array(1, 2, 0))
      val expected = bandTiledGeoTiff.crop(bounds).tile.subsetBands(1, 2, 0)

      actual should be (expected)
    }
  }

  describe("GeoTiffMultibandTile streaming read") {
    it("reads over-buffered windows"){
      val path = geoTiffPath("3bands/int32/3bands-striped-pixel.tif")
      val info = GeoTiffReader.readGeoTiffInfo(
        ByteBuffer.wrap(Filesystem.slurp(path)),
        streaming = true, withOverviews = false,
        byteReaderExternal = None
      )
      val tiff = GeoTiffReader.geoTiffMultibandTile(info)

      val windows: Array[GridBounds[Int]] = info
        .segmentLayout
        .listWindows(10)
        .map(_.buffer(5))

      val tiles = tiff.crop(windows)
    }
  }
}
