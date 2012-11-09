package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.data._
import geotrellis.statistics._
import geotrellis.raster.IntConstant
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData

object TiledPolygonalZonalCount {
  def createTileSums(trd:TiledRasterData, re:RasterExtent) = {
    val tiles = trd.getTileList(re)
    tiles map { r => (r.rasterExtent, sumRaster(r))} toMap
  }
  
  def sumRaster (r:Raster):Long =  {
    var sum = 0L
    r.foreach( (x) => if (x != NODATA && x > 0) sum = sum + x )
    sum
  }

}

case class TiledPolygonalZonalCount[D](p: Polygon[D], r: Op[Raster], tileSums:Map[RasterExtent,Long] = Map(), threshold:Int = 30) extends raster.op.tiles.ThoroughputLimitedReducer1(r, threshold)({
  r =>
    {
      val r2 = r.force
      val s: Long = r2.data match {
        case x: IntConstant if x.n == NODATA => { // TODO: test for NODATA 
          //println("empty tile")
          0L
        }
        case rdata: ArrayRasterData => {
          val tileExtent = r.rasterExtent.extent.asFeature(())
          val jtsPolygon = p.geom
          val jtsExtent = tileExtent.geom
          if (jtsPolygon.contains(jtsExtent)) {
            tileSums.get(r.rasterExtent).getOrElse( {
            var s = 0L
            rdata.foreach((x:Int) => if (s != NODATA) s = s + x)
            s
            
            }
          )
          
          } else if (jtsPolygon.disjoint(jtsExtent)) {
            val w = new com.vividsolutions.jts.io.WKTWriter()
            //println(w.write(jtsExtent))
            //println(w.write(jtsPolygon))
  
            //println("tile is disjoint from polygon!")
            //println("jtsExtent is: " + jtsExtent)
            //println("jtsPolygon is: " + jtsPolygon)
            0L
          } else {
            //println("tile is *not* disjoint")
            val tilePolygonOrig = jtsPolygon.intersection(jtsExtent)
            val tilePolygon = com.vividsolutions.jts.simplify.TopologyPreservingSimplifier.simplify(tilePolygonOrig, r.rasterExtent.cellwidth)
            val rasterExtent = r.rasterExtent
            val p2 = tilePolygon match {
              case t: com.vividsolutions.jts.geom.Polygon => {
                //println("we have a polygon")
                val pp = geotrellis.feature.Polygon(t, ())
                //val w = new com.vividsolutions.jts.io.WKTWriter()
                //println("tile: " + w.write(jtsExtent) + " /// polygon: " + w.write(t))
            	  pp
              }
              case x => {
                //println("tilePolygon is: " + x)
                throw new Exception("tilePolygon is: " + x)
                null
              }
            }
            var sum: Long = 0L
            val f = (col:Int, row:Int, p:Polygon[Unit]) => { sum = sum + r2.get(col,row) }
            geotrellis.feature.rasterize.PolygonRasterizer.foreachCellByPolygon(
              p2,
              rasterExtent,
              f)
            sum
          }
        }
      }
      //println("tile result is: " + s)
      s
      //var histmap = Array.ofDim[Int](1)
    }
})({
  ints => ints.reduceLeft((x, y) => x + y)
})



/**
 * Given a raster and an array of polygons, return a histogram summary of the cells
 * within each polygon.
 */

/*case class PolygonalZonalCount(ps: Array[Polygon], r: Op[Raster]) extends Op[Long] {
  def _run(context: Context) = runAsync(r :: ps.toList)

  val nextSteps: Steps = {
    case raster :: polygons => {
      step2(raster.asInstanceOf[Raster], polygons.asInstanceOf[List[Polygon]])
    }
  }

  def step2(raster: Raster, polygons: List[Polygon]) = {
    val startTime = System.currentTimeMillis()
    // build our map to hold results
    // secretly build array
    var pmax = polygons.map(_.value).reduceLeft(_ max _)
    var histmap = Array.ofDim[Int](pmax + 1)

    // dereference some useful variables
    val geo = raster.rasterExtent
    val rdata = raster.data.asArray.getOrElse(sys.error("need array"))

    val p0 = polygons(0)
    val rows = geo.rows
    val cols = geo.cols

    // calculate the bounding box
    var xmin = p0.xmin
    var ymin = p0.ymin
    var xmax = p0.xmax
    var ymax = p0.ymax
    polygons.tail.foreach {
      p =>
        {
          xmin = min(xmin, p.xmin)
          ymin = min(ymin, p.ymin)
          xmax = max(xmax, p.xmax)
          ymax = max(ymax, p.ymax)
        }
    }

    // save the bounding box as grid coordinates
    val (col1, row1) = geo.mapToGrid(xmin, ymax)
    val (col2, row2) = geo.mapToGrid(xmax, ymin)

    //println("Elapsed: %d" format (System.currentTimeMillis() - startTime))

    val c = geo.cols
    var r = geo.rows

    //println("Elapsed (data 0): %d" format (System.currentTimeMillis() - startTime))

    // burn our polygons onto a raster
    // val zones = Raster.empty(geo)
    //println("Elapsed (empty): %d" format (System.currentTimeMillis() - startTime))
    // val zdata = zones.data.asArray.getOrElse(sys.error("need array"))

    //println("Elapsed (data): %d" format (System.currentTimeMillis() - startTime))

    // Accumulate based on polygon value
    // uses array based backend
    var total: Long = 1
    val cb: ARasterizer.CB[Long] = new ARasterizer.CB[Long] {
      def apply(d: Int, value: Int, amap: Long) = {
        var inp = rdata(d).asInstanceOf[Long]
        if (inp < 0 && inp != NODATA) {
          //println("negative value: %d".format(inp))
          inp = NODATA.asInstanceOf[Long]
        }

        if (inp != NODATA) {
          total += rdata(d)
          //amap(value) += rdata(d)
          //println("in apply, with value: %d, rdata(d): %d, total: %d".format(value, rdata(d), total))
        }
        total
        //amap
      }
    }

    //ARasterizer.rasterize(cb, histmap, geo, polygons.toArray)
    ARasterizer.rasterize[Long](cb, total, geo, polygons.toArray)
    //Rasterizer.rasterize(zones, polygons.toArray)
    //println("Elapsed (after burn): %d" format (System.currentTimeMillis() - startTime))
    //println("(total is: %s".format(total))
    Result(total)
    // iterate over the cells in our bounding box; determine its zone, then
    // looking in the raster for a value to add to the zonal histogram.
*/
    /*
    var row = row1
    while (row < row2) {
      var col = col1
      while (col < col2) {
        val i     = row * cols + col
        val value = rdata(i)
        if (value != NODATA) {
          val zone  = zdata(i)
          if (zone != NODATA) {
            histmap(zone) += value
          }
        }
        col += 1
      }
      row += 1
    }

    println("Elapsed (after sum): %d" format (System.currentTimeMillis() - startTime))

    // return an immutable mapping
    Result(polygons.foldLeft(Map[Int,Int]()) { 
       (m, pgon) => m + (pgon.value -> histmap(pgon.value))
    })
    */
/*
  }

}
*/
