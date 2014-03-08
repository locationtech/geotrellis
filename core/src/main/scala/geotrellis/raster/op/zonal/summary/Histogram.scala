package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.source._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.statistics._

object Histogram extends TileSummary[Histogram,Histogram,ValueSource[Histogram]] {
  def handlePartialTile[D](pt:PartialTileIntersection[D]):Histogram = {
    val PartialTileIntersection(r,polygons) = pt
    val histogram = FastMapHistogram()
    for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
      Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
        new Callback[Geometry,D] {
          def apply (col:Int, row:Int, g:Geometry[D]) {
            val z = r.get(col,row)
            if (isData(z)) histogram.countItem(z, 1)
          }
        }
      )
    }

    histogram
  }

  def handleFullTile(ft:FullTileIntersection):Histogram = {
    val histogram = FastMapHistogram()
    ft.tile.foreach((z:Int) => if (isData(z)) histogram.countItem(z, 1))
    histogram
  }

  def converge(ds:DataSource[Histogram,_]) =
    ds.map(x=>x).converge // Map to kick in the CanBuildFrom for HistogramDS
}
