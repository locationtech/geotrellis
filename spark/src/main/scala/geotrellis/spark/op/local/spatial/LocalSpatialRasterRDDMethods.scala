package geotrellis.spark.op.local.spatial

import geotrellis.raster.rasterize.Rasterizer
import geotrellis.raster.{RasterExtent, ArrayTile, TileExtents}
import geotrellis.spark._
import geotrellis.vector.Geometry

trait LocalSpatialRasterRDDMethods[K <: SpatialKey] extends RasterRDDMethods[K] with Serializable {

  /** Masks this raster by the given Geometry. */
  def mask(geom: Geometry): RasterRDD[K] = {
    mask(Seq(geom))
  }

  /** Masks this raster by the given Geometry. */
  def mask(geoms: Iterable[Geometry]): RasterRDD[K] = {

    val rasterExtent = rasterRDD.metaData.rasterExtent
    val tileLayout = rasterRDD.metaData.tileLayout
    val tileExtents = TileExtents(rasterExtent.extent, tileLayout)

    rasterRDD.mapPairs { case pair @ (SpatialKey(col, row), tile) =>

      val re = RasterExtent(tileExtents(col, row), rasterExtent.cols, rasterExtent.rows)
      val result = ArrayTile.empty(tile.cellType, tileLayout.tileCols, tileLayout.tileRows)

      for (g <- geoms) {

        Rasterizer.foreachCellByGeometry(g, re) {
          if (tile.cellType.isFloatingPoint)
            (col: Int, row: Int) => result.setDouble(col, row, tile.getDouble(col, row))
          else
            (col: Int, row: Int) => result.set(col, row, tile.get(col, row))
        }
      }
      (pair._1, result)
    }
  }

}
