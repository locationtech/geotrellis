package geotrellis.spark

import geotrellis.spark.rdd.LoadImageRDD
import geotrellis.spark.utils.GeotrellisSparkUtils
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable

object InputOutputDriver {
  def main(args: Array[String]) {
    val sparkMaster = args(0) 			// "spark://host:7077"
    val geotrellisJarSuffix = args(1) 	// /geotrellis-spark/target/scala-2.10/geotrellis-spark_2.10-0.9.0-SNAPSHOT.jar
    val nameNode = args(2) 				// hdfs://localhost:9000
    val inputImagePath = args(3)        // /geotrellis/images/argtest
    val outputImagePath = args(4)		// /geotrellis/images/argtestout
    val sc = GeotrellisSparkUtils.createSparkContext(sparkMaster, "InputOutputImage", geotrellisJarSuffix)
    val awtestRdd = LoadImageRDD(sc, nameNode + inputImagePath)

    def printTileWithPartition(idx: Int, itr: Iterator[(TileIdWritable, ArgWritable)]) = {
      itr.foreach(t => println("Tile %d partition %d".format(t._1.get, idx)))
      itr
    }

    println("sc defaultMinSplits/defaultParallelism = %d/%d".format(sc.defaultMinSplits, sc.defaultParallelism))
    println("# of partitions = " + awtestRdd.getPartitions.length)
    println("# of rows = " + awtestRdd.mapPartitionsWithIndex(printTileWithPartition, true).count)

    awtestRdd.save(nameNode + outputImagePath)

    sc.stop
  }
}  