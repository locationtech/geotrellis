package geotrellis.operation

import geotrellis._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.RasterExtent

/**
  * Perform an operation on every tile in a tileset, and return the new tileset.
  *
  * For example,
  * <pre>
  * val r = LoadFile(f)
  * val t = Tile(R, 256)
  * val t2 = DoTile(R, AddConstant(_, 3)) // _ is in place of a raster operation
  * </pre>
  */
case class ForEachTile(r:Op[Raster])(f:(Op[Raster] => Op[Raster])) extends Op[Raster] {
  def _run(context:Context) = runAsync(r :: Nil)

  val nextSteps:Steps = { 
    case (tr:Raster) :: Nil => { 
      val (ops:List[Op[Raster]], tileSet:Option[_]) = 
        tr.data match {
      	  case trData:TileRasterData => {
      		  val ops = trData.rasters.toList.flatten 
      		    .map { c => f(Literal(c)) }
      		  (ops, Some(trData.tileSet))
      	  }
      	  // Not a tiled raster -- just apply f to input raster
      	  case _ => (List(f(r), None))
        }
      runAsync(tr.rasterExtent :: tileSet :: ops)
    }
  
    case rasterExtent :: tileSet :: rasters => {
      val rs:Array[Option[Raster]] = rasters map { case r:Raster => Some(r) } toArray
      val outputRaster = tileSet match { 
        case Some(tileSet:TileSet) => { 
          Raster(TileRasterData(tileSet, rs), rasterExtent.asInstanceOf[RasterExtent])
        }
        case None => rs.head.get
      }
      Result(outputRaster)
    }
  }
}


