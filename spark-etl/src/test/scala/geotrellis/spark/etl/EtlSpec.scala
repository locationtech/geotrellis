package geotrellis.spark.etl

import geotrellis.proj4.WebMercator
import geotrellis.raster.{CellSize, CellType}
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark.etl.config._
import geotrellis.vector.Extent
import org.apache.spark.storage.StorageLevel
import org.scalatest._

object EtlSpec {
  // Test that ETL module can be instantiated in convenient ways
  val credentials = Credentials(
    accumulo  = List(Accumulo("name", "instance", "zookeepers", "user", "password")),
    cassandra = List(Cassandra("name", "hosts", "user", "password")),
    s3        = List(),
    hadoop    = List()
  )

  val config = Config(
    name = "test",
    cache = Some(StorageLevel.NONE),
    ingestType = IngestType(
      format   = "geotiff",
      input    = HadoopType,
      output   = HadoopType,
      inputCredentials  = Some("inputCredentials name"),
      outputCredentials = Some("outputCredentials name")
    ),
    path = IngestPath(
      input = "input",
      output = "output"
    ),
    ingestOptions = IngestOptions(
      resampleMethod  = NearestNeighbor,
      reprojectMethod = BufferedReproject,
      keyIndexMethod  = IngestKeyIndexMethod("zorder"),
      layoutScheme    = Some("tms"),
      layoutExtent    = Some(Extent(1, 2, 3, 4)),
      crs             = Some(WebMercator),
      resolutionThreshold = Some(0.1),
      cellSize = Some(CellSize(256, 256)),
      cellType = Some(CellType.fromString("int8")),
      encoding = Some("geotiff"),
      breaks   = Some("0:ffffe5ff;0.1:f7fcb9ff;0.2:d9f0a3ff;0.3:addd8eff;0.4:78c679ff;0.5:41ab5dff;0.6:238443ff;0.7:006837ff;1:004529ff")
    )
  )

  val etlConf = new EtlConf(credentials, config :: Nil)
  val etlJobs = etlConf.getEtlJobs

  val etlJob = EtlJob(config)
  Etl(etlJob)
  Etl(etlJob, List(s3.S3Module, hadoop.HadoopModule))
}
