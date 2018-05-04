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

package geotrellis.raster.io.geotiff.writer

import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.testkit._
import geotrellis.util._

import org.scalatest._

import java.io._

class GeoTiffWriterTests extends FunSuite
    with BeforeAndAfterAll
    with RasterMatchers
    with GeoTiffTestUtils {

  override def afterAll = purge

  test("Writing out an LZW raster from a streaming reader, and compressed (#2177)") {
    /** This issue arose from immediately writing a compressed GeoTiff, without ever uncompressing it.
      * We don't have support for writing LZW from uncompressed values, but we allow LZW to be used
      * if the original compressed GeoTiff segments exist. There was an issue with Little Endian byte order
      * with a passed through LZW compressor. Also, the predictor tag was not written. The streaming problem of
      * the original issue was solved by removing the assumption that a SegmentBytes, when iterated over,
      * would return the segment bytes in segment index order.
      */
    val temp = File.createTempFile("geotiff-writer", ".tif")
    val path = temp.getPath

    addToPurge(path)

    val p = geoTiffPath("lzw-streaming-bug-2177.tif")
    val rr = FileRangeReader(p)
    val reader = StreamingByteReader(rr)

    val gt2 = MultibandGeoTiff.streaming(reader)
    val gt3 = MultibandGeoTiff(p)
    val gt1 = gt3.tile.toArrayTile

      withClue("Assumption failed: Reading GeoTiff two ways didn't match") {
      assertEqual(gt2.tile, gt1)
    }

    withClue("Assumption failed: Reading GeoTiff compressed doesn't work") {
      assertEqual(gt3.tile, gt1)
    }

    gt3.write(path)

    val resultComp = MultibandGeoTiff(path)
    withClue("Writing from a compressed read produced incorrect GeoTiff.") {
      assertEqual(resultComp.tile, gt1)
    }

    gt2.write(path)

    val result = MultibandGeoTiff(path)
    withClue("Writing from a streaming read produced incorrect GeoTiff.") {
      assertEqual(result.tile, gt1)
    }
  }

  test("Writing out a bit raster and reading it back again") {
    val temp = File.createTempFile("geotiff-writer", ".tif")
    val path = temp.getPath

    // ExpandPacked8ToByte1

    // val (p1, p2) = ("/Users/rob/data/DevelopedLand-ch.tiff", "/Users/rob/data/DevelopedLand-ch-2.tiff")
    val (p1, p2) = ("/Users/rob/data/DevelopedLand-sm.tiff", "/Users/rob/data/DevelopedLand-sm-2.tiff")
    // val (p1, p2) = ("/Users/rob/data/DevelopedLand-df.tiff", "/Users/rob/data/DevelopedLand-df-2.tiff")
    val p3 = "/users/rob/data/DevelopedLand-sm-nb.tiff"
    val base = SinglebandGeoTiff(geoTiffPath("small-bit-raster.tif"))
    val tiff = SinglebandGeoTiff(base.tile, base.extent, base.crs)

    GeoTiffWriter.write(tiff, path)

    val reread = SinglebandGeoTiff(path)

    addToPurge(path)

    assertEqual(reread.raster.tile, base.raster.tile)
  }
}
