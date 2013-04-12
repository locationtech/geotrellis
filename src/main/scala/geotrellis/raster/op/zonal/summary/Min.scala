package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.data._
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData

object Min {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    trd.getTiles(re)
       .map { r => (r.rasterExtent, minRaster(r))}
       .toMap
  }

  def minRaster (r:Raster):Int = {
    var min = Int.MaxValue
    for(z <- r) { if (z != NODATA && z < min) min = z }
    min
  }
}

/**
 * Perform a zonal summary that calculates the min of all raster cells within a geometry.
 * This operation is for integer typed Rasters. If you want the Min for double type rasters
 * (TypeFloat,TypeDouble) use [[MinDouble]]
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
      val f = new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.get(col,row)
            if (z != NODATA && z < min) { min = z }
          }
        }

      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent)(f)
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

  def reducer(mapResults: List[Int]):Int = mapResults.foldLeft(Int.MaxValue)(math.min(_, _)) 
}

object MinDouble {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    trd.getTiles(re)
       .map { r => (r.rasterExtent, minRaster(r))} 
       .toMap
  }

  def minRaster (r:Raster):Double = {
    var min = Double.PositiveInfinity
    r.foreachDouble( x => if (!java.lang.Double.isNaN(x) && x < min) min = x )
    min
  }
}

/**
 * Perform a zonal summary that calculates the min of all raster cells within a geometry.
 *
 * @param   r             Raster to summarize
 * @param   zonePolygon   Polygon that defines the zone
 * @param   tileResults   Cached results of full tiles created by createTileResults
 */
case class MinDouble[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,Double]) 
  (implicit val mB: Manifest[Double], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Double] {
 
  type B = Double
  type D = DD
  
  def handlePartialTileIntersection(rOp: Op[Raster], gOp: Op[Geometry[D]]) = {
    rOp.flatMap ( r => gOp.flatMap ( g => {
      var min = Double.PositiveInfinity
      val f = new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.getDouble(col,row)
            if (!java.lang.Double.isNaN(z) && z < min) { min = z }
          }
        }

      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent)(f)
      min
    }))
  }

  def handleFullTile(rOp:Op[Raster]) = rOp.map (r =>
    tileResults.get(r.rasterExtent).getOrElse({
      var min = Double.PositiveInfinity
      r.force.foreachDouble((x:Double) => if (!java.lang.Double.isNaN(x) && x < min) min = x )
      min
   }))
  
 
  def handleNoDataTile = Double.PositiveInfinity

  def reducer(mapResults: List[Double]):Double = mapResults.foldLeft(Double.PositiveInfinity)(math.min(_, _)) 
}


