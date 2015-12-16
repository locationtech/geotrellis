package geotrellis.spark.io.hadoop.formats

import geotrellis.spark.io.hadoop._
import geotrellis.spark.ingest._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.TaskAttemptContext

class MultiBandGeoTiffInputFormat extends BinaryFileInputFormat[ProjectedExtent, MultiBandTile] {
  def read(bytes: Array[Byte], context: TaskAttemptContext): (ProjectedExtent, MultiBandTile) = {
    val gt = MultiBandGeoTiff(bytes)
    (ProjectedExtent(gt.extent, gt.crs), gt.tile)
  }
}
