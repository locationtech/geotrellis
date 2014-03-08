package geotrellis.process

import geotrellis._
import geotrellis.raster.{IntConstant,DoubleConstant}

import com.typesafe.config.Config

object ConstantRasterLayerBuilder extends RasterLayerBuilder {
  def apply(ds:Option[String],jsonPath:String, json:Config):RasterLayer = {
    val cols = json.getInt("cols")
    val rows = json.getInt("rows")

    val (cellWidth,cellHeight) = getCellWidthAndHeight(json)
    val rasterExtent = RasterExtent(getExtent(json), cellWidth, cellHeight, cols, rows)

    val rasterType = getRasterType(json)
    val info = 
      RasterLayerInfo(
        LayerId(ds,getName(json)),
        getRasterType(json),
        rasterExtent,
        getEpsg(json),
        getXskew(json),
        getYskew(json)
      )

    if(rasterType.isDouble) {
      new DoubleConstantLayer(info, json.getDouble("constant"))
    } else {
      new IntConstantLayer(info, json.getInt("constant"))
    }
  }
}

class IntConstantLayer(info:RasterLayerInfo, value:Int) 
extends UntiledRasterLayer(info) {
  def getRaster(targetExtent:Option[RasterExtent] = None) = {
    val re = targetExtent match {
      case Some(rext) => rext
      case None => info.rasterExtent
    }
    Raster(IntConstant(value,re.cols,re.rows),re)
  }

  def cache(c:Cache[String]) = {} // No-op
}

class DoubleConstantLayer(info:RasterLayerInfo, value:Double) 
extends UntiledRasterLayer(info) {
  def getRaster(targetExtent:Option[RasterExtent]) = {
    val re = targetExtent match {
      case Some(rext) => rext
      case None => info.rasterExtent
    }
    Raster(DoubleConstant(value,re.cols,re.rows),re)
  }

  def cache(c:Cache[String]) = {} // No-op
}

