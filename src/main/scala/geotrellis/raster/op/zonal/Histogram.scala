package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.data._
import geotrellis.statistics.{Histogram => HistogramObj, FastMapHistogram}
import scala.math.{ max, min }
import geotrellis.raster.TileArrayRasterData
import geotrellis.raster.TiledRasterData

object Histogram {
  def createTileResults(trd:TiledRasterData, re:RasterExtent) = {
    val tiles = trd.getTileList(re)
    tiles map { r => (r.rasterExtent, histogramRaster(r))} toMap
  }

  def histogramRaster (r:Raster):HistogramObj = FastMapHistogram.fromRaster(r)
}

/**
 * Perform a zonal summary that calculates a histogram all raster cells within a geometry.
 *
 * @param   r             Raster to summarize
 * @param   zonePolygon   Polygon that defines the zone
 * @param   tileResults   Cached results of full tiles created by createTileResults
 */
case class Histogram[DD] (r:Op[Raster], zonePolygon:Op[Polygon[DD]], tileResults:Map[RasterExtent,HistogramObj]) 
  (implicit val mB: Manifest[HistogramObj], val mD: Manifest[DD]) extends TiledPolygonalZonalSummary[HistogramObj] {
 
  type B = HistogramObj
  type D = DD
  
  def handlePartialTileIntersection(rOp: Op[Raster], gOp: Op[Geometry[D]]) = {
    rOp.flatMap ( r => gOp.flatMap ( g => {
      val raster = r.force
      var histogram = FastMapHistogram()
      val f = (col:Int, row:Int, g:Geometry[_]) => {
        val z = raster.get(col,row)
        if (z != NODATA) histogram.countItem(z, 1)
      }
      geotrellis.feature.rasterize.Rasterizer.foreachCellByFeature(
        g,
        raster.rasterExtent,
        f)
      histogram
    }))
  }

  def handleFullTile(rOp:Op[Raster]) = rOp.map (r =>
    tileResults.get(r.rasterExtent).getOrElse({
      var histogram = FastMapHistogram()
      r.force.foreach((z:Int) => if (z != NODATA) histogram.countItem(z, 1))
      histogram
   }))
  
 
  def handleNoDataTile = Literal(FastMapHistogram())

  def reducer(mapResults: List[HistogramObj]):HistogramObj = FastMapHistogram.fromHistograms(mapResults)
}


