package geotrellis.spark.rdd

import org.apache.spark.rdd.HadoopFileRDD
import org.apache.spark.rdd.HadoopRDD
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.SerializableWritable
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapred.SequenceFileInputFormat
import geotrellis.spark.utils.GeotrellisSparkUtils

class LoadImageRDD(
  sc: SparkContext,
  path: String,
  broadcastedConf: Broadcast[SerializableWritable[Configuration]],
  minSplits: Int)
  extends HadoopFileRDD[TileIdWritable, ArgWritable](
    sc,
    path,
    broadcastedConf,
    classOf[SequenceFileInputFormat[TileIdWritable, ArgWritable]],
    classOf[TileIdWritable],
    classOf[ArgWritable], minSplits) {

}

object LoadImageRDD {

  def apply(sc: SparkContext, path: String) = {
    val globbedPath = path + "/*/data"
    new LoadImageRDD(
      sc, globbedPath, sc.broadcast(new SerializableWritable(sc.hadoopConfiguration)), sc.defaultMinSplits)
  }

  def main(args: Array[String]): Unit = {
    val sparkMaster = "spark://karadi:7077"
    val dependencyJarSuffix = "/geotrellis-spark/target/scala-2.10/geotrellis-spark_2.10-0.9.0-SNAPSHOT.jar"
    val nameNode = "hdfs://localhost:9000"

    val sc = GeotrellisSparkUtils.createSparkContext(sparkMaster, "LoadImage", dependencyJarSuffix)
    val imagePath = "/geotrellis/images/argtest"
    val awtestRdd = LoadImageRDD(sc, nameNode + imagePath)
    //val awtestRdd = sc.sequenceFile(nameNode + imagePath, classOf[TileIdWritable], classOf[ArgWritable])
    awtestRdd.foreach { case (tile,_) => println(tile.get) }
    println("# of partitions = " + awtestRdd.getPartitions.length)
    sc.stop

  }
}