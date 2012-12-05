package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.data._
//import geotrellis.statistics._
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData

object Max {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    val tiles = trd.getTileList(re)
    tiles map { r => (r.rasterExtent, maxRaster(r))} toMap
  }

  def maxRaster (r:Raster):Int = {
    var max = Int.MinValue
    r.foreach( (x) => if (x != NODATA && x > max) max = x )
    max
  }
}

/**
 * Perform a zonal summary that calculates the max value of all raster cells within a geometry.
 *
 * @param   r             Raster to summarize
 * @param   zonePolygon   Polygon that defines the zone
 * @param   tileResults   Cached results of full tiles created by createTileResults
 */
case class Max[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,Int]) 
  (implicit val mB: Manifest[Int], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Int] {
 
  type B = Int
  type D = DD
  
  def handlePartialTileIntersection(rOp: Op[Raster], gOp: Op[Geometry[D]]) = {
    rOp.flatMap ( r => gOp.flatMap ( g => {
      var max = Int.MinValue
      val f = (col:Int, row:Int, g:Geometry[_]) => {
        val z = r.get(col,row)
        if (z != NODATA && z > max) { max = z }
      }
      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent,
        f)
      max
    }))
  }

  def handleFullTile(rOp:Op[Raster]) = rOp.map (r =>
    tileResults.get(r.rasterExtent).getOrElse({
      var max = Int.MinValue
      r.force.foreach((x:Int) => if (x != NODATA && x > max) max = x )
      max
   }))
  
 
  def handleNoDataTile = Int.MinValue
  def handleDisjointTile = Int.MinValue

  def reducer(mapResults: List[Int]):Int = mapResults.foldLeft(Int.MinValue)(math.max(_, _)) 
}

object MaxDouble {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    val tiles = trd.getTileList(re)
    tiles map { r => (r.rasterExtent, maxRaster(r))} toMap
  }

  def maxRaster (r:Raster):Double = {
    var max = Double.NegativeInfinity
    r.foreach( (x) => if (x != NODATA && x > max) max = x )
    max
  }
}

/**
 * Perform a zonal summary that calculates the max value of all raster cells within a geometry.
 *
 * @param   r             Raster to summarize
 * @param   zonePolygon   Polygon that defines the zone
 * @param   tileResults   Cached results of full tiles created by createTileResults
 */
case class MaxDouble[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,Double]) 
  (implicit val mB: Manifest[Double], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Double] {
 
  type B = Double
  type D = DD
  
  def handlePartialTileIntersection(rOp: Op[Raster], gOp: Op[Geometry[D]]) = {
    rOp.flatMap ( r => gOp.flatMap ( g => {
      var max = Double.NegativeInfinity
      val f = (col:Int, row:Int, g:Geometry[_]) => {
        val z = r.get(col,row)
        if (z != NODATA && z > max) { max = z }
      }
      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent,
        f)
      max
    }))
  }

  def handleFullTile(rOp:Op[Raster]) = rOp.map (r =>
    tileResults.get(r.rasterExtent).getOrElse({
      var max = Double.NegativeInfinity
      r.force.foreach((x:Int) => if (x != NODATA && x > max) max = x )
      max
   }))
  
 
  def handleNoDataTile = Double.NegativeInfinity
  def handleDisjointTile = Double.NegativeInfinity

  def reducer(mapResults: List[Double]):Double = mapResults.foldLeft(Double.NegativeInfinity)(math.max(_, _)) 
}


