package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.data._
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData

trait Mean[T] {
  val sum: T
  val count: T
  def mean: T
  def +(b: T) : T
}

object Mean {

  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    trd.getTiles(re)
       .map { r => (r.rasterExtent, meanRaster(r))}
       .toMap
  }

  def meanRaster (r:Raster):Long = {
    var sum = 0L
    var count = 0L
    for(z <- r) { if (z != NODATA) { sum = sum + z; count = count + 1 }}
    math.round((sum/count).toDouble)
  }
}

case class MeanLong(sum: Long, count: Long) extends Mean[Long] {
  def mean = math.round((sum/count).toDouble)
  def +(b: MeanLong) = MeanLong(this.sum + b.sum,this.count + b.count)
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
case class Mean[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,MeanLong])
  (implicit val mB: Manifest[MeanLong], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Long] {

  type B = MeanLong
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
      MeanLong(sum,count)
    }))

  def handleFullTile(rOp:Op[Raster]) = 
    rOp.map (r =>
      tileResults.get(r.rasterExtent).getOrElse({
        var s = 0L
        var c = 0L
        r.force.foreach((x:Int) => if (s != NODATA) { s = s + x; c = c + 1 })
        MeanLong(s,c)
    }))
   

  def handleNoDataTile = MeanLong(0L,0L)

  def reducer(mapResults: List[MeanLong]):Long = {
    mapResults.foldLeft[MeanLong](MeanLong(0L,0L))(_ + _ ).mean
  }
}

object MeanDouble {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    trd.getTiles(re)
       .map { r => (r.rasterExtent, meanRaster(r))}
       .toMap
  }

  def meanRaster (r:Raster):Double = {
    var sum = 0.0
    var count = 0L
    r.foreachDouble( (x) => if (!java.lang.Double.isNaN(x)) { sum = sum + x; count = count + 1 })
    println("Sum: " + sum + " Count: " + count + " Result: " + sum/count)
    sum/count
  }
}

/**
 * Perform a zonal summary that calculates the mean of all raster cells within a geometry.
 *
 * @param   r             Raster to summarize
 * @param   zonePolygon   Polygon that defines the zone
 * @param   tileResults   Cached results of full tiles created by createTileResults
 */
case class MeanDouble[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,Double])
  (implicit val mB: Manifest[Double], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[Double] {
 
  type B = Double
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
      sum/count
    }))
  }

  def handleFullTile(rOp:Op[Raster]) = rOp.map (r =>
    tileResults.get(r.rasterExtent).getOrElse({
      var s = 0.0
      var c = 0L
      r.force.foreachDouble((x:Double) => if (!java.lang.Double.isNaN(s)) { s = s + x; c = c + 1 })
      println("Full Tile Sum: " + s + " Count: " + c + " Result: " + s/c)
      s/c
   }))
  
  def handleNoDataTile = 0.0

  def reducer(mapResults: List[Double]):Double = {
    mapResults.map(a =>  println("MR " + a))

    mapResults.foldLeft(0.0)(_ + _) / mapResults.length
  }
}
