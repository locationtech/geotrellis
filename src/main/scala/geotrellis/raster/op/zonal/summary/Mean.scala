package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.data._
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData

case class LongMean(sum: Long, count: Long) {
  def mean = math.round((sum/count).toDouble)
  def +(b: LongMean) = LongMean(sum + b.sum,count + b.count)
}

object Mean {

  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    trd.getTiles(re)
       .map { r => (r.rasterExtent, meanTiledRaster(r))}
       .toMap
  }

  def meanRaster (r:Raster):Long = {
    meanTiledRaster(r).mean
  }

  def meanTiledRaster (r:Raster):LongMean = {
    var sum = 0L
    var count = 0L
    for(z <- r) { if (z != NODATA) { sum = sum + z; count = count + 1 }}
    LongMean(sum,count)
  }
}

/**
 * Perform a zonal summary that calculates the mean of all raster cells within a geometry.
 * This operation is for integer typed Rasters. If you want the Mean for double type rasters
 * (TypeFloat,TypeDouble) use [[MeanDouble]]
 *
 * @param   r             Raster to summarize
 * @param   zonePolygon   Polygon that defines the zone
 * @param   tileResults   Cached results of full tiles created by createTileResults
 */
case class Mean[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,LongMean])
  (implicit val mB: Manifest[LongMean], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Long] {

  type B = LongMean
  type D = DD
  
  def handlePartialTileIntersection(rOp: Op[Raster], gOp: Op[Geometry[D]]) = 
    rOp.flatMap ( r => gOp.flatMap ( g => {
      var sum: Long = 0L
      var count: Long = 0L
      val f = new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.get(col,row)
            if (z != NODATA) { sum = sum + z; count = count + 1 }
          }
        }

      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent)(f)
      LongMean(sum,count)
    }))

  def handleFullTile(rOp:Op[Raster]) = 
    rOp.map (r =>
      tileResults.get(r.rasterExtent).getOrElse({
        var s = 0L
        var c = 0L
        r.force.foreach((x:Int) => if (s != NODATA) { s = s + x; c = c + 1 })
        LongMean(s,c)
    }))
   

  def handleNoDataTile = LongMean(0L,0L)

  def reducer(mapResults: List[LongMean]):Long = {
    mapResults.foldLeft(LongMean(0L,0L))(_ + _).mean
  }
}

case class DoubleMean(sum: Double, count: Double) {
  def mean = sum/count
  def +(b: DoubleMean) = DoubleMean(sum + b.sum,count + b.count)
}

object MeanDouble {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    trd.getTiles(re)
       .map { r => (r.rasterExtent, meanTileRaster(r))}
       .toMap
  }

  def meanRaster (r:Raster):Double = {
    meanTileRaster(r).mean
  }

  def meanTileRaster (r:Raster):DoubleMean = {

    var sum = 0.0
    var count = 0L
    r.foreachDouble( (x) => if (!java.lang.Double.isNaN(x)) { sum = sum + x; count = count + 1 })
    println("Sum: " + sum + " Count: " + count + " Result: " + sum/count)
    DoubleMean(sum,count)
  }
}

/**
 * Perform a zonal summary that calculates the mean of all raster cells within a geometry.
 *
 * @param   r             Raster to summarize
 * @param   zonePolygon   Polygon that defines the zone
 * @param   tileResults   Cached results of full tiles created by createTileResults
 */
case class MeanDouble[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,DoubleMean])
  (implicit val mB: Manifest[DoubleMean], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Double] {
 
  type B = DoubleMean
  type D = DD
  
  def handlePartialTileIntersection(rOp: Op[Raster], gOp: Op[Geometry[D]]) = {
    rOp.flatMap ( r => gOp.flatMap ( g => {
      var sum = 0.0
      var count = 0L
      val f = new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.getDouble(col,row)
            if (!java.lang.Double.isNaN(z)) { sum = sum + z; count = count + 1 }
          }
        println("Partial Sum: " + sum + " Count: " + count + " Result: " + sum/count)
        }
      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        r.rasterExtent)(f)
      DoubleMean(sum,count)
    }))
  }

  def handleFullTile(rOp:Op[Raster]) = rOp.map (r =>
    tileResults.get(r.rasterExtent).getOrElse({
      var s = 0.0
      var c = 0L
      r.force.foreachDouble((x:Double) => if (!java.lang.Double.isNaN(s)) { s = s + x; c = c + 1 })
      println("Full Tile Sum: " + s + " Count: " + c + " Result: " + s/c)
      DoubleMean(s,c)
   }))
  
  def handleNoDataTile = DoubleMean(0.0,0.0)

  def reducer(mapResults: List[DoubleMean]):Double =  mapResults.foldLeft(DoubleMean(0.0,0.0))(_ + _).mean
}
