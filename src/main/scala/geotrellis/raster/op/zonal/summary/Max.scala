package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.data._
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData

object Max {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    trd.getTiles(re)
       .map { r => (r.rasterExtent, maxRaster(r))}
       .toMap
  }

  def maxRaster (r:Raster):Int = {
    var max = Int.MinValue
    for(z <- r) { if (z > max) max = z }
    max
  }
}

/**
 * Perform a zonal summary that calculates the max value of all raster cells within a geometry.
 * This operation is for integer typed Rasters. If you want the Max for double type rasters
 * (TypeFloat,TypeDouble) use [[MaxDouble]]
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
      val f = new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.get(col,row)
            if (z != NODATA && z > max) { max = z }
          }
        }

      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent)(f)
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

  def reducer(mapResults: List[Int]):Int = mapResults.foldLeft(Int.MinValue)(math.max(_, _)) 
}

object MaxDouble {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    trd.getTiles(re)
       .map { r => (r.rasterExtent, maxRaster(r))} 
       .toMap
  }

  def maxRaster (r:Raster):Double = {
    var max = Double.NegativeInfinity
    r.foreachDouble( x => if (!java.lang.Double.isNaN(x) && x > max) max = x )
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
      val f = new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.getDouble(col,row)
            if (!java.lang.Double.isNaN(z) && z > max) { max = z }
          }
        }

      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent)(f)
      max
    }))
  }

  def handleFullTile(rOp:Op[Raster]) = rOp.map (r =>
    tileResults.get(r.rasterExtent).getOrElse({
      var max = Double.NegativeInfinity
      r.force.foreachDouble((x:Double) => if (!java.lang.Double.isNaN(x) && x > max) max = x )
      max
   }))
  
 
  def handleNoDataTile = Double.NegativeInfinity

  def reducer(mapResults: List[Double]):Double = mapResults.foldLeft(Double.NegativeInfinity)(math.max(_, _)) 
}
