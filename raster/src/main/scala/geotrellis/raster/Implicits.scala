package geotrellis.raster

import geotrellis.vector.Point
import geotrellis.macros.{ NoDataMacros, TypeConversionMacros }
import geotrellis.vector.{Geometry, Feature}
import geotrellis.util.MethodExtensions


object Implicits extends Implicits

trait Implicits
    extends costdistance.Implicits
    with crop.Implicits
    with density.Implicits
    with distance.Implicits
    with equalization.Implicits
    with hydrology.Implicits
    with interpolation.Implicits
    with mapalgebra.focal.Implicits
    with mapalgebra.focal.hillshade.Implicits
    with mapalgebra.local.Implicits
    with mapalgebra.zonal.Implicits
    with mask.Implicits
    with matching.Implicits
    with merge.Implicits
    with prototype.Implicits
    with rasterize.Implicits
    with regiongroup.Implicits
    with render.Implicits
    with reproject.Implicits
    with resample.Implicits
    with split.Implicits
    with summary.polygonal.Implicits
    with transform.Implicits {

  // Implicit method extension for core types

  implicit class withTileMethods(val self: Tile) extends MethodExtensions[Tile]
      with DelayedConversionTileMethods
      with merge.SinglebandTileMergeMethods
      with sigmoidal.SinglebandSigmoidalMethods
      with split.SinglebandTileSplitMethods
      with summary.SinglebandTileSummaryMethods
      with vectorize.VectorizeMethods
      with viewshed.ViewshedMethods

  implicit class withMultibandTileMethods(val self: MultibandTile) extends MethodExtensions[MultibandTile]
      with DelayedConversionMultibandTileMethods
      with merge.MultibandTileMergeMethods
      with sigmoidal.MultibandSigmoidalMethods
      with split.MultibandTileSplitMethods
      with summary.MultibandTileSummaryMethods

  implicit class withSinglebandRasterMethods(val self: SinglebandRaster) extends MethodExtensions[SinglebandRaster]
      with vectorize.SinglebandRasterVectorizeMethods

  implicit class SinglebandRasterAnyRefMethods(val self: SinglebandRaster) extends AnyRef {
    def getValueAtPoint(point: Point): Int =
      getValueAtPoint(point.x, point.y)

    def getValueAtPoint(x: Double, y: Double): Int =
      self.tile.get(
        self.rasterExtent.mapXToGrid(x),
        self.rasterExtent.mapYToGrid(y)
      )

    def getDoubleValueAtPoint(point: Point): Double =
      getDoubleValueAtPoint(point.x, point.y)

    def getDoubleValueAtPoint(x: Double, y: Double): Double =
      self.tile.getDouble(
        self.rasterExtent.mapXToGrid(x),
        self.rasterExtent.mapYToGrid(y)
      )
  }

  implicit class TraversableTileExtensions(rs: Traversable[Tile]) {
    def assertEqualDimensions(): Unit =
      if(Set(rs.map(_.dimensions)).size != 1) {
        val dimensions = rs.map(_.dimensions).toSeq
        throw new GeoAttrsError("Cannot combine tiles with different dimensions." +
          s"$dimensions are not all equal")
      }
  }

  implicit class TileTupleExtensions(t: (Tile, Tile)) {
    def assertEqualDimensions(): Unit =
      if(t._1.dimensions != t._2.dimensions) {
        throw new GeoAttrsError("Cannot combine rasters with different dimensions." +
          s"${t._1.dimensions} does not match ${t._2.dimensions}")
      }
  }
}
