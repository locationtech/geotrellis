package geotrellis.spark
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable

import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.fs.Path

object TestLowerBounds {
  def main(args: Array[String]): Unit = {
    val conf = SparkUtils.createHadoopConfiguration
    val filePath = new Path("/geotrellis/images/intstring.seq")
    val fs = filePath.getFileSystem(conf)
    //	  println(fs)
    //	  val writer = SequenceFile.createWriter(fs, conf, filePath,
    //        							classOf[IntWritable], classOf[IntWritable],
    //    								SequenceFile.CompressionType.NONE)
    //	  for(i <- 1 to 10) {
    //	    val iw = new IntWritable(i)
    //	    writer.append(iw, iw)
    //	  }
    //	  writer.close

    val sparkMaster = args(0) // "spark://host:7077"
    val geotrellisJarSuffix = args(1) // /geotrellis-spark/target/scala-2.10/geotrellis-spark_2.10-0.9.0-SNAPSHOT.jar
    val nameNode = args(2) // hdfs://localhost:9000
    val inputImagePath = args(3) // /geotrellis/images/argtest
    val sc = SparkUtils.createSparkContext(sparkMaster, "TestLowerBounds", geotrellisJarSuffix)
    val intStringRdd = sc.sequenceFile(inputImagePath, classOf[TileIdWritable], classOf[ArgWritable])
    //intStringRdd.foreach(t => println(s"key=${t._1} and val=${t._2}"))
    intStringRdd.save(inputImagePath)
  }
}