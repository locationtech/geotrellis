package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.source._
import geotrellis.feature._
import geotrellis.feature.rasterize._

object Max extends TileSummary[Int,Int,ValueSource[Int]] {
  def handlePartialTile[D](pt:PartialTileIntersection[D]):Int = {
    val PartialTileIntersection(r,polygons) = pt
    var max = NODATA
    for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
      Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
        new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.get(col,row)
            if (isData(z) && (z > max || isNoData(max)) ) { max = z }
          }
        }
      )
    }
    max
  }

  def handleFullTile(ft:FullTileIntersection):Int = {
    var max = NODATA
    ft.tile.foreach { (x:Int) =>
      if (isData(x) && (x > max || isNoData(max))) { max = x }
    }
    max
  }

  def converge(ds:DataSource[Int,_]) =
    ds.reduce { (a,b) => 
      if(isNoData(a)) { b } 
      else if(isNoData(b)) { a }
      else { math.max(a,b) }
    }
}

object MaxDouble extends TileSummary[Double,Double,ValueSource[Double]] {
  def handlePartialTile[D](pt:PartialTileIntersection[D]):Double = {
    val PartialTileIntersection(r,polygons) = pt
    var max = Double.NaN
    for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
      Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
        new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.getDouble(col,row)
            if (isData(z) && (z > max || isNoData(max))) { max = z }
          }
        }
      )
    }

    max
  }

  def handleFullTile(ft:FullTileIntersection):Double = {
    var max = Double.NaN
    ft.tile.foreach((x:Int) => if (isData(x) && (x > max || isNoData(max))) { max = x })
    max
  }

  def converge(ds:DataSource[Double,_]) =
    ds.reduce { (a,b) =>
      if(isNoData(a)) { b } 
      else if(isNoData(b)) { a }
      else { math.max(a,b) }
    }
}
