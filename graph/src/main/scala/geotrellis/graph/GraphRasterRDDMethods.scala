package geotrellis.graph

import geotrellis.spark._
import geotrellis.spark.op.focal._

import geotrellis.raster._

import org.apache.spark.graphx._

import spire.syntax.cfor._

import collection.mutable.{ArrayBuffer, ListBuffer}

object GraphRasterRDDMethods {

  private val Sqrt2 = math.sqrt(2)

  @inline
  private final def getCost(tile: Tile)(
    start: (Int, Int))(
    end: (Int, Int)): Double = getCost(tile, tile)(start)(end)

  @inline
  private final def getCost(t1: Tile, t2: Tile)(
    start: (Int, Int))(
    end: (Int, Int)): Double = {
    val (sc, sr) = start
    val (ec, er) = end
    val v1 = t1.get(sc, sr)
    val v2 = t2.get(ec, er)

    val r = v1 + v2

    if (sc - ec != 0 && sr - er != 0) r / Sqrt2
    else r / 2.0
  }

  @inline
  private final def constructEdges(tile: Tile, getVertexId: (Int, Int) => Long) = {
    val (cols, rows) = tile.dimensions

    val cost = getCost(tile)(_)
    val edges = new ArrayBuffer[Edge[Double]](cols * rows * 5)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        if (tile.get(col, row) != NODATA) {
          val vertexId = getVertexId(col, row)

          val c = cost(col, row)(_)

          val isRight = col == cols - 1
          val isLeft = col == 0
          val isBottom = row == rows - 1

          if (!isRight && tile.get(col + 1, row) != NODATA) {
            val otherVertexId = getVertexId(col + 1, row)
            val edgeCost = c(col + 1, row)

            edges += Edge(vertexId, otherVertexId, edgeCost)
            edges += Edge(otherVertexId, vertexId, edgeCost)
          }

          if (!isRight && !isBottom && tile.get(col + 1, row + 1) != NODATA) {
            val otherVertexId = getVertexId(col + 1, row + 1)
            val edgeCost = c(col + 1, row + 1)

            edges += Edge(vertexId, otherVertexId, edgeCost)
            edges += Edge(otherVertexId, vertexId, edgeCost)
          }

          if (!isLeft && !isBottom && tile.get(col -1, row + 1) != NODATA) {
            val otherVertexId = getVertexId(col - 1, row + 1)
            val edgeCost = c(col - 1, row + 1)

            edges += Edge(vertexId, otherVertexId, edgeCost)
            edges += Edge(otherVertexId, vertexId, edgeCost)
          }

          if (!isBottom && tile.get(col, row + 1) != NODATA) {
            val otherVertexId = getVertexId(col, row + 1)
            val edgeCost = c(col, row + 1)

            edges += Edge(vertexId, otherVertexId, edgeCost)
            edges += Edge(otherVertexId, vertexId, edgeCost)
          }
        }
      }
    }

    edges
  }

}

trait GraphRasterRDDMethods[K] extends RasterRDDMethods[K] {

  import GraphRasterRDDMethods._

  val _sc: SpatialComponent[K]

  // TODO: Update or remove
  // def toGraph: GraphRDD[K] = {
  //   val metaData = rasterRDD.metadata
  //   val gridBounds = metaData.gridBounds
  //   val tileLayout = metaData.tileLayout

  //   val (layoutCols, layoutRows) = (gridBounds.width - 1, gridBounds.height - 1)
  //   val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)

  //   def getVertexId(layoutCol: Long, layoutRow: Long)
  //     (tileCol: Int, tileRow: Int) = ((layoutRow * tileRows + tileRow)
  //       * tileCols * layoutCols + layoutCol * tileCols + tileCol)

  //   val verticesRDD = rasterRDD.flatMap { case(key, tile) =>
  //     val SpatialKey(layoutCol, layoutRow) = key
  //     val getVertexIdForTile = getVertexId(layoutCol, layoutRow) _

  //     val buff = ListBuffer[(VertexId, Double)]()

  //     cfor(0)(_ < tile.size, _ + 1) { i =>
  //       val (tileCol, tileRow) = (i % tileCols, i / tileCols)
  //       val v = tile.get(tileCol, tileRow)

  //       if (v != NODATA) {
  //         val vertexId = getVertexIdForTile(tileCol, tileRow)
  //         buff += ((vertexId, v))
  //       }
  //     }

  //     buff
  //   }

  //   implicit val _scImplicit = _sc
  //   val edgesRDD = rasterRDD.zipWithNeighbors.flatMap { case(key, tile, neighbors) =>
  //     val SpatialKey(layoutCol, layoutRow) = key
  //     val getVertexIdForTile = getVertexId(layoutCol, layoutRow) _

  //     val edges = constructEdges(tile, getVertexIdForTile)

