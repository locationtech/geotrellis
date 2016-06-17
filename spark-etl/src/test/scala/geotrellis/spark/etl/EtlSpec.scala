package geotrellis.spark.etl

import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark.etl.config._
import org.scalatest._

object EtlSpec {
  // Test that ETL module can be instantiated in convenient ways
  val etlJob = EtlJob(Config(
    name = "test",
    ingestType = IngestType(
      format = "geotiff",
      input  = HadoopType,
      output = HadoopType
    ),
    path = IngestPath(input = "input", output = "output"),
    ingestOptions = IngestOptions(
      resampleMethod  = NearestNeighbor,
      reprojectMethod = BufferedReproject,
      keyIndexMethod  = IngestKeyIndexMethod("zorder")
    )))

  Etl(etlJob)
  Etl(etlJob, List(s3.S3Module, hadoop.HadoopModule))
}
