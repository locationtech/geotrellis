package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.source._
import geotrellis.feature._
import geotrellis.feature.rasterize._

object Min extends TileSummary[Int,Int,ValueSource[Int]] {
  def handlePartialTile[D](pt:PartialTileIntersection[D]):Int = {
    val PartialTileIntersection(r,polygons) = pt
    var min = NODATA
    for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
      Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
        new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.get(col,row)
            if (isData(z) && (z < min || isNoData(min)) ) { min = z }
          }
        }
      )
    }
    min
  }

  def handleFullTile(ft:FullTileIntersection):Int = {
    var min = NODATA
    ft.tile.foreach { (x:Int) =>
      if (isData(x) && (x < min || isNoData(min))) { min = x }
    }
    min
  }

  def converge(ds:DataSource[Int,_]) =
    ds.reduce { (a,b) => 
      if(isNoData(a)) { b } 
      else if(isNoData(b)) { a }
      else { math.min(a,b) }
    }
}

object MinDouble extends TileSummary[Double,Double,ValueSource[Double]] {
  def handlePartialTile[D](pt:PartialTileIntersection[D]):Double = {
    val PartialTileIntersection(r,polygons) = pt
    var min = Double.NaN
    for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
      Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
        new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.getDouble(col,row)
            if (isData(z) && (z < min || isNoData(min))) { min = z }
          }
        }
      )
    }

    min
  }

  def handleFullTile(ft:FullTileIntersection):Double = {
    var min = Double.NaN
    ft.tile.foreach((x:Int) => if (isData(x) && (x < min || isNoData(min))) { min = x })
    min
  }

  def converge(ds:DataSource[Double,_]) =
    ds.reduce { (a,b) =>
      if(isNoData(a)) { b } 
      else if(isNoData(b)) { a }
      else { math.min(a,b) }
    }
}
