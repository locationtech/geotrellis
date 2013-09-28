package geotrellis.process

import geotrellis._
import geotrellis.raster.TileLayout

case class RasterLayerInfo(name:String,
                           rasterType:RasterType,
                           rasterExtent:RasterExtent,
                           epsg:Int,
                           xskew:Double,
                           yskew:Double,
			   tileLayout:TileLayout,
                           shouldCache:Boolean = false) {
  var cached = false
}


object RasterLayerInfo {
  //* For untiled rasters */
  def apply(name:String,
            rasterType:RasterType,
            rasterExtent:RasterExtent,
            epsg:Int,
            xskew:Double,
            yskew:Double):RasterLayerInfo = {
    val tl = TileLayout(1,1,rasterExtent.cols,rasterExtent.rows)
    RasterLayerInfo(name,rasterType,rasterExtent,epsg,xskew,yskew,false)
  }

  def apply(name:String,
            rasterType:RasterType,
            rasterExtent:RasterExtent,
            epsg:Int,
            xskew:Double,
            yskew:Double,
            shouldCache:Boolean):RasterLayerInfo = {
    val tl = TileLayout(1,1,rasterExtent.cols,rasterExtent.rows)
    RasterLayerInfo(name,rasterType,rasterExtent,epsg,xskew,yskew,tl,shouldCache)
  }

  def apply(name:String,
            rasterType:RasterType,
            rasterExtent:RasterExtent,
            epsg:Int,
            xskew:Double,
            yskew:Double,
            tileLayout:TileLayout):RasterLayerInfo = {
    RasterLayerInfo(name,rasterType,rasterExtent,epsg,xskew,yskew,tileLayout,false)
  }
}
