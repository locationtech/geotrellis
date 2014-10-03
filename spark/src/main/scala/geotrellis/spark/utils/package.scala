package geotrellis.spark

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat

import scala.util.{Failure, Success, Try}

package object utils {
  implicit class HadoopConfigurationWrapper(config: Configuration) {
    def withInputPath(path: Path): Configuration = {
      val job = Job.getInstance(config)
      FileInputFormat.addInputPath(job, path)
      job.getConfiguration
    }

    /** Creates a Configuration with all files in a directory (recursively searched)*/
    def withInputDirectory(path: Path): Configuration = {
      val allFiles = HdfsUtils.listFiles(path, config)
      HdfsUtils.putFilesInConf(allFiles.mkString(","), config)
    }
  }

  implicit class TryOption[T](option: Option[T]) {
    def mapNone(exception: => Throwable) = option match {
      case Some(t) => Success(t)
      case None    => Failure(exception)
    }
  }
}
