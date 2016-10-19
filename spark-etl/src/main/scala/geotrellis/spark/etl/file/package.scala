package geotrellis.spark.etl

import geotrellis.spark.etl.config.{Backend, HadoopPath}

package object file {
  def getPath(b: Backend): HadoopPath =
    b.path match {
      case p: HadoopPath => p
      case _ => throw new Exception("Path string not corresponds backend type")
    }
}
