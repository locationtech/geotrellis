package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.vector.ProjectedExtent
import geotrellis.spark.ingest._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class GeoTiffSequenceHadoopInput extends HadoopInput[ProjectedExtent, Tile] {
  val format = "geotiff-sequence"
  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    sc
      .sequenceFile[String, Array[Byte]](getPath(conf.input.backend).path)
      .map { case (path, bytes) =>
        val geotiff = GeoTiffReader.readSingleband(bytes)
        (ProjectedExtent(geotiff.extent, geotiff.crs), geotiff.tile)
      }
}

