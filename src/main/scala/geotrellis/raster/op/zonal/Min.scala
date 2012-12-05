package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.data._
//import geotrellis.statistics._
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData

object Min {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    val tiles = trd.getTileList(re)
    tiles map { r => (r.rasterExtent, minRaster(r))} toMap
  }

  def minRaster (r:Raster):Int = {
    var min = Int.MaxValue
    r.foreach( (x) => if (x != NODATA && x < min) min = x )
    min
  }
}

/**
 * Perform a zonal summary that calculates the sum of all raster cells within a geometry.
 *
 * @param   r             Raster to summarize
 * @param   zonePolygon   Polygon that defines the zone
 * @param   tileResults   Cached results of full tiles created by createTileResults
 */
case class Min[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,Int]) 
  (implicit val mB: Manifest[Int], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Int] {
 
  type B = Int
  type D = DD
  
  def handlePartialTileIntersection(rOp: Op[Raster], gOp: Op[Geometry[D]]) = {
    rOp.flatMap ( r => gOp.flatMap ( g => {
      var min = Int.MaxValue
      val f = (col:Int, row:Int, g:Geometry[_]) => {
        val z = r.get(col,row)
        if (z != NODATA && z < min) { min = z }
      }
      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent,
        f)
      min
    }))
  }

  def handleFullTile(rOp:Op[Raster]) = rOp.map (r =>
    tileResults.get(r.rasterExtent).getOrElse({
      var min = Int.MaxValue
      r.force.foreach((x:Int) => if (x != NODATA && x < min) min = x )
      min
   }))
  
 
  def handleNoDataTile = Int.MaxValue
  def handleDisjointTile = Int.MaxValue

  def reducer(mapResults: List[Int]):Int = mapResults.foldLeft(Int.MaxValue)(math.min(_, _)) 
}


