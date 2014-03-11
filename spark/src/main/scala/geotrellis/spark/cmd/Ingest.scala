/***
 * Copyright (c) 2014 Digital Globe.
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
 ***/

package geotrellis.spark.cmd
import geotrellis._
import geotrellis.data.GeoTiff
import geotrellis.data.GeoTiff.Metadata
import geotrellis.raster.RasterData
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.rdd.RasterSplitGenerator
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.HdfsUtils
import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile
import org.apache.spark.Logging
import org.geotools.coverage.grid.GridCoverage2D

import java.awt.image.DataBufferByte
import java.awt.image.DataBufferDouble
import java.awt.image.DataBufferFloat
import java.awt.image.DataBufferInt
import java.awt.image.DataBufferShort
import java.net.URL

import com.quantifind.sumac.ArgMain

/**
 * @author akini
 *
 * Ingest GeoTIFFs into ArgWritable.
 *
 * Ingest --input <path-to-tiffs> --output <path-to-raster> --sparkMaster <spark-master-ip>
 *
 * e.g., Ingest --input file:///home/akini/test/small_files/all-ones.tif --output file:///tmp/all-ones
 *
 * Constraints:
 *
 * --input <path-to-tiffs> - this can either be a directory or a single tiff file and has to be on the local fs.
 * Currently, mosaicing is not implemented so only the single tiff file case is tested
 *
 * --output <path-to-raster> - this can be either on hdfs (hdfs://) or local fs (file://). If the directory
 * already exists, it is deleted
 *
 *
 * Outstanding issues:
 * 1. Mosaicing overlapping tiles
 *
 */
object Ingest extends ArgMain[CommandArguments] with Logging {

  final val DefaultProjection = "EPSG:4326"
  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  //var dumpDir: String = null

  def main(args: CommandArguments) {
    val inPath = new Path(args.input)
    val outPath = new Path(args.output)

    /*dumpDir = args.dumpDir
    logInfo(s"Deleting and creating dump directory: $dumpDir")
    val dir = new File(dumpDir)
    dir.delete()
    dir.mkdirs()*/

    val conf = SparkUtils.createHadoopConfiguration

    val (files, meta) = PyramidMetadata.fromTifFiles(inPath, conf)
    println("------- FILES ------")
    println(files.mkString("\n"))
    println("\n\n\n")
    println("------- META ------")
    println(meta)

    val tiles = files.flatMap(file => tiffToTiles(file, meta))

    logInfo(s"Deleting and creating output path: $outPath")
    val outFs = outPath.getFileSystem(conf)
    outFs.delete(outPath, true)
    outFs.mkdirs(outPath)

    logInfo("Saving metadata: ")
    logInfo(meta.toString)
    meta.save(outPath, conf)

    val outPathWithZoom = new Path(outPath, meta.maxZoomLevel.toString)
    logInfo(s"Creating Output Path With Zoom: $outPathWithZoom")
    outFs.mkdirs(outPathWithZoom)

    val tileExtent = meta.metadataForBaseZoom.tileExtent
    val (zoom, tileSize, rasterType) = (meta.maxZoomLevel, meta.tileSize, meta.rasterType)
    val splitGenerator = RasterSplitGenerator(tileExtent, zoom,
      TmsTiling.tileSizeBytes(tileSize, rasterType),
      HdfsUtils.blockSize(conf))

    val partitioner = TileIdPartitioner(splitGenerator, outPathWithZoom, conf)

    logInfo("Saving splits: " + partitioner)

    val key = new TileIdWritable()

    // open as many writers as number of partitions
    def openWriters(num: Int) = {
      val writers = new Array[MapFile.Writer](num)
      for (i <- 0 until num) {
        val mapFilePath = new Path(outPathWithZoom, f"part-${i}%05d")

        writers(i) = new MapFile.Writer(conf, outFs, mapFilePath.toUri.toString,
          classOf[TileIdWritable], classOf[ArgWritable], SequenceFile.CompressionType.NONE)

      }
      writers
    }

    conf.set("io.map.index.interval", "1")
    val writers = openWriters(partitioner.numPartitions)
    try {
      tiles.foreach {
        case (tileId, tile) => {
          key.set(tileId)
          writers(partitioner.getPartition(key)).append(key, ArgWritable.fromRasterData(tile.toArrayRaster.data))
          //logInfo(s"Saved tileId=${tileId},partition=${partitioner.getPartition(key)}")
        }
      }
    } finally {
      writers.foreach(_.close)
    }
    logInfo(s"Done saving ${tiles.length} tiles")
  }

