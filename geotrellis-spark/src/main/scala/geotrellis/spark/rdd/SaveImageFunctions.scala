package geotrellis.spark.rdd

import scala.reflect.ClassTag
import org.apache.hadoop.io.Writable
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.MapFileOutputFormat
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.rdd.RDD
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import org.apache.hadoop.mapred.SequenceFileOutputFormat
import org.apache.hadoop.io.SequenceFile

class SaveImageFunctions[K <% TileIdWritable: ClassTag, V <% ArgWritable: ClassTag](self: RDD[(K, V)]) {
  def save(path: String) = {
    println("Saving image out...")
    val jobConf = new JobConf(self.context.hadoopConfiguration)
    jobConf.set("io.map.index.interval", "1");
    SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)
    self.saveAsHadoopFile(path, classOf[TileIdWritable], classOf[ArgWritable], classOf[MapFileOutputFormat], jobConf)
    println("End saving image out...")

  }
}