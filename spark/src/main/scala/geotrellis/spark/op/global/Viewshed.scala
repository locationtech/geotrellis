package geotrellis.spark.op.global

import geotrellis.spark._

import reflect.ClassTag

object Viewshed {

  def apply[K](rasterRDD: RasterRDD[K], startCol: Int, startRow: Int)
    (implicit keyClassTag: ClassTag[K], _sc: SpatialComponent[K]): RasterRDD[K] = {
    ???
  }

  private def offsets[K](
    rasterRDD: RasterRDD[K],
    startCol: Int,
    startRow: Int): RasterRDD[K] = {
    val metaData = rasterRDD.metaData

    val gridBounds = metaData.gridBounds
    val (layoutCols, layoutRows) = (gridBounds.width - 1, gridBounds.height - 1)

    val tileLayout = metaData.tileLayout
    val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)



    ???
  }

}
