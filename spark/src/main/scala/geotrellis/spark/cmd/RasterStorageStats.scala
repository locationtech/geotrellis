package geotrellis.spark.cmd
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.rdd.TileIdPartitioner

import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile

import com.quantifind.sumac.ArgMain

/*
 * Utility for printing storage statistics for a Raster, and for verifying every tile
 * belongs in its partition. 
 * 
 * RasterStorageStats --input <path-to-raster>
 */
object RasterStorageStats extends ArgMain[CommandArguments] {
  def main(args: CommandArguments): Unit = {
    val inputRasterPath = new Path(args.input) 
    val partDirGlob = new Path(inputRasterPath, "part*")
    val conf = SparkUtils.createHadoopConfiguration
    val fs = inputRasterPath.getFileSystem(conf)
    val partDirs = fs.globStatus(partDirGlob).map(_.getPath())

    val partitioner = TileIdPartitioner(inputRasterPath, conf)

    val stats = partDirs.zipWithIndex.map({
      case (partDir, index) =>
        val dataFileSt = fs.getFileStatus(new Path(partDir, "data"))
        val blockSize = dataFileSt.getBlockSize()
        val bytes = dataFileSt.getLen().toInt
        val blocks = math.ceil(bytes / blockSize.toFloat).toInt

        val indexFile = new Path(partDir, "index")
        val indexReader = new SequenceFile.Reader(fs, indexFile, conf)
        val key = new TileIdWritable()
        var tiles = 0
        while (indexReader.next(key)) {
          assert(partitioner.getPartition(key) == index)
          tiles = tiles + 1
        }
        indexReader.close()
        println(s"File ${partDir.getName()}: tiles=${tiles} len=${bytes} blocks=${blocks} blockSize=${blockSize}")
        (index, tiles, blocks, bytes)
    })

    // takes a column index and computes various aggregates on that column
    def agg(index: Int) = {
      val stat = stats.map{t =>
        val l = t.productIterator.toList.asInstanceOf[List[Int]]
        (l(0), l(index))
      }
      val min = stat.reduceLeft { (t1, t2) => if (t1._2 < t2._2) t1 else t2 }
      val max = stat.reduceLeft { (t1, t2) => if (t1._2 > t2._2) t1 else t2 }
      val tot = stat.unzip._2.sum
      val avg = tot / stats.length.toFloat
      Map("min" -> min, "max" -> max, "tot" -> tot, "avg" -> avg)
    }
    val tileAgg = agg(1)
    val blockAgg = agg(2)
    val lenAgg = agg(3)
    println(s"Stats (min,max,tot,avg): ")
    println(s"Tiles (${tileAgg("min")}, ${tileAgg("max")}, ${tileAgg("tot")}, ${tileAgg("avg")})")
    println(s"Blocks (${blockAgg("min")}, ${blockAgg("max")}, ${blockAgg("tot")}, ${blockAgg("avg")})")
    println(s"Bytes (${lenAgg("min")}, ${lenAgg("max")}, ${lenAgg("tot")}, ${lenAgg("avg")})")
    
  }
}