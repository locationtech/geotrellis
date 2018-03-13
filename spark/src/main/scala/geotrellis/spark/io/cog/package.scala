package geotrellis.spark.io

import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.vector.Extent
import geotrellis.raster.{CellGrid, MultibandTile}
import geotrellis.raster.io.geotiff.GeoTiff

import org.apache.spark.util.AccumulatorV2
import java.util

import scala.reflect.{ClassTag, classTag}

package object cog extends Implicits {
  type MetadataAccumulator[M] = AccumulatorV2[(Int, M), util.Map[Int, M]]

  val GTKey     = "GT_KEY"
  val Extension = "tiff"

  implicit class withExtentMethods(extent: Extent) {
    def bufferByLayout(layout: LayoutDefinition): Extent =
      Extent(
        extent.xmin + layout.cellwidth / 2,
        extent.ymin + layout.cellheight / 2,
        extent.xmax - layout.cellwidth / 2,
        extent.ymax - layout.cellheight / 2
      )
  }

  /** Bands number information should be encapsulated into layer metadata */
  def geoTiffBandsCount[V <: CellGrid: ClassTag](tiff: GeoTiff[V]) = {
    if(classTag[V].runtimeClass.isAssignableFrom(classTag[MultibandTile].runtimeClass))
      tiff.tile.asInstanceOf[MultibandTile].bandCount
    else 1
  }
}