  //     if (tile.get(0, tileRows - 1) != NODATA)
  //       neighbors.sw match {
  //         case Some(sw) if (sw.get(tileCols -1, 0) != NODATA) => {
  //           val vertexId = getVertexIdForTile(0, tileRows - 1)
  //           val otherVertexId = getVertexId(layoutCol - 1, layoutRow + 1)(tileCols - 1, 0)

  //           val cost = getCost(tile, sw)(0, tileRows - 1)(tileCols - 1, 0)

  //           edges += Edge(vertexId, otherVertexId, cost)
  //           edges += Edge(otherVertexId, vertexId, cost)
  //         }
  //         case _ =>
  //       }

  //     if (tile.get(tileCols - 1, tileRows - 1) != NODATA)
  //       neighbors.se match {
  //         case Some(se) if (se.get(0, 0) != NODATA) => {
  //           val vertexId = getVertexIdForTile(tileCols - 1, tileRows - 1)
  //           val otherVertexId = getVertexId(layoutCol + 1, layoutRow + 1)(0, 0)

  //           val cost = getCost(tile, se)(tileCols - 1, tileRows - 1)(0, 0)

  //           edges += Edge(vertexId, otherVertexId, cost)
  //           edges += Edge(otherVertexId, vertexId, cost)
  //         }
  //         case _ =>
  //       }

  //     neighbors.e match {
  //       case Some(e) => {
  //         val cost = getCost(tile, e) _
  //         val getVertexIdForOtherTile = getVertexId(layoutCol + 1, layoutRow) _

  //         cfor(0)(_ < tileRows, _ + 1) { row =>
  //           if (tile.get(tileCols - 1, row) != NODATA) {
  //             val vertexId = getVertexIdForTile(tileCols - 1, row)

  //             val c = cost(tileCols - 1, row)(_)

  //             val isTop = row == 0
  //             val isBottom = row == tileRows - 1

  //             if (!isTop && e.get(0, row - 1) != NODATA) {
  //               val otherVertexId = getVertexIdForOtherTile(0, row - 1)
  //               val edgeCost = c(0, row - 1)

  //               edges += Edge(vertexId, otherVertexId, edgeCost)
  //               edges += Edge(otherVertexId, vertexId, edgeCost)
  //             }

  //             if (!isBottom && e.get(0, row + 1) != NODATA) {
  //               val otherVertexId = getVertexIdForOtherTile(0, row + 1)
  //               val edgeCost = c(0, row + 1)

  //               edges += Edge(vertexId, otherVertexId, edgeCost)
  //               edges += Edge(otherVertexId, vertexId, edgeCost)
  //             }

  //             if (e.get(0, row) != NODATA) {
  //               val otherVertexId = getVertexIdForOtherTile(0, row)
  //               val edgeCost = c(0, row)

  //               edges += Edge(vertexId, otherVertexId, edgeCost)
  //               edges += Edge(otherVertexId, vertexId, edgeCost)
  //             }
  //           }
  //         }
  //       }
  //       case _ =>
  //     }

  //     neighbors.s match {
  //       case Some(s) => {
  //         val cost = getCost(tile, s)(_)
  //         val getVertexIdForOtherTile = getVertexId(layoutCol, layoutRow + 1) _

  //         cfor(0)(_ < tileCols, _ + 1) { col =>
  //           if (tile.get(col, tileRows - 1) != NODATA) {
  //             val vertexId = getVertexIdForTile(col, tileRows - 1)

  //             val c = cost(col, tileRows - 1)(_)

  //             val isLeft = col == 0
  //             val isRight = col == tileCols - 1

  //             if (!isLeft && s.get(col - 1, 0) != NODATA) {
  //               val otherVertexId = getVertexIdForOtherTile(col - 1, 0)
  //               val edgeCost = c(col - 1, 0)

  //               edges += Edge(vertexId, otherVertexId, edgeCost)
  //               edges += Edge(otherVertexId, vertexId, edgeCost)
  //             }

  //             if (!isRight && s.get(col + 1, 0) != NODATA) {
  //               val otherVertexId = getVertexIdForOtherTile(col + 1, 0)
  //               val edgeCost = c(col + 1, 0)

  //               edges += Edge(vertexId, otherVertexId, edgeCost)
  //               edges += Edge(otherVertexId, vertexId, edgeCost)
  //             }

  //             if (s.get(col, 0) != NODATA) {
  //               val otherVertexId = getVertexIdForOtherTile(col, 0)
  //               val edgeCost = c(col, 0)

  //               edges += Edge(vertexId, otherVertexId, edgeCost)
  //               edges += Edge(otherVertexId, vertexId, edgeCost)
  //             }
  //           }
  //         }
  //       }
  //       case _ =>
  //     }

  //     edges
  //   }

  //   val graph = Graph(verticesRDD, edgesRDD)
  //   val keysRDD = rasterRDD.map { case(key, tile) => key }
  //   new GraphRDD(graph, keysRDD, rasterRDD.metaData)
  // }

}
