package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.data._
import geotrellis.statistics._
import geotrellis.raster.IntConstant
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData

object TiledPolygonalZonalSum {
  def createTileSums(trd:TiledRasterData, re:RasterExtent) = {
    val tiles = trd.getTileList(re)
    tiles map { r => (r.rasterExtent, sumRaster(r))} toMap
  }
  
  def sumRaster (r:Raster):Long =  {
    var sum = 0L
    r.foreach( (x) => if (x != NODATA) sum = sum + x )
    sum
  }

  def f[D](raster:Raster, polygon:Polygon[D]) = {
    var sum: Long = 0L
    val f = (col:Int, row:Int, p:Polygon[D]) => { 
      val z = raster.get(col,row)
      if (z != NODATA) { sum = sum + z; } 
    }
    geotrellis.feature.rasterize.PolygonRasterizer.foreachCellByPolygon(
      polygon,
      raster.rasterExtent,
      f)
    sum
  }

  val reducer =  (ints:List[Long]) => ints.reduceLeft((x, y) => x + y)

  def apply[D](p: Polygon[D], r: Op[Raster], tileSums:Map[RasterExtent,Long] = Map(), threshold:Int = 30) = {
    val f = (r2:Raster, polygon:Polygon[D]) => {
      var sum: Long = 0L
      val f2 = (col:Int, row:Int, pz:Polygon[D]) => {
        val z = r2.get(col,row)
        if (z != NODATA) sum = sum + z
      }
      geotrellis.feature.rasterize.PolygonRasterizer.foreachCellByPolygon(
        polygon,
        r2.rasterExtent,
        f2)
      sum
    }

    TiledPolygonalZonalSummary2(p, r, f, sumRaster, 0L, reducer, tileSums, threshold) 
  }
}

object TiledPolygonalZonalSummary2 {
  def apply[R:Manifest, Z:Manifest, D](p: Polygon[D], r: Op[Raster], f:(Raster,Polygon[D]) => R, g:(Raster) => R, empty:R, reducer:List[R] => Z, tileResults:Map[RasterExtent,R] = Map(), threshold:Int = 30):Op[Z]= {
  raster.op.tiles.ThoroughputLimitedReducer1(r, threshold)({ r =>
    {
      val r2 = r.force
      val s: R = r2.data match {
        case x: IntConstant if x.n == NODATA => { // TODO: test for NODATA 
          //println("empty tile")
          empty
        }
        case rdata: ArrayRasterData => {
          val tileExtent = r.rasterExtent.extent.asFeature(())
          val jtsPolygon = p.geom
          val jtsExtent = tileExtent.geom
          if (jtsPolygon.contains(jtsExtent)) {
            tileResults.get(r2.rasterExtent).getOrElse( g(r2) )
          } else if (jtsPolygon.disjoint(jtsExtent)) {
            empty
          } else {
            val tilePolygonOrig = jtsPolygon.intersection(jtsExtent)
            //val tilePolygon = com.vividsolutions.jts.simplify.TopologyPreservingSimplifier.simplify(tilePolygonOrig, r.rasterExtent.cellwidth)
            val rasterExtent = r.rasterExtent
            val p2 = tilePolygonOrig match {
              case t: com.vividsolutions.jts.geom.Polygon => {
                val pp = geotrellis.feature.Polygon(t, p.data)
            	  pp
              }
              case x => {
                throw new Exception("tilePolygon is: " + x)
                null
              }
            }
            f(r2,p2)
          } 
        }
      }
      s
      //var histmap = Array.ofDim[Int](1)
    }
})(reducer)
}
}

