package geotrellis.spark.etl.hadoop

import geotrellis.raster.MultiBandTile
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.ProjectedExtent
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class MultibandGeoTiffSequenceHadoopInput extends HadoopInput[ProjectedExtent, MultiBandTile] {
  val format = "multiband-geotiff-sequence"
  def apply(props: Parameters)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultiBandTile)] =
    sc
      .sequenceFile[String, Array[Byte]](props("path"))
      .map { case (path, bytes) =>
        val geotiff = GeoTiffReader.readMultiBand(bytes)
        (ProjectedExtent(geotiff.extent, geotiff.crs), geotiff.tile)
      }
}
