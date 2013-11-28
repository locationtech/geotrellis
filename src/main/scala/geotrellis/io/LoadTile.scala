package geotrellis.io

import geotrellis._

import geotrellis.process._

object LoadTile {
  def apply(n:Op[String],col:Op[Int],row:Op[Int]):LoadTile =
    LoadTile(None,n,col,row,None)

  def apply(n:String,col:Int,row:Int,re:RasterExtent):LoadTile =
    LoadTile(None,n,col,row,Some(re))

  def apply(n:String,col:Int,row:Int,re:Option[RasterExtent]):LoadTile =
    LoadTile(None,n,col,row,re)

  def apply(ds:String,n:String,col:Int,row:Int):LoadTile =
    LoadTile(Some(ds),n,col,row,None)

  def apply(ds: String, n: String, col: Int, row: Int, re: RasterExtent):LoadTile =
    LoadTile(Some(ds),n,col,row,None)
}

case class LoadTile(ds:Op[Option[String]],
                    name:Op[String],
                    col:Op[Int],
                    row:Op[Int],
                    targetExtent:Op[Option[RasterExtent]]) extends Op[Raster] {
  def _run() = runAsync(List(ds, name, col, row, targetExtent))
  val nextSteps:Steps = {
    case (ds:Option[_]) :: 
         (n:String) :: 
         (col:Int) :: 
         (row:Int) :: 
         (te:Option[_]) :: Nil =>
      LayerResult { layerLoader =>
        val layer = layerLoader.getRasterLayer(ds.asInstanceOf[Option[String]],n)
        layer.getTile(col,row,te.asInstanceOf[Option[RasterExtent]])
      }
  }
}
