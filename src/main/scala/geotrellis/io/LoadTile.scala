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

case class LoadTile(ds:Op[Option[String]],n:Op[String],col:Op[Int],row:Op[Int],targetExtent:Op[Option[RasterExtent]]) 
    extends Op[Raster] {
  def _run(context:Context) = runAsync(List(ds, n, col, row, targetExtent, context))
  val nextSteps:Steps = {
    case (ds:Option[String]) :: 
         (n:String) :: 
         (col:Int) :: 
         (row:Int) :: 
         (te:Option[RasterExtent]) ::
         (context:Context) :: Nil =>
      val layer = context.getRasterLayer(ds,n)
      Result(layer.getTile(col,row,te))
  }
}
