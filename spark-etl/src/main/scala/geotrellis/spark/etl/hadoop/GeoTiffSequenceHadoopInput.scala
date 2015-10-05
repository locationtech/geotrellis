package geotrellis.spark.etl.hadoop

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.spark._
import geotrellis.vector.ProjectedExtent
import geotrellis.spark.ingest._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class GeoTiffSequenceHadoopInput extends HadoopInput[ProjectedExtent, SpatialKey] {
  val format = "geotiff-sequence"
  def source(props: Parameters)(implicit sc: SparkContext): RDD[(ProjectedExtent, V)] =
    sc
      .sequenceFile[String, Array[Byte]](props("path"))
      .map { case (path, bytes) =>
        val geotiff = GeoTiffReader.readSingleBand(bytes)
        (ProjectedExtent(geotiff.extent, geotiff.crs), geotiff.tile)
      }
}

