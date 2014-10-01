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

package geotrellis.geotools

import geotrellis.raster._
import geotrellis.vector.Extent

import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.coverage.grid.GridEnvelope2D
import org.geotools.factory.Hints
import org.geotools.gce
import org.geotools.gce.geotiff.{ GeoTiffReader => GTGeoTiffReader }
import org.geotools.geometry.GeneralEnvelope
import org.geotools.referencing.CRS

import java.awt.image.DataBuffer._
import java.io.File
import java.net.URL

import spire.syntax.cfor._

object GeoTiffReader {
  def read(path: String): (Tile, Extent) = {
    val reader = getReader(path)
    val cov = reader.read(null)
    val rasterExtent = loadRasterExtent(cov)
    val cellType = getDataType(cov)
    val noData = d2i(reader.getMetadata.getNoData)

    val cols = rasterExtent.cols
    val rows = rasterExtent.rows

    val gtData = cov.getRenderedImage.getData

    val tile = 
      cellType match {
        case TypeBit | TypeByte | TypeShort =>
          val data = Array.ofDim[Int](cols * rows).fill(NODATA)
          gtData.getPixels(0, 0, cols, rows, data)
          val tile = ArrayTile(data.map(_.toShort), cols, rows)
          val nd = s2i(noData.toShort)
          if(isData(nd) && isData(noData)) {
            var conflicts = 0
            cfor(0)(_ < rows, _ + 1) { row =>
              cfor(0)(_ < cols, _ + 1) { col =>
                val z = tile.get(col, row)
                if(isNoData(z)) conflicts += 1
                if (z == nd) { tile.set(col, row, NODATA) }
              }
            }

            if(conflicts > 0) {
              println(s"[WARNING]  GeoTiff contained values of $shortNODATA, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")

            }
          }

          tile
        case TypeInt =>
          val data = Array.ofDim[Int](cols * rows).fill(NODATA)
          gtData.getPixels(0, 0, cols, rows, data)
          val tile = ArrayTile(data, cols, rows)
          val nd = noData.toInt
          if(isData(nd) && isData(noData)) {
            var conflicts = 0
            cfor(0)(_ < rows, _ + 1) { row =>
              cfor(0)(_ < cols, _ + 1) { col =>
                val z = tile.get(col, row)
                if(isNoData(z)) conflicts += 1
                if (z == nd) { tile.set(col, row, NODATA) }
              }
            }

            if(conflicts > 0) {
              println(s"[WARNING]  GeoTiff contained values of $NODATA, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")

            }
          }

          tile
        case TypeFloat =>
          val data = Array.ofDim[Float](cols * rows).fill(Float.NaN)
          gtData.getPixels(0, 0, cols, rows, data)
          val tile = ArrayTile(data, cols, rows)
          val nd = noData.toFloat
          if(isData(nd)) {
            var conflicts = 0
            cfor(0)(_ < rows, _ + 1) { row =>
              cfor(0)(_ < cols, _ + 1) { col =>
                val z = tile.getDouble(col, row)
                if(isNoData(z)) conflicts += 1
                if (z == nd) { tile.setDouble(col, row, Double.NaN) }
              }
            }

            if(conflicts > 0) {
              println(s"[WARNING]  GeoTiff contained values of ${Float.NaN}, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")

            }
          }

          tile
        case TypeDouble =>
          val data = Array.ofDim[Double](cols * rows).fill(Double.NaN)
          gtData.getPixels(0, 0, cols, rows, data)
          val tile = ArrayTile(data, cols, rows)
          if(isData(noData)) {
            var conflicts = 0
            cfor(0)(_ < rows, _ + 1) { row =>
              cfor(0)(_ < cols, _ + 1) { col =>
                val z = tile.getDouble(col, row)
                if(isNoData(z)) conflicts += 1
                if (z == noData) { tile.setDouble(col, row, Double.NaN) }
              }
            }

            if(conflicts > 0) {
              println(s"[WARNING]  GeoTiff contained values of ${Double.NaN}, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")

            }
          }

          tile

      }
    (tile, rasterExtent.extent)
  }

  /* By default we get a EPSG:3785 reader. Since Scala doesn't allow multiple overloaded methods with 
   * default arguments, we have two variants of getReader that take URL instead of one 
   */
  def getReader(url: URL): GTGeoTiffReader =
    getReader(url.asInstanceOf[Object], "EPSG:3785")

  def getReader(url: URL, epsg: String): GTGeoTiffReader =
    getReader(url.asInstanceOf[Object], epsg)

  def getReader(path: String, epsg: String = "EPSG:3785"): GTGeoTiffReader = {
    val fh = new File(path)
    if (!fh.canRead) sys.error(s"can't read $path")

    getReader(fh, epsg)
  }

  private def getReader(o: Object, epsg: String): GTGeoTiffReader = {
    val gtf = new gce.geotiff.GeoTiffFormat
    val hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode(epsg))

    gtf.getReader(o, hints)
  }

  def loadRasterExtent(path: String): RasterExtent =
    loadRasterExtent(getGridCoverage2D(path))

  def loadRasterExtent(cov: GridCoverage2D): RasterExtent = {
    val env = cov.getEnvelope2D
    val e = Extent(env.getMinX, env.getMinY, env.getMaxX, env.getMaxY)

    val data = cov.getRenderedImage.getData
    val rows = data.getHeight
    val cols = data.getWidth

    val cellwidth = (e.xmax - e.xmin) / cols
    val cellheight = (e.ymax - e.ymin) / rows

    RasterExtent(e, cellwidth, cellheight, cols, rows)
  }

  /* By default we use projection EPSG:3785. Since Scala doesn't allow multiple overloaded methods with 
   * default arguments, we have two variants of getGridCoverage2D that take URL instead of one  
   */
  def getGridCoverage2D(url: URL): GridCoverage2D =
    getReader(url).read(null)

  def getGridCoverage2D(url: URL, epsg: String): GridCoverage2D =
    getReader(url, epsg).read(null)

  def getGridCoverage2D(path: String, epsg: String = "EPSG:3785"): GridCoverage2D =
    getReader(path, epsg).read(null)

  def getGridCoverage2D(reader: gce.geotiff.GeoTiffReader): GridCoverage2D =
    reader.read(null)

  def getDataType(reader: gce.geotiff.GeoTiffReader): CellType =
    getDataType(getGridCoverage2D(reader))

  def getDataType(cov: GridCoverage2D): CellType = {
    cov.getRenderedImage.getSampleModel.getDataType match {
      // Bytes are unsigned in GTiff
      case TYPE_BYTE   => TypeShort
      case TYPE_SHORT  => TypeShort
      case TYPE_USHORT => TypeInt
      case TYPE_INT    => TypeInt
      case TYPE_FLOAT  => TypeFloat
      case TYPE_DOUBLE => TypeDouble
      case _           => TypeDouble
    }
  }

}
