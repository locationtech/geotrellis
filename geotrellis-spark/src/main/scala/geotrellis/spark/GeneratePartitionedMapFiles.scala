package geotrellis.spark

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import geotrellis.TypeInt
import geotrellis.raster.IntArrayRasterData
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.tiling.TmsTiling
import scala.util.Random
import org.apache.hadoop.io.MapFile

/**
 * @author akini
 * 
 * A simple example demonstrating reading and writing of (tileId, Array) out to sequence file
 * Run command: GeneratePartitionedMapFiles [HDFS-OR-LOCAL-PATH-TO-DIR] [NUMTILES] [NUMFILES]
 *  		e.g.,	GeneratePartitionedMapFiles file:///tmp/argtest/ 100 5 
 * This would give 5 partitions, each having 20 tiles
 * 
 * 
 */
object GeneratePartitionedMapFiles {
  val defaultTileSize = TmsTiling.DefaultTileSize
  
  def fill(value: Int) = {
    //val arr = Array.fill(defaultTileSize * defaultTileSize)(value)
    val arr = (Seq.fill(defaultTileSize * defaultTileSize)(Random.nextInt)).toArray
    IntArrayRasterData(arr, defaultTileSize, defaultTileSize)
  }

  def main(args: Array[String]): Unit = {
    val dirPath = new Path(args(0)) // full path to directory
    val numTiles = args(1).toInt
    val numFiles = args(2).toInt
    
    val tilesPerFile = numTiles / numFiles
    val conf = new Configuration()
    val fs = dirPath.getFileSystem(conf)

    val key = new TileIdWritable()

    // first let's write a few records out

    
    var writer: Option[MapFile.Writer] = None

    for (i <- 0 until numTiles) {
      if(i % tilesPerFile == 0) {
    	writer.foreach(_.close)  
        val mapFilePath = new Path(dirPath, i.toString)
        println("writing to " + mapFilePath)
        writer = Some(new MapFile.Writer(conf, fs, mapFilePath.toUri.toString, 
        							classOf[TileIdWritable], classOf[ArgWritable],
    								SequenceFile.CompressionType.RECORD))         
      }
      key.set(i)
      val array = fill(i)
      val value = ArgWritable.toWritable(array)
      writer.foreach(_.append(key, value))
    }
    writer.foreach(_.close)  
  }

}