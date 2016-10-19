package geotrellis.spark.etl

import geotrellis.spark.etl.config.{Backend, S3Path}

package object s3 {
  def getPath(b: Backend): S3Path =
    b.path match {
      case p: S3Path => p
      case _ => throw new Exception("Path string not corresponds backend type")
    }
}
