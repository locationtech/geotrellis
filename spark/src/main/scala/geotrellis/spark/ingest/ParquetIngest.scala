package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.cmd.args.AccumuloArgs
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.parquet._
import geotrellis.raster.io.geotiff.SingleBandGeoTiff
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import geotrellis.spark.tiling._
import geotrellis.spark.utils.SparkUtils
import geotrellis.vector._
import geotrellis.proj4._
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import geotrellis.raster.Tile

import org.apache.hadoop.fs._

import org.apache.spark._

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

import scala.reflect.ClassTag
import com.github.nscala_time.time.Imports._


class ParquetArgs extends IngestArgs {
  @Required var output: String = _
  @Required var parquetpartitions: Integer = _
}

object ParquetIngestCommand extends ArgMain[ParquetArgs] with Logging {
  def main(args: ParquetArgs): Unit = {

    implicit val sc = SparkUtils.createSparkContext("Parquet-Ingest")

    val job = sc.newJob("geotiff-ingest")

    val layoutScheme = ZoomedLayoutScheme(256)

    val sourceTiles = sc.binaryFiles(args.input).map{ case(inputKey, b) =>
      val geoTiff = SingleBandGeoTiff(b.toArray)
      val isoString = geoTiff.tags.headTags("ISO_TIME")
      val dateTime = DateTime.parse(isoString)
      (SpaceTimeInputKey(geoTiff.extent, geoTiff.crs, dateTime), geoTiff.tile)
    }.repartition(args.parquetpartitions)

    Ingest[SpaceTimeInputKey, SpaceTimeKey](sourceTiles, args.destCrs, layoutScheme){ (rdd, level) =>
      val catalog = ParquetRasterCatalog(args.output)
      val writer = catalog.writer[SpaceTimeKey](ZCurveKeyIndexMethod.byYear, true)
      writer.write(LayerId(args.layerName, level.zoom), rdd)
    }
  }
}
