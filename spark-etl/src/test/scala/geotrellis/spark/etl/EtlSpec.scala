package geotrellis.spark.etl

import geotrellis.spark.tiling.{FloatingLayoutScheme, ZoomedLayoutScheme}

object EtlSpec {
  // Test that ETL module can be instantiated in convenient ways
  val args = Seq("-options", "arguments")

  Etl(args)
  Etl(args, List(s3.S3Module, hadoop.HadoopModule))
  Etl(args, List(s3.S3Module), (crs, tileSize) => ZoomedLayoutScheme(crs, tileSize))
  Etl(args, (crs, tileSize) => ZoomedLayoutScheme(crs, tileSize))
  Etl(args, List(s3.S3Module), (_, tileSize) => FloatingLayoutScheme(tileSize))
}
