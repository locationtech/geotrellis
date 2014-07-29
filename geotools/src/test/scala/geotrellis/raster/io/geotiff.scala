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

package geotrellis.raster.io

import geotrellis.engine._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.GeoTiffWriter
import geotrellis.vector.Extent
import geotrellis.raster.stats.FastMapHistogram

import geotrellis.testkit._

import org.scalatest._

import org.geotools.gce.geotiff.GeoTiffFormat
import org.geotools.factory.Hints
import org.geotools.referencing.CRS
import org.geotools.coverage.grid.GridCoordinates2D



import java.io.{File,FileWriter}
import javax.imageio.ImageIO

import scala.math.{abs, round}

import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder
import org.geotools.coverage.grid.io.imageio.IIOMetadataDumper
import java.awt.image.BufferedImage

import java.awt.image.DataBuffer
import java.awt.Transparency

class GeoTiffSpec extends FunSpec with TestEngine with Matchers {
  describe("A GeoTiffReader") {
    it ("should fail on non-existent files") {
      val path = "/does/not/exist.tif"
      an [Exception] should be thrownBy { GeoTiff.readRaster(path) }
    }

    it ("should load correct extent & gridToMap should work") {
      val path = "raster-test/data/econic.tif"
      val (_, rasterExtent) = GeoTiff.readRaster(path)
      val (xmap, ymap) = rasterExtent.gridToMap(0,0)
      xmap should be (-15381.615 +- 0.001)
      ymap should be (15418.729 +- 0.001)
    }

    it("should correctly translate NODATA values for an int raster which has no NODATA value associated") {
      val path = "geotools/data/cea.tif"
      val (raster, rasterExtent) = GeoTiff.readRaster(path)
      val (cols, rows) = (raster.cols, raster.rows)

      // Find NODATA value
      val reader = GeoTiff.getReader(path)

      val nodata = reader.getMetadata().getNoData()
      val geoRaster = reader.read(null).getRenderedImage.getData
      val data = Array.fill(cols * rows)(nodata)
      geoRaster.getPixels(0, 0, cols, rows, data)

      val rdata = raster.toArray

      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          if(isNoData(rdata(row*cols + col))) {
            val v = data(row*cols + col)
            if(isNoData(nodata))
              isNoData(v) should be (true)
            else
             v should be (nodata)
          }
        }
      }
    }

    it ("should produce same raster for GeoTiff.readRaster and through the Layer") {
      val e = Extent(-15471.6, -15511.3, 15428.4, 15388.7)
      val rasterExtent = RasterExtent(e, 60.0, 60.0, 513, 513)

      val path = "raster-test/data/econic.tif"
      val (raster1, _) = GeoTiff.readRaster(path)//.warp(rasterExtent)

      val raster2 = GeoTiffRasterLayerBuilder.fromTif(path).getRaster()//(Some(rasterExtent))
      assertEqual(raster1,raster2)
    }

    it ("should write and read back in the same") {
      val (r, re) = GeoTiff.readRaster("raster-test/data/econic.tif")
      GeoTiffWriter.write("/tmp/written.tif", r, re.extent, "foo")
      val (r2, re2) = GeoTiff.readRaster("/tmp/written.tif")
      re should be (re2)
      assertEqual(r, r2)
    }

    it ("should translate NODATA correctly") {
      // This test was set up by taking an ARG, slope.arg, and converting it to TIF using GDAL.
      // Then gdalwarp was used to change the NoData value for slope.tif to -9999.
      // If the NoData values are translated correctly, then all NoData values from the read in GTiff
      // should correspond to NoData values of the directly read arg.
      val originalArg = RasterSource.fromPath("raster-test/data/data/slope.arg").get
      val (translatedTif, _) = GeoTiff.readRaster("raster-test/data/slope.tif")

      translatedTif.rows should be (originalArg.rows)
      translatedTif.cols should be (originalArg.cols)
      for(col <- 0 until originalArg.cols) {
        for(row <- 0 until originalArg.rows) {
          if(isNoData(originalArg.getDouble(col,row)))
             isNoData(translatedTif.getDouble(col,row)) should be (true)
          else
            translatedTif.getDouble(col,row) should be (originalArg.getDouble(col,row))
        }
      }
    }
  }
}
