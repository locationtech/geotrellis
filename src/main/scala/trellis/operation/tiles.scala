package trellis.operation

import trellis._
import trellis.raster._
import trellis.process._
import trellis.RasterExtent

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
case class ForEachTile(r:Op[IntRaster])(f:(Op[IntRaster] => Op[IntRaster])) extends Op[IntRaster] {
  def _run(context:Context) = runAsync(r :: Nil)

  val nextSteps:Steps = { 
    case (tr:IntRaster) :: Nil => { 
      val (ops:List[Op[IntRaster]], tileSet:Option[_]) = 
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
      val rs:Array[Option[IntRaster]] = rasters map { case r:IntRaster => Some(r) } toArray
      val outputRaster = tileSet match { 
        case Some(tileSet:TileSet) => { 
          IntRaster(TileRasterData(tileSet, rs), rasterExtent.asInstanceOf[RasterExtent])
        }
        case None => rs.head.get
      }
      Result(outputRaster)
    }
  }
}


