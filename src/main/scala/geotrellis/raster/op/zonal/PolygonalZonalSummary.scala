package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis.raster.op.tiles._
import geotrellis.feature._
import geotrellis.feature.op.geometry._
import geotrellis.feature.rasterize._
import geotrellis.data._
import geotrellis.statistics._
import geotrellis.raster.IntConstant
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData
import geotrellis.logic._
import geotrellis.{ op => liftOp }
import com.vividsolutions.jts.{ geom => jts }

trait TiledPolygonalZonalSummary[C] extends ThroughputLimitedReducer1[C] {

  type D
  val zonePolygon: Op[Polygon[D]]

  implicit val mB:Manifest[B]
  implicit val mD:Manifest[D]

  /**
   * Compute the intermediate product of a given raster
   * tile and a polygon
   *
   * @param r A raster tile
   * @param p Polygon that intersects r's extent
   * @return intermediate product
   */
  def handlePartialTileIntersection(r: Op[Raster], p: Op[Geometry[D]]):Op[B]

  /**
   * Compute an intermediate product of a given raster tile
   * (given that the entire raster is in the zone)
   *
   * @param r A tile contained fully in the zone
   * @return intermediate product
   */
  def handleFullTile(r: Op[Raster]):Op[B]

  /**
   * Value to use for a raster that contains only
   * no data cells
   */
  def handleNoDataTile():Op[B]

  /**
   * Value to use for tiles that are completely
   * outside of the zone
   */
  def handleDisjointTile():Op[B]  
  
  def mapper(rasterOp: Op[Raster]):Op[List[B]] = 
    raster.op.Force(rasterOp).flatMap(
      strictRaster => 
        strictRaster data match {
          case x: IntConstant if x.n == NODATA => {
            AsList(handleNoDataTile())
          }
          case rdata: ArrayRasterData => {
            val tileExtent = AsFeature(GetExtent(strictRaster))
            val intersections = AsPolygonSet(Intersect(zonePolygon, tileExtent))
            val handlePartialTile = handlePartialTileWithIntersections(strictRaster, intersections)

            If( Contains(zonePolygon, tileExtent),
               AsList(handleFullTile(strictRaster)),
               If( Disjoint(zonePolygon, tileExtent),
                  AsList(handleDisjointTile), 
                  handlePartialTile))
              }
        })

  def handlePartialTileWithIntersections(
    r: Op[Raster], p: Op[List[Geometry[D]]]):Op[List[B]] =
    p.flatMap(
      polygons =>
        Collect(polygons.map(
          polygon => 
            handlePartialTileIntersection(r, polygon))).map(_.toList))
  
}    
