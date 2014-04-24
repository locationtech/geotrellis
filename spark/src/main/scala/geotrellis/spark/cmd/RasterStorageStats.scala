package geotrellis.spark.cmd
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.utils.SparkUtils
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import com.quantifind.sumac.ArgMain
import geotrellis.spark.cmd.args.RasterArgs

/*
 * Utility for printing storage statistics for a Raster, and for verifying every tile
 * belongs in its partition. 
 * 
 * RasterStorageStats --input <path-to-raster>
 */
object RasterStorageStats extends ArgMain[RasterArgs] {
  
  // Number of stats we're tracking. If this doesn't match with the number of stats 
  // we're tracking, we throw an assertion error below
  final val NUM_STATS = 4
  
  def main(args: RasterArgs): Unit = {
    val inputRasterPath = new Path(args.inputraster) 
    val partDirGlob = new Path(inputRasterPath, "part*")
    val conf = SparkUtils.createHadoopConfiguration
    val fs = inputRasterPath.getFileSystem(conf)
    val partDirs = fs.globStatus(partDirGlob).map(_.getPath())

    val partitioner = TileIdPartitioner(inputRasterPath, conf)

    val stats = partDirs.zipWithIndex.map({
      case (partDir, index) =>
        val dataFile = new Path(partDir, "data")
        val dataReader = new SequenceFile.Reader(fs, dataFile, conf)
        val isCompressed = dataReader.isCompressed()
        dataReader.close()
        
        val dataFileSt = fs.getFileStatus(dataFile)
        
        val blockSize = dataFileSt.getBlockSize()
        val bytes = dataFileSt.getLen()
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
        println(s"File ${partDir.getName()}: tiles=${tiles} len=${bytes} blocks=${blocks} blockSize=${blockSize} isCompressed=${isCompressed}")
        (index, tiles, blocks, bytes)
    })

    assert(stats.head.productArity == NUM_STATS)
    
    // takes a column index and computes various aggregates on that column
    def agg(index: Int) = {
      assert(index >= 2 && index <= NUM_STATS) 
      val stat = stats.map{t => 
        (t._1, index match {
          case 2 => t._2
          case 3 => t._3
          case 4 => t._4     
        })
      }
      val min = stat.reduceLeft { (t1, t2) => if (t1._2 < t2._2) t1 else t2 }
      val max = stat.reduceLeft { (t1, t2) => if (t1._2 > t2._2) t1 else t2 }
      val tot = stat.unzip._2.sum
      val avg = tot / stats.length.toFloat
      Map("min" -> min, "max" -> max, "tot" -> tot, "avg" -> avg)
    }
    val tileAgg = agg(2)
    val blockAgg = agg(3)
    val lenAgg = agg(4)
    val zeroTileFiles = stats.filter(_._2 == 0).length
    
    println(s"Stats (min,max,tot,avg): ")
    println(s"Tiles (${tileAgg("min")}, ${tileAgg("max")}, ${tileAgg("tot")}, ${tileAgg("avg")})")
    println(s"Blocks (${blockAgg("min")}, ${blockAgg("max")}, ${blockAgg("tot")}, ${blockAgg("avg")})")
    println(s"Bytes (${lenAgg("min")}, ${lenAgg("max")}, ${lenAgg("tot")}, ${lenAgg("avg")})")
    println(s"${zeroTileFiles} files have zero tiles")
  }
}