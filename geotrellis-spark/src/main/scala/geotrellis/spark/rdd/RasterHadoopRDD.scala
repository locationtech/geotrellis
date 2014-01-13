package geotrellis.spark.rdd

import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapred.FileInputFormat
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.SequenceFileInputFormat
import org.apache.spark.SerializableWritable
import org.apache.spark.SparkContext._
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.HadoopRDD



class RasterHadoopRDD(
  sc: SparkContext, 
  path: String, 
  broadcastedConf: Broadcast[SerializableWritable[Configuration]], 
  minSplits: Int)
  extends HadoopRDD[TileIdWritable, ArgWritable](
		  	sc,
		  	broadcastedConf,
		  	Some((jobConf: JobConf) => FileInputFormat.setInputPaths(jobConf, path)),
		  	classOf[SequenceFileInputFormat[TileIdWritable, ArgWritable]],
		  	classOf[TileIdWritable],
		  	classOf[ArgWritable],
		  	minSplits) {
  
  /*
   * Overriding the partitioner with a TileIdPartitioner 
   */
  override val partitioner = {
    val splitFile = path.stripSuffix(RasterHadoopRDD.SeqFileGlob) 
    Some(TileIdPartitioner(splitFile, sc.hadoopConfiguration))
  }
}

object RasterHadoopRDD {

  val SeqFileGlob = "/*[0-9]*/data"

  def apply(sc: SparkContext, path: String) = {
    val globbedPath = path + SeqFileGlob

    new RasterHadoopRDD(
      sc, globbedPath, sc.broadcast(new SerializableWritable(sc.hadoopConfiguration)), sc.defaultMinSplits)
  }
}