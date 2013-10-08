/**
 *
 */
package geotrellis.raster

import geotrellis._
import scala.collection.concurrent.Map
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable

object TileCache {
  // relative coordinate of each neighbor
  val neighborTuples = (for (i <- -1 to 1; j <- -1 to 1) yield (i, j)).filter(_ != (0, 0))

}
class TileCache(tiles: TiledRasterData, loader:((Int,Int)) => Raster) {
  val tileCounts = mutable.Map[(Int, Int), AtomicInteger]().withDefaultValue(new AtomicInteger())
  val rasterCache = mutable.Map[(Int,Int), Raster]().withDefault(loader)
  private def withinBounds(col: Int, row: Int): Boolean =
    (col >= 0 && col < tiles.cols && row >= 0 && row < tiles.rows)

  def register(tileCol: Int, tileRow: Int) {
    TileCache.neighborTuples
      .filter((n) => tiles.withinBounds(n._1, n._2))
      .foreach {
        case n => {
          tileCounts.get(n).map(_.getAndIncrement())
        }
       } 
  }
  
  //Note: This function could be rewritten to accept a loadFunction and
  //      then curry it for passing to getOrElseUpdate
  def getTile(tileCol: Int, tileRow: Int):Raster = {
    val tuple = (tileCol,tileRow)
    val raster = rasterCache.get((tileCol,tileRow)) match {
      case Some(r) => r
      case None => throw new Exception("RasterCache no longer has raster at ${tileCol},${tileRow}")
    }
    val count:Option[Int] = tileCounts.get(tuple).map(_.getAndDecrement())
    count match {
      case Some(0) => {
        rasterCache.remove((tileCol,tileRow))
      }
      case _ => 
    }
    raster
  }
  
}

object TileNeighbors {
  val NONE = new TileNeighbors {
    def n:Option[Op[Raster]] = None 
    def ne:Option[Op[Raster]] = None 
    def e:Option[Op[Raster]] = None 
    def se:Option[Op[Raster]] = None 
    def s:Option[Op[Raster]] = None 
    def sw:Option[Op[Raster]] = None 
    def w:Option[Op[Raster]] = None 
    def nw:Option[Op[Raster]] = None 

    def getNeighbors:Op[Seq[Option[Raster]]] = Literal(Seq[Option[Raster]]())
  }
}

trait TileNeighbors {
  /** North */
  def n:Option[Op[Raster]]
  /** NorthEast */
  def ne:Option[Op[Raster]]
  /** East */
  def e:Option[Op[Raster]]
  /** SoutEast */
  def se:Option[Op[Raster]]
  /** South */
  def s:Option[Op[Raster]]
  /** SouthWest */
  def sw:Option[Op[Raster]]
  /** West */
  def w:Option[Op[Raster]]
  /** NorthWest */
  def nw:Option[Op[Raster]]

  def getNeighbors:Op[Seq[Option[Raster]]]
}

/** 
 * Tile Neighbors that are represented by a sequence of neighboring tiles,
 * in the order (n,ne,e,se,s,sw,w,nw)
 */
case class SeqTileNeighbors(seq:Seq[Option[Op[Raster]]]) extends TileNeighbors {
  def n = seq(0)
  def ne = seq(1)
  def e = seq(2)
  def se = seq(3)
  def s = seq(4)
  def sw = seq(5)
  def w = seq(6)
  def nw = seq(7)

  def getNeighbors =
    if(seq.flatten.isEmpty) { Literal(Seq[Option[Raster]]()) }
    else {
      logic.Collect(
        seq.map {
          case Some(op) => op.map(Some(_))
          case None => Literal(None)
        }
      )
    }
}

object TileNeighborhood {
  // relative coordinates of neighbors
  val C  = (0, 0)   // center (focus tile)
  val UL = (-1, -1) // upper left neighbor
  val U  = ( 0, -1) // upper neighbor
  val UR = ( 1, -1) // upper right neighbor
  val R  = ( 1,  0) // right neighbor
  val DR = ( 1,  1) // down (lower) right neighbor
  val D  = ( 0,  1) // down (lower) neighbor
  val DL = (-1,  1) // down (lower) right neighbor
  val L  = (-1,  0) // left neighbor
  
  def buildTileNeighborhood(trd: TiledRasterData, re:RasterExtent, col: Int, row: Int):TileArrayRasterData = {
    val tileLayout = trd.tileLayout
    val rl = tileLayout.getResolutionLayout(re)

    val colMax = tileLayout.tileCols - 1
    val rowMax = tileLayout.tileRows - 1

    // get tileCols, tileRows, & list of relative neighbor coordinate tuples
    val (tileCols:Int, tileRows:Int, neighbors:List[(Int,Int)]) = 
      if (col == 0 && row == 0) { // top left corner
        (2,2, List(C, R, D, DR))
      } else if (col == 0 && row == rowMax) { // bottom left corner
        (2,2, List(U, UR, C, R)) 
      } else if (col == colMax && row == 0) { // top right corner
        (2,2, List(L, C, DL, D))
      } else if (col == colMax && row == rowMax) { // bottom right corner
        (2,2, List(UL, U, L, C))
      } else if (col == 0) { // left border
        (2,3, List(U, UR, C, R, D, DR))
      } else if (col == colMax) { // right border
        (2,3, List(UL, U, L, C, DL, D))
      } else if (row == 0) { // top border
        (3,2, List(L, C, R, DL, D, DR))
      } else if (row == rowMax) { // bottom border
        (3,2, List(UL, U, UR, L, C, R))
      } else {
        (3,3, List(UL, U, UR, L, C, R, DL, D, DR))
      }

    val neighborTiles = for( (colDelta, rowDelta) <- neighbors) yield { 
      val nTileCol = col + colDelta
      val nTileRow = row + rowDelta
      val nTile = trd.getTileRaster(rl, nTileCol, nTileRow)
      nTile
    }

    val (nwColD, nwRowD) = neighbors.head
    val nwExtent = rl.getExtent(col + nwColD, row + nwRowD)
    //val nwExtent = neighborTiles.head.rasterExtent.extent
    val xmin = nwExtent.xmin
    val ymax = nwExtent.ymax
    //val seExtent = neighborTiles.last.rasterExtent.extent
    val (seColD, seRowD) = neighbors.last
    val seExtent = rl.getExtent(col + seColD, row + seRowD)
    val xmax = seExtent.xmax
    val ymin = seExtent.ymin
     
    val nTileLayout = TileLayout(tileCols, tileRows, tileLayout.pixelCols, tileLayout.pixelRows)
    val extent = Extent(xmin, ymin, xmax, ymax)
    val nRasterExtent = RasterExtent(
      extent, 
      re.cellwidth, 
      re.cellheight, 
      tileLayout.pixelCols * tileCols,
      tileLayout.pixelRows * tileRows
    )
 
    val nRasterData = new TileArrayRasterData(neighborTiles.toArray, nTileLayout)
    nRasterData
  }
}
