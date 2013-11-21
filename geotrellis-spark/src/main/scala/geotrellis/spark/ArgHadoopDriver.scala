package geotrellis.spark

import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.conf.Configuration
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.tiling.TmsTiling

// A simple example demonstrating reading and writing of (tileId, Array) out to sequence file 
object ArgHadoopDriver {

  def fill(value: Int) = Array.fill(TmsTiling.DefaultTileSize * TmsTiling.DefaultTileSize)(value)

  def main(args: Array[String]): Unit = {
    val seqFilePath = new Path(args(0)) // full path to sequence file
    val numTiles = args(1).toInt
    val conf = new Configuration()
    val fs = seqFilePath.getFileSystem(conf)

    val key = new TileIdWritable()
    val value = new ArgWritable()

    // first let's write a few records out
    val writer = SequenceFile.createWriter(fs, conf, seqFilePath, classOf[TileIdWritable], classOf[ArgWritable]) 
    for (i <- 0 until numTiles) {
      key.set(i);
      val array = fill(i)
      val value = ArgWritable.toWritable(array)
      writer.append(key, value);
    }
    writer.close();

    // now let's read them back in and check their key,values
    val reader = new SequenceFile.Reader(fs, seqFilePath, conf)
    var i = 0
    while (reader.next(key, value)) {
      val actual = ArgWritable.toArray(value)
      val expected = fill(i)
      if (key.get != i || !actual.sameElements(expected))
        throw new Exception("Arrays don't match for key = " + i)
      i = i + 1
    }
    reader.close()
  }

}