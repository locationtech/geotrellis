package geotrellis.raster.op.hydrology

import geotrellis._
import geotrellis.raster._
import scala.math._

object FlowDirection {
  /** Determine flow direction of the cell at (c,r) in given raster */
  def flow(c:Int,r:Int,raster:Raster) = {
    val neighbors = getNeighbors(c,r,raster)
    val max = neighbors.values.max
    neighbors.filter { case(_,v) => v == max }.keys.reduce(_+_)
  }
  
  /** Produces a map of available immediate neighbors and their drop in elevation from the provided cell */
  def getNeighbors(c:Int,r:Int,raster:Raster):Map[Int,Double] = {
    val center = raster.get(c,r)
    val ncols = raster.cols
    val nrows = raster.rows
    // weights to be applied to drop in elevation towards the neighbours
    val distances = 
      Map[Int,Double](
        (1,        1   ),
        (2,   sqrt(2)  ),
        (4,        1   ),
        (8,   sqrt(2)  ),
        (16,       1   ),
        (32,  sqrt(2)  ),
        (64,       1   ),
        (128, sqrt(2)  )
      )
    // coordinates of the 8 neighbours
    val map = 
      Map[Int,(Int,Int)](
        ( 1,   (c+1,r)    ),
        ( 2,   (c+1,r+1)  ),
        ( 4,   (c,r+1)    ),
        ( 8,   (c-1,r+1)  ),
        (16,   (c-1,r)    ),
        (32,   (c-1,r-1)  ),
        (64,   (c,r-1)    ),
        (128,  (c+1,r-1)  )
      )
    // remove invalid neighbours and produce map of drop-values
      map.filter { case(_,(col,row)) =>
        0 <= col && col < ncols &&
        0 <= row && row < nrows &&
        isData(raster.get(col,row))
      }.map { case (k,v) => k -> (center-raster.get(v._1,v._2)) / distances(k) }
  }

  /** Determines whether or not the cell at (c,r) in given raster is a sink */
  def isSink(c:Int,r:Int,raster:Raster) = {
    getNeighbors(c,r,raster).values.foldLeft(true)( _ && _ < 0)
  }
}

/**
 *  Operation to compute a flow direction raster from an elevation raster
 *  
 *  The directional encoding is from:
 *    Greenlee,D. D. 1987. "Raster and Vector Processing for Scanned Linework."
 *    Photogrammetric Engineering and Remote Sensing (ISSN 0099-1112),vol. 53,Oct. 1987,p. 1383-1387.
 *  
 *  The direction of flow is towards the neighboring cell with the largest drop in elevation. If two 
 *  or more cells have the same drop in elevation,their directional values are added together. The
 *  8-bit encoding of the direction preserves the multi-directional property.
 *  
 *  Sinks,cells which have no drop in elevation towards any neighbor,have no direction of flow.
 *
 *  @param        raster           Elevation raster the operation will run against. 
 */
case class FlowDirection(raster:Op[Raster]) extends Op1(raster)({
  (raster) =>
    val ncols = raster.cols
    var nrows = raster.rows
    val data = IntArrayRasterData(Array.ofDim[Int](nrows*ncols),ncols,nrows)
    var r = 0
    while (r < nrows) {
      var c = 0
      while (c < ncols) {
        if (isNoData(raster.get(c,r)) || (FlowDirection.isSink(c,r,raster))) {
          data.set(c,r,NODATA)
        } else {
          data.set(c,r,FlowDirection.flow(c,r,raster))
        }        
        c = c + 1
      }
      r = r + 1
    }
    Result(Raster(data,raster.rasterExtent))
})