  private def warp(image: GridCoverage2D, imgMeta: Metadata, pyMeta: PyramidMetadata): Raster = {

    val (pixelWidth, pixelHeight) = imgMeta.pixels

    val (zoom, tileSize, rasterType, nodata) =
      (pyMeta.maxZoomLevel, pyMeta.tileSize, pyMeta.rasterType, pyMeta.nodata)

    def buildRaster = {
      val extent = Extent(imgMeta.bounds.getLowerCorner.getOrdinate(0),
        imgMeta.bounds.getLowerCorner.getOrdinate(1),
        imgMeta.bounds.getUpperCorner.getOrdinate(0),
        imgMeta.bounds.getUpperCorner.getOrdinate(1))
      val re = RasterExtent(extent, pixelWidth, pixelHeight)
      val rawDataBuff = image.getRenderedImage().getData().getDataBuffer()
      val rd = rasterType match {
        case TypeDouble => RasterData(rawDataBuff.asInstanceOf[DataBufferDouble].getData(), tileSize, tileSize)
        case TypeFloat  => RasterData(rawDataBuff.asInstanceOf[DataBufferFloat].getData(), tileSize, tileSize)
        case TypeInt    => RasterData(rawDataBuff.asInstanceOf[DataBufferInt].getData(), tileSize, tileSize)
        case TypeShort  => RasterData(rawDataBuff.asInstanceOf[DataBufferShort].getData(), tileSize, tileSize)
        case TypeByte   => RasterData(rawDataBuff.asInstanceOf[DataBufferByte].getData(), tileSize, tileSize)
        case _          => sys.error("Unrecognized AWT type - " + rasterType)
      }
      val trd = NoDataHandler.removeUserNoData(rd, nodata)
      Raster(trd, re)
    }

    val origRaster = buildRaster
    val res = TmsTiling.resolution(zoom, tileSize)
    val newRe = RasterExtent(origRaster.rasterExtent.extent, res, res)
    //println(s"re: ${re},newRe: ${newRe}")
    //val start = System.currentTimeMillis
    //println(s"[extent,cols,rows,cellwidth,cellheight,rdLen,bufLen]: ")
    //println(s"Before Warp: [${r.rasterExtent},${r.cols},${r.rows},${r.rasterExtent.cellwidth},${r.rasterExtent.cellheight},${r.toArrayRaster.data.length},${rawDataBuff.asInstanceOf[DataBufferFloat].getData().length}]")
    val warpRaster = origRaster.warp(newRe)
    //val end = System.currentTimeMillis
    //println(s"After Warp: [${wr.rasterExtent},${wr.cols},${wr.rows},${wr.rasterExtent.cellwidth},cellheight=${wr.rasterExtent.cellheight},${wr.toArrayRaster.data.length}]")
    //println(s"Warp operation took ${end - start} ms.")
    warpRaster
  }

  private def tiffToTiles(file: Path, pyMeta: PyramidMetadata): List[(Long, Raster)] = {
    val url = new URL(file.toUri().toString())
    val image = GeoTiff.getGridCoverage2D(url)
    val imageMeta = GeoTiff.getMetadata(url).get
    val (zoom, tileSize, rasterType, nodata) =
      (pyMeta.maxZoomLevel, pyMeta.tileSize, pyMeta.rasterType, pyMeta.nodata)

    val raster = warp(image, imageMeta, pyMeta)

    val tileExtent = pyMeta.metadataForBaseZoom.tileExtent
    val tiles = for {
      ty <- tileExtent.ymin to tileExtent.ymax
      tx <- tileExtent.xmin to tileExtent.xmax
      tileId = TmsTiling.tileId(tx, ty, zoom)
      extent = TmsTiling.tileToExtent(tx, ty, zoom, tileSize)
      cropRasterTmp = CroppedRaster(raster, extent)
      // TODO - do away with the second crop. It is put in as workaround for the case when 
      // CroppedRaster's translation from Extent to gridBounds ends up giving us an extra row/col
      cropRaster = CroppedRaster(cropRasterTmp, GridBounds(0, 0, tileSize - 1, tileSize - 1))
    } yield (tileId, cropRaster)

    /* debugging stuff 
     
     def dump = {
      logInfo("-------------- dumping data for debugging purposes -------------")
      logInfo(s"first, dumping the original raster before warping to ${dumpDir}/original.tif")
      GeoTiffWriter.write(s"$dumpDir/original.tif", r, pyMeta.nodata)

      logInfo(s"now, dumping the warped raster to ${dumpDir}/warped.tif")
      GeoTiffWriter.write(s"$dumpDir/warped.tif", wr, pyMeta.nodata)

      logInfo(s"finally, dumping the tiles to ${dumpDir}/tile-[tileId].tif")
      tiles.foreach {
        case (tileId, cr1, cr2) => {
          val (tx, ty) = TmsTiling.tileXY(tileId, zoom)
          GeoTiffWriter.write(s"${dumpDir}/tile-${tileId}.tif", cr2, pyMeta.nodata)
          logInfo(s"---------tx: $tx, ty: $ty file: tile-${tileId}.tif: cr1: rows=${cr1.rows}, cols=${cr1.cols}, rdLen=${cr1.toArrayRaster.data.length}, cr2: rows=${cr2.rows}, cols=${cr2.cols}, rdLen=${cr2.toArrayRaster.data.length}")
        }
      }
      logInfo("-------------- end dumping data for debugging purposes -------------")

    }
    dump
    
    end debugging stuff */

    tiles.toList
  }

  /* debugging only */
  private def inspect(tile: GridCoverage2D): Unit = {
    val env = tile.getEnvelope()
    val r = tile.getRenderedImage().getData()
    for {
      py <- 0 until r.getHeight()
      if (py < 10)
      px <- 0 until r.getWidth()
      if (px < 10)
    } {
      val d = r.getSampleFloat(px, py, 0)
      println(s"($px,$py)=$d")
    }
  }
}