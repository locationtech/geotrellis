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
import geotrellis._
import geotrellis.spark.Tile
import geotrellis.spark.cmd.args.HadoopArgs
import geotrellis.spark.cmd.args.SparkArgs
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.ingest.IngestInputFormat
import geotrellis.spark.ingest.TiffTiler
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.rdd.RasterSplitGenerator
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.HdfsUtils
import geotrellis.spark.utils.SparkUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile
import org.apache.spark.Logging
import org.apache.spark.SerializableWritable
import org.apache.spark.SparkContext._
import org.geotools.coverage.grid.GridCoverage2D
import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.FieldArgs
import com.quantifind.sumac.validation.Required
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.rdd.SplitGenerator
import geotrellis.spark.metadata.RasterMetadata
import geotrellis.spark.rdd.MultiLevelTileIdPartitioner
import geotrellis.spark.formats.MultiLevelTileIdWritable

class BuildPyramidArgs extends SparkArgs with HadoopArgs {
  @Required var pyramid: String = _
}

object BuildPyramid extends ArgMain[BuildPyramidArgs] with Logging {

  private def fillRasterMetadata(meta: PyramidMetadata): PyramidMetadata = {
    val newRasterMetadata = meta.rasterMetadata ++
      ((1 until meta.maxZoomLevel) map { zoom =>
        val tileExtent = TmsTiling.extentToTile(meta.extent, zoom, meta.tileSize)
        val pixelExtent = TmsTiling.extentToPixel(meta.extent, zoom, meta.tileSize)
        (zoom.toString -> RasterMetadata(pixelExtent, tileExtent))
      }).toMap

    val newMeta = meta.copy(rasterMetadata = newRasterMetadata)
    newMeta
  }
  private def getSplits(meta: PyramidMetadata, blockSize: Long): Map[Int, SplitGenerator] = {
    println("Using blocksize = " + blockSize)
    val tileSize = TmsTiling.tileSizeBytes(meta.tileSize, meta.rasterType)
    for ((zoom, rm) <- meta.rasterMetadata if zoom.toInt < meta.maxZoomLevel)
      yield (zoom.toInt -> RasterSplitGenerator(rm.tileExtent, zoom.toInt, tileSize, blockSize))
  }

  private def decimate(tile: Tile, meta: PyramidMetadata) = {
    val (parTx, parTy) = TmsTiling.tileXY(tile.id, meta.maxZoomLevel)
    val parExtent = TmsTiling.tileToExtent(parTx, parTy, meta.maxZoomLevel, meta.tileSize)
    for (
      zoom <- 1 until meta.maxZoomLevel;
      childTileCoord = TmsTiling.latLonToTile(parExtent.ymin, parExtent.xmin, zoom, meta.tileSize);
      childTileId = TmsTiling.tileId(childTileCoord.tx, childTileCoord.ty, zoom)
    ) yield (MultiLevelTileIdWritable(childTileId, zoom, tile.id), ArgWritable.fromRasterData(tile.raster.data))
  }

  def main(args: BuildPyramidArgs) {
    val sc = args.sparkContext("BuildPyramid")
    val pyramid = new Path(args.pyramid)
    val conf = args.hadoopConf
    val meta = PyramidMetadata(pyramid, conf)

    println(s"BEFORE: ${meta}")
    val newMeta = fillRasterMetadata(meta)
    println(s"AFTER: ${newMeta}")

    val splits = getSplits(newMeta, HdfsUtils.defaultBlockSize(pyramid, conf))

    // TODO - revisit the "saving" of splits 
    // 1. Splits shouldn't be saved before the job is complete - needs refactoring of TileIdPartitioner's apply that takes gen
    // 2. The base zoom level shouldn't be rewritten. This will be made easier once #1 is done. 
    // Addendum for #2: base zoom is no longer part of mp 
    val mp = MultiLevelTileIdPartitioner(splits, pyramid, conf)
    println(mp)

    val rasterPath = new Path(pyramid, meta.maxZoomLevel.toString)
    val rdd = RasterRDD(rasterPath, sc)
    val broadCastedConf = sc.broadcast(new SerializableWritable(conf))
    val broadCastedMeta = sc.broadcast(newMeta)
    val pyramidStr = pyramid.toUri().toString()
    val res = rdd.mapPartitions({ partition =>
      val meta = broadCastedMeta.value
      partition.map { tile =>
        decimate(tile, meta)
      }.flatten
    }, true)
      .groupByKey(mp)
      .mapPartitions({ partition =>
        {
          val conf = broadCastedConf.value.value
          val buf = partition.toArray.sortWith((x, y) => x._1.get() < y._1.get())
          //println("WHOOO - partitioner = " + mp)
          val (zoom, index) = (buf.head._1.zoom, mp.getPartitionForZoom(buf.head._1))
          val outPath = new Path(pyramidStr, zoom.toString)
          val mapFilePath = new Path(outPath, f"part-${index}%05d")
          println("writing to " + mapFilePath.toString())
          val fs = mapFilePath.getFileSystem(conf)
          val fsRep = fs.getDefaultReplication()
          logInfo(s"Working on partition ${index} with rep = (${conf.getInt("dfs.replication", -1)}, ${fsRep})")
          val writer = new MapFile.Writer(conf, fs, mapFilePath.toUri.toString,
            classOf[TileIdWritable], classOf[ArgWritable], SequenceFile.CompressionType.RECORD)
          buf.foreach(writableTile => println(s"zoom=${zoom}, key=${writableTile._1.get}"))
            //writer.append(writableTile._1.asInstanceOf[TileIdWritable], writableTile._2.head))

          //buf.foreach(writableTile => writer.append(writableTile._1.asInstanceOf[TileIdWritable], writableTile._2.head))
          writer.close()
          partition
        }
      }, true)

    println(s"Result has ${res.count} tuples and partitioner = ${res.partitioner}")
    // TODO - save newMeta
  }
}