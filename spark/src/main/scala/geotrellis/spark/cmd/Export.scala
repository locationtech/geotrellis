/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.cmd
import geotrellis.raster._
import geotrellis.raster.io.geotiff.GeoTiffWriter

import geotrellis.spark._
import geotrellis.spark.cmd.args.HadoopArgs
import geotrellis.spark.cmd.args.RasterArgs
import geotrellis.spark.cmd.args.SparkArgs
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.storage.RasterReader
import geotrellis.spark.tiling.TmsTiling

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.Logging

import java.io.File

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

/**
 * @author akini
 *
 * Export a raster as GeoTIFF either as a single tiff or tiff-per-tile.
 * The tiff-per-tile use case uses Spark to read the tiles
 *
 *
 * Export 	[--single <boolean>]
 * 			--inputraster <path-to-raster>
 *      	--output <path-to-dir-or-file>
 *       	[--sparkMaster <spark-master-ip>]
 *
 * Single tiff
 * Export --single true --inputraster file:///tmp/all-ones/10 --output /tmp/all-ones-export.tif
 *
 * tiff-per-tile
 * Export --inputraster file:///tmp/all-ones --zoom 10 --output /tmp/all-ones-export --sparkMaster local
 *
 * Constraints:
 *
 * --single <boolean> - this is either true or false (default) depending on whether the output needs
 * to be a single merged tiff or tiff-per-tile
 *
 * --inputraster <path-to-raster> - this can be either on hdfs (hdfs://) or local fs (file://) and is a fully
 * qualified path to the raster
 *
 * --output <path-to-dir-or-file> - this is a file in the case of a single tiff, and a directory in
 * the case of tiff-per-tile. Either way, the output would be on the local file system, and cannot have the 
 * "file://" scheme in it. 
 *
 *
 */
class ExportArgs extends RasterArgs with SparkArgs with HadoopArgs {
  @Required var output: String = _

  var single: Boolean = false
}

object Export extends ArgMain[ExportArgs] with Logging {

  def main(args: ExportArgs) {
    if (args.single)
      exportSingle(args)
    else
      exportTiles(args)
  }

  private def exportSingle(args: ExportArgs) {
    val (rasterPath, zoom, output, meta, hadoopConf) = extractFromArgs(args)
    
    // get extents and layout
    val (tileSize, cellType) = (meta.tileSize, meta.cellType)
    val tileExtent = meta.rasterMetadata(zoom.toString).tileExtent
    val res = TmsTiling.resolution(zoom, tileSize)
    val extent = TmsTiling.tileToExtent(tileExtent, zoom, tileSize)
    val layout = TileLayout(tileExtent.width.toInt, tileExtent.height.toInt, tileSize, tileSize)

    // open the reader
    val reader = RasterReader(rasterPath, hadoopConf)

    // TMS tiles start from lower left corner whereas CompositeTile expects them to start from  
    // upper left, so we need to re-sort the array
    def compare(left: TmsTile, right: TmsTile): Boolean = {
      val (lx, ly) = left.tileXY(zoom)
      val (rx, ry) = right.tileXY(zoom)
      (ly > ry) || (ly == ry && lx < rx)
    }

    val tiles =
      reader.map(_.toTmsTile(meta, zoom))
        .toList
        .sortWith(compare)
        .map(_.tile)

    reader.close()

    val tile = CompositeTile(tiles, layout).toArrayTile
    GeoTiffWriter.write(s"${output}", tile, extent, meta.nodata)
    logInfo(s"---------finished writing to file ${output}")
  }

  private def exportTiles(args: ExportArgs) {
    val (rasterPath, zoom, output, meta, hadoopConf) = extractFromArgs(args)
    
    val (tileSize, cellType) = (meta.tileSize, meta.cellType)

    val sc = args.sparkContext("Export")

    logInfo(s"Deleting and creating output directory: $output")
    val dir = new File(output)
    dir.delete()
    dir.mkdirs()

    try {
      val rrdd = RasterRDD(rasterPath.toUri.toString, sc, true)

      for (tmsTile <- rrdd) {
        val (tx, ty) = tmsTile.tileXY(zoom)
        val extent = TmsTiling.tileToExtent(tmsTile.id, zoom, rrdd.opCtx.rasterDefinition.tileLayout.pixelCols)
        GeoTiffWriter.write(s"${output}/tile-${tmsTile.id}.tif", tmsTile.tile, extent, meta.nodata)
        logInfo(s"---------tx: ${tx}, ty: ${ty} file: tile-${tmsTile.id}.tif")
      }

      logInfo(s"Exported ${rrdd.count} tiles to $output")
    }
    finally {
      sc.stop
      System.clearProperty("spark.master.port")
    }
  }

  private def extractFromArgs(args: ExportArgs): Tuple5[Path, Int, String, PyramidMetadata, Configuration] = {
    val rasterPath = new Path(args.inputraster)
    val (pyramidPath, zoom) = (rasterPath.getParent, rasterPath.getName.toInt)
    val output = args.output
    val hadoopConf = args.hadoopConf
    val meta = PyramidMetadata(pyramidPath, hadoopConf)
    (rasterPath, zoom, output, meta, hadoopConf)
  }
}
