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
import geotrellis.RasterExtent
import geotrellis.TypeByte
import geotrellis.TypeDouble
import geotrellis.TypeFloat
import geotrellis.TypeInt
import geotrellis.TypeShort
import geotrellis.spark.ingest.GeoTiff
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
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import geotrellis.spark.ingest.IngestInputFormat
import geotrellis.spark.Tile
import org.apache.spark.SparkContext._
object ParallelIngest extends ArgMain[CommandArguments] with Logging {

  final val DefaultProjection = "EPSG:4326"
  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def main(args: CommandArguments) {
    val inPath = new Path(args.input)
    val outPath = new Path(args.output)
    val sparkMaster = args.sparkMaster

    val conf = SparkUtils.createHadoopConfiguration

    val (files, meta) = PyramidMetadata.fromTifFiles(inPath, conf)
    println("------- FILES ------")
    println(files.mkString("\n"))
    println("\n\n\n")
    println("------- META ------")
    println(meta)

    logInfo(s"Deleting and creating output path: $outPath")
    val outFs = outPath.getFileSystem(conf)
    outFs.delete(outPath, true)
    outFs.mkdirs(outPath)

    logInfo("Saving metadata: ")
    logInfo(meta.toString)
    meta.save(outPath, conf)
    meta.writeToJobConf(conf)

    val outPathWithZoom = new Path(outPath, meta.maxZoomLevel.toString)
    logInfo(s"Creating Output Path With Zoom: $outPathWithZoom")
    outFs.mkdirs(outPathWithZoom)

    val tileExtent = meta.metadataForBaseZoom.tileExtent
    val (zoom, tileSize, rasterType) = (meta.maxZoomLevel, meta.tileSize, meta.rasterType)
    val splitGenerator = RasterSplitGenerator(tileExtent, zoom,
      TmsTiling.tileSizeBytes(tileSize, rasterType),
      HdfsUtils.blockSize(conf))

    val partitioner = TileIdPartitioner(splitGenerator, outPathWithZoom, conf)

    logInfo(s"Saving ${partitioner.numPartitions - 1} splits: " + partitioner)

    val job = new Job(conf)
    FileInputFormat.setInputPaths(job, files.mkString(","))
    val sc = SparkUtils.createSparkContext(sparkMaster, "Ingest")
    val rdd = sc.newAPIHadoopRDD[Long, Raster, IngestInputFormat](job.getConfiguration(),
      classOf[IngestInputFormat], classOf[Long], classOf[Raster])
    val broadCastedConf = sc.broadcast(conf)
    val outPathWithZoomStr = outPathWithZoom.toUri().toString()
    val res =
      rdd.map(t => Tile(t._1, t._2).toWritable)
        .partitionBy(partitioner)
        .reduceByKey((tile1, tile2) => tile2) // pick the later one
        .mapPartitionsWithIndex({ (index, iter) =>
          {
            logInfo("Working on partition " + index)
            val buf = iter.toArray.sortWith((x,y) => x._1.get() < y._1.get())
            
            val mapFilePath = new Path(outPathWithZoomStr, f"part-${index}%05d")
            val fs = mapFilePath.getFileSystem(broadCastedConf.value)
            val writer = new MapFile.Writer(broadCastedConf.value, fs, mapFilePath.toUri.toString,
              classOf[TileIdWritable], classOf[ArgWritable], SequenceFile.CompressionType.RECORD)
            buf.foreach(writableTile => writer.append(writableTile._1, writableTile._2))
            writer.close()
            iter
          }
        }, true)
    logInfo("rdd has " + res.count() + " entries.")
    /*val key = new TileIdWritable()

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
    }
    finally {
      writers.foreach(_.close)
    }
    logInfo(s"Done saving ${tiles.length} tiles")*/
  }
}