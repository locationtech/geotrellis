package geotrellis.spark.rdd

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.spark.tiling._

case class LayerMetaData(cellType: CellType, extent: Extent, zoomLevel: ZoomLevel) {
  override
  def hashCode = 
    (cellType, extent, zoomLevel.level).hashCode

  override
  def equals(o: Any): Boolean = 
    o match {
      case other: LayerMetaData =>
        (other.cellType, other.extent, other.zoomLevel.level).equals(
          (cellType, extent, zoomLevel.level)
        )
      case _ => false
    }
}
