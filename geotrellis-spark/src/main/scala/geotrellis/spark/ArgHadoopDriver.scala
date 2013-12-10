package geotrellis.spark

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile

import geotrellis.TypeInt
import geotrellis.raster.IntArrayRasterData
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.tiling.TmsTiling

/**
 * @author akini
 * 
 * A simple example demonstrating reading and writing of (tileId, Array) out to sequence file
 * Run command: ArgHadoopDriver [NUMTILES] [HDFS-OR-LOCAL-PATH-TO-SEQUENCE-FILE]
 *  		e.g.,	ArgHadoopDriver file:///tmp/args.seq 100 
 * 
 * Currently the following also needs to be passed in to the VM to quell a Hadoop Configuration exception that:
 * -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl
 * 
 */
object ArgHadoopDriver {
  val defaultTileSize = TmsTiling.DefaultTileSize
  
  def fill(value: Int) = {
    val arr = Array.fill(defaultTileSize * defaultTileSize)(value)
    IntArrayRasterData(arr, defaultTileSize, defaultTileSize)
  }

  def main(args: Array[String]): Unit = {
    val seqFilePath = new Path(args(0)) // full path to sequence file
    val numTiles = args(1).toInt
    val conf = new Configuration()
    val fs = seqFilePath.getFileSystem(conf)

    val key = new TileIdWritable()

    // first let's write a few records out
    val writer = SequenceFile.createWriter(fs, conf, seqFilePath, 
    										classOf[TileIdWritable], classOf[ArgWritable],
    										SequenceFile.CompressionType.RECORD) 
    for (i <- 0 until numTiles) {
      key.set(i)
      val array = fill(i)
      val value = ArgWritable.toWritable(array)
      writer.append(key, value)
    }
    writer.close()

    // now let's read them back in and check their key,values
    val reader = new SequenceFile.Reader(fs, seqFilePath, conf)
    var i = 0
    val value = ArgWritable(defaultTileSize * defaultTileSize * TypeInt.bytes, 0)

    import scala.runtime.ScalaRunTime._
    while (reader.next(key, value)) {
      val actual = ArgWritable.toRasterData(value, TypeInt, defaultTileSize, defaultTileSize).asInstanceOf[IntArrayRasterData].array
      val expected = fill(i)
      if (key.get != i || !actual.sameElements(expected.toArray))
        throw new Exception("Arrays don't match for key = %d and i = %d, expected = %s and actual = %s".format(key.get, i, stringOf(expected.array), stringOf(actual.array)))

      i = i + 1
    }
    reader.close()
  }

}