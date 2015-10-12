package geotrellis.spark.etl

import geotrellis.spark.tiling.{FloatingLayoutScheme, ZoomedLayoutScheme}
import org.scalatest._

object EtlSpec {
  // Test that ETL module can be instantiated in convenient ways
  val args = Seq("-options", "arguments")

  Etl(args)
  Etl(args, List(s3.S3Module, hadoop.HadoopModule))
}