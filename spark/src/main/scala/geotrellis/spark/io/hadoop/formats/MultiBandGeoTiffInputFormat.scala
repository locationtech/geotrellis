package geotrellis.spark.io.hadoop.formats

import geotrellis.spark.io.hadoop._
import geotrellis.spark.ingest._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.TaskAttemptContext

class MultibandGeoTiffInputFormat extends BinaryFileInputFormat[ProjectedExtent, MultibandTile] {
  def read(bytes: Array[Byte], context: TaskAttemptContext): (ProjectedExtent, MultibandTile) = {
    val gt = MultibandGeoTiff(bytes)
    (ProjectedExtent(gt.extent, gt.crs), gt.tile)
  }
}
