package geotrellis.process

import geotrellis._
import geotrellis.raster.TileLayout

case class RasterLayerInfo(id:LayerId,
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
  def apply(id:LayerId,
            rasterType:RasterType,
            rasterExtent:RasterExtent,
            epsg:Int,
            xskew:Double,
            yskew:Double):RasterLayerInfo = {
    val tl = TileLayout(1,1,rasterExtent.cols,rasterExtent.rows)
    RasterLayerInfo(id,rasterType,rasterExtent,epsg,xskew,yskew,false)
  }

  def apply(id:LayerId,
            rasterType:RasterType,
            rasterExtent:RasterExtent,
            epsg:Int,
            xskew:Double,
            yskew:Double,
            shouldCache:Boolean):RasterLayerInfo = {
    val tl = TileLayout(1,1,rasterExtent.cols,rasterExtent.rows)
    RasterLayerInfo(id,rasterType,rasterExtent,epsg,xskew,yskew,tl,shouldCache)
  }

  def apply(id:LayerId,
            rasterType:RasterType,
            rasterExtent:RasterExtent,
            epsg:Int,
            xskew:Double,
            yskew:Double,
            tileLayout:TileLayout):RasterLayerInfo = {
    RasterLayerInfo(id,rasterType,rasterExtent,epsg,xskew,yskew,tileLayout,false)
  }
}
