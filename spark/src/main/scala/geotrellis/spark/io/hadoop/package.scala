package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.avro.codecs._
import geotrellis.raster._
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce._

import scala.reflect._

package object hadoop {
  implicit def stringToPath(path: String): Path = new Path(path)

  class SpatialKeyWritable() extends AvroKeyWritable[SpatialKey, SpatialKeyWritable]
  class SpaceTimeKeyWritable() extends AvroKeyWritable[SpaceTimeKey, SpaceTimeKeyWritable]
  class TileWritable() extends AvroWritable[Tile]
  class MultibandTileWritable() extends AvroWritable[MultibandTile]

  class SpatialFilterMapFileInputFormat extends FilterMapFileInputFormat[SpatialKey, SpatialKeyWritable, TileWritable]
  class SpaceTimeFilterMapFileInputFormat extends FilterMapFileInputFormat[SpaceTimeKey, SpaceTimeKeyWritable, TileWritable]
  class SpatialMultibandFilterMapFileInputFormat extends FilterMapFileInputFormat[SpatialKey, SpatialKeyWritable, MultibandTileWritable]
  class SpaceTimeMultibandFilterMapFileInputFormat extends FilterMapFileInputFormat[SpaceTimeKey, SpaceTimeKeyWritable, MultibandTileWritable]

  implicit def spatialHadoopFormat =
    HadoopFormat.Aux[SpatialKey, Tile, SpatialKeyWritable, TileWritable, SpatialFilterMapFileInputFormat]
  implicit def spaceTimeHadoopFormat =
    HadoopFormat.Aux[SpaceTimeKey, Tile, SpaceTimeKeyWritable, TileWritable, SpaceTimeFilterMapFileInputFormat]
  implicit def spatialMultibandHadoopFormat =
    HadoopFormat.Aux[SpatialKey, MultibandTile, SpatialKeyWritable, MultibandTileWritable, SpatialMultibandFilterMapFileInputFormat]
  implicit def spaceTimeMultibandHadoopFormat =
    HadoopFormat.Aux[SpaceTimeKey, MultibandTile, SpaceTimeKeyWritable, MultibandTileWritable, SpaceTimeMultibandFilterMapFileInputFormat]

  implicit class HadoopSparkContextMethodsWrapper(val sc: SparkContext) extends HadoopSparkContextMethods

  implicit class withHadoopConfigurationMethods(val self: Configuration) extends MethodExtensions[Configuration] {
    def modify(f: Job => Unit): Configuration = {
      val job = Job.getInstance(self)
      f(job)
      job.getConfiguration
    }

    def withInputPath(path: Path): Configuration =
      modify(FileInputFormat.addInputPath(_, path))

    /** Creates a Configuration with all files in a directory (recursively searched)*/
    def withInputDirectory(path: Path): Configuration = {
      val allFiles = HdfsUtils.listFiles(path, self)
      if(allFiles.isEmpty) {
        sys.error(s"$path contains no files.")
      }
      HdfsUtils.putFilesInConf(allFiles.mkString(","), self)
    }

    /** Creates a configuration with a given directory, to search for all files
      * with an extension contained in the given set of extensions */
    def withInputDirectory(path: Path, extensions: Seq[String]): Configuration = {
      val searchPath = path.toString match {
        case p if extensions.exists(p.endsWith) => path
        case p =>
          val extensionsStr = extensions.mkString("{", ",", "}")
          new Path(s"$p/*$extensionsStr")
      }

      withInputDirectory(searchPath)
    }

    def setSerialized[T: ClassTag](key: String, value: T): Unit = {
      val ser = KryoSerializer.serialize(value)
      self.set(key, new String(ser.map(_.toChar)))
    }

    def getSerialized[T: ClassTag](key: String): T = {
      val s = self.get(key)
      KryoSerializer.deserialize(s.toCharArray.map(_.toByte))
    }
  }

  implicit class RDDHadoopMethods[K,V](rdd: RDD[(K,V)]) extends SaveToHadoopMethods[K, V](rdd)
}
