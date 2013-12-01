package geotrellis.io

import geotrellis._

import geotrellis.process._

object LoadTile {
  def apply(n:String,col:Op[Int],row:Op[Int]):LoadTile =
    LoadTile(LayerId(n),col,row,None)

  def apply(n:String,col:Int,row:Int,re:RasterExtent):LoadTile =
    LoadTile(LayerId(n),col,row,Some(re))

  def apply(ds:String,n:String,col:Int,row:Int):LoadTile =
    LoadTile(LayerId(ds,n),col,row,None)

  def apply(ds: String, n: String, col: Int, row: Int, re: RasterExtent):LoadTile =
    LoadTile(LayerId(ds,n),col,row,None)

  def apply(layerId: LayerId, col: Int, row: Int):LoadTile =
    LoadTile(layerId, col, row, None)
}

case class LoadTile(layerId:Op[LayerId],
                    col:Op[Int],
                    row:Op[Int],
                    targetExtent:Op[Option[RasterExtent]]) extends Op[Raster] {
  def _run() = runAsync(List(layerId, col, row, targetExtent))
  val nextSteps:Steps = {
    case (layerId:LayerId) :: 
         (col:Int) :: 
         (row:Int) :: 
         (te:Option[_]) :: Nil =>
      LayerResult { layerLoader =>
        val layer = layerLoader.getRasterLayer(layerId)
        layer.getTile(col,row,te.asInstanceOf[Option[RasterExtent]])
      }
  }
}
