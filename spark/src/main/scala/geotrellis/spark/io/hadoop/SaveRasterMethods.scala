package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.hadoop.formats._

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapred.SequenceFileOutputFormat
import org.apache.hadoop.mapred.MapFileOutputFormat
import org.apache.hadoop.mapred.JobConf
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

abstract class SaveRasterMthods[K: HadoopWritable] extends Logging {
  val rdd: RasterRDD[K]

  def saveAsHadoopRasterRDD(layerId: LayerId, path: String): Unit =
    saveAsHadoopRasterRDD(layerId, new Path(path))

  def saveAsHadoopRasterRDD(layerId: LayerId, path: Path) = {
    val keyWritable = implicitly[HadoopWritable[K]]
    import keyWritable.implicits._

    val conf = rdd.context.hadoopConfiguration
    val jobConf = new JobConf(conf)

    jobConf.set("io.map.index.interval", "1")
    SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)

    val pathString = path.toUri.toString

    logInfo("Saving RasterRDD to ${path.toUri.toString} out...")

    rdd
      .map { case (key, tile) => (key.toWritable, TileWritable(tile)) }
      .sortByKey()
      .saveAsHadoopFile(
        pathString,
        implicitly[ClassTag[keyWritable.Writable]].runtimeClass,
        classOf[TileWritable],
        classOf[MapFileOutputFormat],
        jobConf
      )

    logInfo(s"Finished saving tiles to ${path}")
    logInfo(s"Saving metadata..")

    HadoopUtils.writeLayerMetaData(LayerMetaData(layerId, rdd.metaData), path, rdd.context.hadoopConfiguration)

    logInfo(s"Finished saving ${path}")
  }
}
