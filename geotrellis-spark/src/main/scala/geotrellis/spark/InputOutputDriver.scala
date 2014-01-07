package geotrellis.spark

import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.rdd.ImageHadoopRDD
import geotrellis.spark.utils.SparkUtils

import org.apache.spark.SparkContext._
import org.apache.spark.Logging

object InputOutputDriver extends Logging {
  def main(args: Array[String]) {
    val sparkMaster = args(0) 			// "spark://host:7077"
    val geotrellisJarSuffix = args(1) 	// /geotrellis-spark/target/scala-2.10/geotrellis-spark_2.10-0.9.0-SNAPSHOT.jar
    val nameNode = args(2) 				// hdfs://localhost:9000
    val inputImagePath = args(3)        // /geotrellis/images/argtest
    val outputImagePath = args(4)		// /geotrellis/images/argtestout
    val sc = SparkUtils.createSparkContext(sparkMaster, "InputOutputImage", geotrellisJarSuffix)
    val awtestRdd = ImageHadoopRDD(sc, nameNode + inputImagePath)

    def printTileWithPartition(idx: Int, itr: Iterator[(TileIdWritable, ArgWritable)]) = {
      itr.foreach(t => logInfo("Tile %d partition %d".format(t._1.get, idx)))
      itr
    }

    logInfo("sc defaultMinSplits/defaultParallelism = %d/%d".format(sc.defaultMinSplits, sc.defaultParallelism))
    logInfo("# of partitions = " + awtestRdd.getPartitions.length)
    logInfo("# of rows = " + awtestRdd.mapPartitionsWithIndex(printTileWithPartition, true).count)
    logInfo("# of matching rows = " + awtestRdd.lookup(TileIdWritable(77)).length)

    awtestRdd.save(nameNode + outputImagePath)

    sc.stop
  }
}  