/*
 * Copyright 2020 Azavea
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

package geotrellis.raster.io.geotiff.compression

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.testkit._
import geotrellis.vector.Extent
import java.net.URL
import java.io.File
import scala.collection.parallel._
import scala.util.Random
import sys.process._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec

class JpegCompressionSpec extends AnyFunSpec
    with RasterMatchers
    with BeforeAndAfterAll
    with GeoTiffTestUtils {

  override def afterAll = purge

  describe("Reading GeoTiffs with JPEG compression") {
    it("Does not cause Too many open files exception") {
      /*
       * Tests a resource closing issue that appeared in the JPEGDecompression logic.
       * See https://github.com/locationtech/geotrellis/pull/3249 for details.
       */

      val url = "https://oin-hotosm.s3.amazonaws.com/5ed6406bb2d2d20005f78420/0/5ed6406bb2d2d20005f78421.tif"

      val temp = File.createTempFile("oam-scene", ".tif")
      val jpegRasterPath = temp.getPath

      addToPurge(jpegRasterPath)

      println(f"Downloading ${url}...")

      new URL(url) #> new File(jpegRasterPath) !!

      println(f"Starting test...")

      val extent = RasterSource(jpegRasterPath).metadata.gridExtent.extent

      val parList = (1 to 10000).toList.par
      // TODO: Replace with java.util.concurrent.ForkJoinPool once we drop 2.11 support.
      val forkJoinPool = new scala.concurrent.forkjoin.ForkJoinPool(50)
      parList.tasksupport = new ForkJoinTaskSupport(forkJoinPool)

      try {
        parList.foreach { _ =>
          val (xmin, ymin) = (
            (Random.nextDouble * (extent.width - 1)) + extent.xmin,
            (Random.nextDouble * (extent.height - 1)) + extent.ymin
          )

          val windowExtent = Extent(
            xmin,
            ymin,
            xmin + 1,
            ymin + 1
          )

          RasterSource(jpegRasterPath).read(windowExtent).map { r =>
            // Do something to ensure the JVM doesn't optimize things away.
            val m = r._1.band(1).mutable
            m.set(0, 0, 1)
          }

          info("READ")
        }
      } finally {
        forkJoinPool.shutdown()
      }

      println("DONE")

    }
  }
}
