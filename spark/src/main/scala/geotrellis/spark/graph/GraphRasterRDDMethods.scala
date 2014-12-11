package geotrellis.spark.graph

import geotrellis.spark._
import geotrellis.spark.op.focal._

import geotrellis.raster._

import org.apache.spark.graphx._

import spire.syntax.cfor._

import collection.mutable.ArrayBuffer

object GraphRasterRDDMethods {

  private val Sqrt2 = math.sqrt(2)

  private def getCost(tile: Tile)(start: (Int, Int))(end: (Int, Int)): Int =
    getCost(tile, tile)(start)(end)

  private def getCost(t1: Tile, t2: Tile)(start: (Int, Int))(end: (Int, Int)): Int = {
    val (sc, sr) = start
    val (ec, er) = end
    val r = t1.get(sc, sr) + t2.get(ec, er)

    if (math.abs(sc - ec) == 1 && math.abs(sr - er) == 1) (r / Sqrt2).toInt
    else r / 2
  }

  private def constructEdges(offset: Long, tile: Tile) = {
    val (cols, rows) = tile.dimensions

    val cost = getCost(tile)(_)
    val edges = new ArrayBuffer[Edge[Int]](cols * rows * 5)
    var vertexId = offset
    cfor(0)(_ < rows - 1, _ + 1) { i =>
      cfor(0)(_ < cols, _ + 1) { j =>
        val c = cost(j, i)
        if (j != cols - 1) {
          edges += Edge(vertexId, vertexId + 1, c(j + 1, i))
          edges += Edge(vertexId, vertexId + 1 + cols, c(j + 1, i + 1))
        }

        if (j != 0)
          edges += Edge(vertexId, vertexId - 1 + cols, c(j - 1, i + 1))

        edges += Edge(vertexId, vertexId + cols, c(j, i + 1))

        vertexId += 1
      }
    }

    edges
  }

}

trait GraphRasterRDDMethods[K] extends RasterRDDMethods[K] {

  import GraphRasterRDDMethods._

  val _sc: SpatialComponent[K]

  // Nodata?
  def toGraph: Graph[K, Int] = {
    val metaData = rasterRDD.metaData
    val gridBounds = metaData.gridBounds
    val tileLayout = metaData.tileLayout

    val (layoutCols, layoutRows) = (gridBounds.width - 1, gridBounds.height - 1)
    val (cols, rows) = tileLayout.tileDimensions
    val area = cols * rows

    def getOffset[K](key: K) = {
      val SpatialKey(col, row) = key
      getOffsetByColAndRow(col, row)
    }

    def getOffsetByColAndRow(col: Long, row: Long) =
      area * layoutCols * row + area * col

    val verticesRDD = rasterRDD.map { case(key, tile) =>
      val offset = getOffset(key)
      val vertices = Array.ofDim[(VertexId, K)](area)
      cfor(0)(_ < area, _ + 1) { i => vertices(i) = (offset + 1, key) }
      vertices
    }.flatMap(x => x)

    implicit val _scImplicit = _sc
    val edgesRDD = rasterRDD.zipWithNeighbors.map { case(key, tile, neighbors) =>
      val SpatialKey(col, row) = key
      val upperLeftOffset = getOffsetByColAndRow(col, row)

      val edges = constructEdges(upperLeftOffset, tile)

      val lowerLeftOffset = upperLeftOffset + (rows - 1) * cols

      neighbors.sw match {
        case Some(t) => {
          val otherOffset = getOffsetByColAndRow(col - 1, row + 1) + cols - 1
          edges += Edge(
            lowerLeftOffset,
            otherOffset,
            getCost(tile, t)(0, rows - 1)(cols - 1, 0)
          )
        }
        case _ =>
      }

      val lowerRightOffset = upperLeftOffset + area - 1

      neighbors.se match {
        case Some(t) => {
          val otherOffset = getOffsetByColAndRow(col + 1, row + 1)
          edges += Edge(
            lowerRightOffset,
            otherOffset,
            getCost(tile, t)(cols - 1, rows - 1)(0, 0)
          )
        }
        case _ =>
      }

      val upperRightOffset = upperLeftOffset + cols - 1

      neighbors.e match {
        case Some(t) => {
          val otherOffset = getOffsetByColAndRow(col + 1, row)
          val cost = getCost(tile, t)(_)
          cfor(0)(_ < rows, _ + 1) { i =>
            val cOff = upperRightOffset + i * cols
            val c = cost(cols - 1, i)(_)
            if (i != 0)
              edges += Edge(cOff, otherOffset + (i - 1) * cols, c(0, i - 1))

            if (i != rows - 1)
              edges += Edge(cOff, otherOffset + (i + 1) * cols, c(0, i + 1))

            edges += Edge(cOff, otherOffset + i * cols, c(0, i))
          }
        }
        case _ =>
      }

      neighbors.s match {
        case Some(t) => {
          val otherOffset = getOffsetByColAndRow(col, row + 1)
          val cost = getCost(tile, t)(_)
          cfor(0)(_ < cols, _ + 1) { i =>
            val cOff = lowerLeftOffset + i
            val c = cost(i, rows - 1)(_)
            if (i != 0)
              edges += Edge(cOff, otherOffset + i - 1, c(i - 1, 0))

            if (i != rows - 1)
              edges += Edge(cOff, otherOffset + i + 1, c(i + 1, 0))

            edges += Edge(cOff, otherOffset + i, c(i, 0))
          }
        }
        case _ =>
      }

      edges.toArray
    }.flatMap(x => x)

    Graph(verticesRDD, edgesRDD)
  }

}
