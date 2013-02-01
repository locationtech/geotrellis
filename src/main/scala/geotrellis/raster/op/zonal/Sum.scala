package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.data._
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData

object Sum {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    val tiles = trd.getTileList(re)
    tiles map { r => (r.rasterExtent, sumRaster(r))} toMap
  }

  def sumRaster (r:Raster):Long = {
    var sum = 0L
    r.foreach( (x) => if (x != NODATA) sum = sum + x )
    sum
  }
}

/**
 * Perform a zonal summary that calculates the sum of all raster cells within a geometry.
 * This operation is for integer typed Rasters. If you want the Sum for double type rasters
 * (TypeFloat,TypeDouble) use [[SumDouble]]
 *
 * @param   r             Raster to summarize
 * @param   zonePolygon   Polygon that defines the zone
 * @param   tileResults   Cached results of full tiles created by createTileResults
 */
case class Sum[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,Long]) 
  (implicit val mB: Manifest[Long], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Long] {
 
  type B = Long
  type D = DD
  
  def handlePartialTileIntersection(rOp: Op[Raster], gOp: Op[Geometry[D]]) = {
    rOp.flatMap ( r => gOp.flatMap ( g => {
      var sum: Long = 0L
      val f = new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.get(col,row)
            if (z != NODATA) { sum = sum + z }
          }
        }

      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent)(f)
      sum
    }))
  }

  def handleFullTile(rOp:Op[Raster]) = rOp.map (r =>
    tileResults.get(r.rasterExtent).getOrElse({
      var s = 0L
      r.force.foreach((x:Int) => if (s != NODATA) s = s + x)
      s
   }))
 
  def handleNoDataTile = 0L

  def reducer(mapResults: List[Long]):Long = mapResults.foldLeft(0L)(_ + _) 
}

object SumDouble {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    val tiles = trd.getTileList(re)
    tiles map { r => (r.rasterExtent, sumRaster(r))} toMap
  }

  def sumRaster (r:Raster):Double = {
    var sum = 0.0
    r.foreachDouble( (x) => if (!java.lang.Double.isNaN(x)) sum = sum + x)
    sum
  }
}

/**
 * Perform a zonal summary that calculates the sum of all raster cells within a geometry.
 *
 * @param   r             Raster to summarize
 * @param   zonePolygon   Polygon that defines the zone
 * @param   tileResults   Cached results of full tiles created by createTileResults
 */
case class SumDouble[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,Double])
  (implicit val mB: Manifest[Double], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Double] {
 
  type B = Double
  type D = DD
  
  def handlePartialTileIntersection(rOp: Op[Raster], gOp: Op[Geometry[D]]) = {
    rOp.flatMap ( r => gOp.flatMap ( g => {
      var sum = 0.0
      val f = new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.getDouble(col,row)
            if (!java.lang.Double.isNaN(z)) { sum = sum + z }
          }
        }
      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent)(f)
      sum
    }))
  }

  def handleFullTile(rOp:Op[Raster]) = rOp.map (r =>
    tileResults.get(r.rasterExtent).getOrElse({
      var s = 0.0
      r.force.foreachDouble((x:Double) => if (!java.lang.Double.isNaN(s)) s = s + x)
      s
   }))
  
  def handleNoDataTile = 0.0

  def reducer(mapResults: List[Double]):Double = mapResults.foldLeft(0.0)(_ + _) 
}
