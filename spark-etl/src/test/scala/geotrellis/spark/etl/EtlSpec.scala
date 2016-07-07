package geotrellis.spark.etl

import geotrellis.raster.{CellSize, CellType}
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark.etl.config._
import geotrellis.vector.Extent
import org.apache.spark.storage.StorageLevel
import org.scalatest._

object EtlSpec {
  // Test that ETL module can be instantiated in convenient ways
  val credentials = Credentials(
    accumulo = List(Accumulo("name", "instance", "zookeepers", "user", "password")),
    cassandra = List(Cassandra("name", "hosts", "user", "password")),
    s3 = List(),
    hadoop = List()
  )

  val input = Input(
    name = "test",
    cache = Some(StorageLevel.NONE),
    ingestType = IngestType(
      format = "geotiff",
      input = HadoopType
    ),
    path = "input",
    ingestOptions = IngestOptions(
      resampleMethod = NearestNeighbor,
      reprojectMethod = BufferedReproject,
      keyIndexMethod = IngestKeyIndexMethod("zorder"),
      layoutScheme = Some("zoomed"),
      layoutExtent = Some(Extent(1, 2, 3, 4)),
      crs = Some("EPSG:3857"),
      resolutionThreshold = Some(0.1),
      cellSize = Some(CellSize(256, 256)),
      cellType = Some(CellType.fromString("int8")),
      encoding = Some("geotiff"),
      breaks = Some("0:ffffe5ff;0.1:f7fcb9ff;0.2:d9f0a3ff;0.3:addd8eff;0.4:78c679ff;0.5:41ab5dff;0.6:238443ff;0.7:006837ff;1:004529ff")
    )
  )

  val output = Output(
    ingestOutputType = IngestOutputType(
      output = AccumuloType,
      credentials = Some("name")
    ),
    path = "output"
  )

  val etlConf = new EtlConf(credentials, input :: Nil, output)
  val etlJobs = etlConf.getEtlJobs

  val etlJob = EtlJob(input, output)
  Etl(etlJob)
  Etl(etlJob, List(s3.S3Module, hadoop.HadoopModule))
}
