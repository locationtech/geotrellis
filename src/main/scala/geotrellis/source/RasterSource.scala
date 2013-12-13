package geotrellis.source

import geotrellis._
import geotrellis.process.LayerId
import geotrellis.raster.op._
import geotrellis.statistics.Histogram
import geotrellis.raster._

class RasterSource(val rasterDef: Op[RasterDefinition], val tileOps:Op[Seq[Op[Raster]]]) 
    extends  RasterSourceLike[RasterSource] {
  def elements = tileOps
  val rasterDefinition = rasterDef
}

object RasterSource {
  def fromPath(path:String):RasterSource = 
    fromPath(path,None)

  def fromPath(path:String,rasterExtent:RasterExtent):RasterSource =
    fromPath(path,Some(rasterExtent))

  def fromPath(path:String,targetExtent:Option[RasterExtent]):RasterSource = {
    val rasterLayer = io.LoadRasterLayerFromPath(path)
    val rasterDef = 
      rasterLayer map { layer =>
        RasterDefinition(layer.info.id,
                         layer.info.rasterExtent,
                         layer.info.tileLayout,
                         layer.info.rasterType)
      }

    val tileOps = 
      targetExtent match {
        case re @ Some(_) =>
          rasterLayer.map { layer =>
            Seq(Literal(layer.getRaster(re)))
          }
        case None =>
          rasterLayer.map { layer =>
            (for(tileRow <- 0 until layer.info.tileLayout.tileRows;
              tileCol <- 0 until layer.info.tileLayout.tileCols) yield {
              Literal(layer.getTile(tileCol,tileRow))
            })
          }
      }

    RasterSource(rasterDef,tileOps)
  }

  def apply(name:String):RasterSource =
    RasterSource(io.LoadRasterDefinition(LayerId(name)),None)

  def apply(name:String,rasterExtent:RasterExtent):RasterSource =
    RasterSource(io.LoadRasterDefinition(LayerId(name)),Some(rasterExtent))

  def apply(store:String,name:String):RasterSource =
    RasterSource(io.LoadRasterDefinition(LayerId(name)),None)

  def apply(store:String,name:String,rasterExtent:RasterExtent):RasterSource =
    RasterSource(io.LoadRasterDefinition(LayerId(store,name)),Some(rasterExtent))

  def apply(layerId:LayerId):RasterSource =
    RasterSource(io.LoadRasterDefinition(layerId),None)

  def apply(layerId:LayerId,rasterExtent:RasterExtent):RasterSource =
    RasterSource(io.LoadRasterDefinition(layerId),Some(rasterExtent))

  def apply(rasterDef:Op[RasterDefinition]):RasterSource = 
    apply(rasterDef,None)

  def apply(rasterDef:Op[RasterDefinition],targetExtent:RasterExtent):RasterSource = 
    apply(rasterDef,Some(targetExtent))

  def apply(rasterDef:Op[RasterDefinition],targetExtent:Option[RasterExtent]):RasterSource = {
    val tileOps = 
      targetExtent match {
        case re @ Some(_) =>
          rasterDef.map { rd =>
            Seq(io.LoadRaster(rd.layerId,re))
          }
        case None =>
          rasterDef.map { rd =>
            (for(tileRow <- 0 until rd.tileLayout.tileRows;
              tileCol <- 0 until rd.tileLayout.tileCols) yield {
              io.LoadTile(rd.layerId,tileCol,tileRow)
            })
          }
      }

    new RasterSource(rasterDef, tileOps)
  }

  def apply(rasterDef:Op[RasterDefinition],tileOps:Op[Seq[Op[Raster]]]) =
    new RasterSource(rasterDef, tileOps)

  /** Create a RasterSource who's tile ops are the tiles of a TileRaster. */
  def apply(tiledRaster:TileRaster):RasterSource = {
    val rasterDef = 
      RasterDefinition(
        LayerId("LiteralTileRaster"),
        tiledRaster.rasterExtent,
        tiledRaster.tileLayout,
        tiledRaster.rasterType
      )
    
    val tileOps = tiledRaster.tiles.map(Literal(_))

    new RasterSource(rasterDef, tileOps)
  }

  def apply(tiledRaster:Op[TileRaster])(implicit d:DI):RasterSource = {
    val rasterDef = tiledRaster.map { tr =>
      RasterDefinition(
        LayerId("LiteralTileRaster"),
        tr.rasterExtent,
        tr.tileLayout,
        tr.rasterType
      )
    }
    val tileOps = tiledRaster.map(_.tiles.map(Literal(_)))
    new RasterSource(rasterDef, tileOps)
  }

  def apply(raster:Op[Raster])(implicit d:DI,d2:DI):RasterSource = {
    val rasterDef = 
      raster.map { r =>
        val (cols,rows) = (r.rasterExtent.cols,r.rasterExtent.rows)
        RasterDefinition(LayerId("LiteralRaster"),
                         r.rasterExtent,
                         TileLayout.singleTile(cols,rows),
                         r.rasterType)
      }

    val tileOps = Literal(Seq(raster))
    new RasterSource(rasterDef, tileOps)
  }

  def apply(r:Raster):RasterSource = {
    val rasterDef = 
      Literal {
        val (cols,rows) = (r.rasterExtent.cols,r.rasterExtent.rows)
        RasterDefinition(LayerId("LiteralRaster"),
                         r.rasterExtent,
                         TileLayout.singleTile(cols,rows),
                         r.rasterType)
      }

    val tileOps = Literal(Seq(Literal(r)))
    new RasterSource(rasterDef, tileOps)
  }
}
