package geotrellis.spark.etl

import geotrellis.raster.{CellSize, CellType}
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark.etl.config._
import geotrellis.vector.Extent
import org.apache.spark.storage.StorageLevel
import org.scalatest._

object EtlSpec {
  // Test that ETL module can be instantiated in convenient ways
  val profiles = List(
    AccumuloProfile("accumulo-name", "instance", "zookeepers", "user", "password"),
    CassandraProfile("name", "hosts", "user", "password"),
    HBaseProfile("hbase-name", "zookeepers", "master")
  )

  val input = Input(
    name = "test",
    format = "geotiff",
    cache = Some(StorageLevel.NONE),
    noData = Some(0d),
    backend = Backend(
      `type`  = HadoopType,
      profile = None,
      path    = HadoopPath("path")
    )
  )

  val output =
    Output(
      backend = Backend(
        `type` = AccumuloType,
        profile = Some(profiles.head),
        path = AccumuloPath("output")
      ),
      resampleMethod = NearestNeighbor,
      reprojectMethod = BufferedReproject,
      keyIndexMethod = IngestKeyIndexMethod("zorder"),
      tileSize = 256,
      pyramid = true,
      partitions = Some(100),
      layoutScheme = Some("zoomed"),
      layoutExtent = Some(Extent(1.2, 2.3, 3.4, 4.5)),
      crs = Some("EPSG:3857"),
      resolutionThreshold = Some(0.1),
      cellSize = Some(CellSize(256.2, 256.1)),
      cellType = Some(CellType.fromString("int8")),
      encoding = Some("geotiff"),
      breaks = Some("0:ffffe5ff;0.1:f7fcb9ff;0.2:d9f0a3ff;0.3:addd8eff;0.4:78c679ff;0.5:41ab5dff;0.6:238443ff;0.7:006837ff;1:004529ff"),
      maxZoom = Some(13)
    )

  val etlConf = new EtlConf(
    input  = input,
    output = output,
    outputProfile = Some(profiles.head)
  )

  Etl(etlConf)
  Etl(etlConf, List(s3.S3Module, hadoop.HadoopModule))
}
