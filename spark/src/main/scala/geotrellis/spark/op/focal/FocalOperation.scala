package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.focal._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

import spire.syntax.cfor._

import annotation.tailrec

// TODO: after tile gridbounds are correct change to inclusive
trait FocalOperation[K] extends RasterRDDMethods[K] {

  val _sc: SpatialComponent[K]

  def zipWithNeighbors: RDD[(K, Tile, TileNeighbors)] = {
    val sc = rasterRDD.sparkContext
    val bcMetadata = sc.broadcast(rasterRDD.metaData)
    val tilesWithNeighborIndices = rasterRDD.map { case (key, tile) =>
      val metadata = bcMetadata.value
      val gridBounds = metadata.gridBounds
      val SpatialKey(col, row) = key

      (key, tile, getTileNeighbors(gridBounds, col, row))
    }

    fetchTiles(tilesWithNeighborIndices)
  }

  private def coordsToIndex(col: Int, row: Int) =
    (row % 3) * 3 + (col % 3)

  private def getTileNeighbors(gridBounds: GridBounds, col: Int, row: Int) = {

    val index = coordsToIndex(col, row)

    val neighborCoordinates = Array.ofDim[Option[(Int, Int)]](9)
    cfor(0)(_ < 9, _ + 1) { i =>
      val (dy, dx) = indicesDifferenceToCoords(index, i)
      val (r, c) = (row + dy, col + dx)

      neighborCoordinates(i) =
        if (c > gridBounds.width || r > gridBounds.height || c < 0 || r < 0) None
        else Some((r, c))
    }

    neighborCoordinates.toSeq
  }

  private def indicesDifferenceToCoords(from: Int, to: Int) = {
    val xStart = if (from % 3 == 0) 0 else if (from % 3 == 1) -1 else 1
    val yStart = if (from / 3 == 0) 0 else if (from / 3 == 1) -1 else 1

    val y = (yStart + to / 3) % 3 match {
      case 2 => -1
      case y => y
    }

    val x = (xStart + to % 3) % 3 match {
      case 2 => -1
      case x => x
    }

    (y, x)
  }

  private def fetchTiles(
    rdd: RDD[(K, Tile, Seq[Option[(Int, Int)]])],
    idx: Int = 0): RDD[(K, Tile, TileNeighbors)] = {
    val sc = rdd.sparkContext
    if (idx == 9) sc.parallelize(Seq[(K, Tile, TileNeighbors)]())
    else {
      val bcIdx = sc.broadcast(idx)
      val bcMetadata = sc.broadcast(rasterRDD.metaData)

      rdd.groupBy { case(key, tile, seq) => seq(bcIdx.value) }
        .filter { case(k, it) => !k.isEmpty }
        .map { case(k, it) => (k.get, it) }
        .map { case((row, col), seq) =>
          val index = bcIdx.value
          val gridBounds = bcMetadata.value.gridBounds

          val neighborMap: Map[(Int, Int), (K, Tile)] =
            seq.map { case(k, t, _) =>
              val SpatialKey(col, row) = k
              (k, t, (row, col))
            }.groupBy(_._3).map(t => {
              val k = t._1
              val v = t._2.head
              (k, (v._1, v._2))
            })

          val (key, tile) = neighborMap((row, col))

          val tileNeighborsSeq = Seq(
            (-1, 0), (-1, 1), (0, 1), (1, 1),
            (1, 0), (1, -1), (0, -1), (-1, -1)
          ).map { case(dy, dx) =>
              neighborMap.get((row + dy, col + dx)).map(_._2)
          }

          val tileNeighbors: TileNeighbors = SeqTileNeighbors(tileNeighborsSeq)

          (key, tile, tileNeighbors)
      } ++ fetchTiles(rdd, idx + 1)
    }
  }

  def focal(n: Neighborhood)
    (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RasterRDD[K] = {
    val sc = rasterRDD.sparkContext
    val bcCalc = sc.broadcast(calc)
    val bcNeighborhood = sc.broadcast(n)

    val rdd = zipWithNeighbors.map { case (key, center, neighbors) =>
      val calc = bcCalc.value
      val neighborhood = bcNeighborhood.value

      val (neighborhoodTile, analysisArea) =
        TileWithNeighbors(center, neighbors.getNeighbors)
      (key, calc(neighborhoodTile, neighborhood, Some(analysisArea)))
    }

    new RasterRDD(rdd, rasterRDD.metaData)
  }

  def focalWithExtent(n: Neighborhood)
    (calc: (Tile, Neighborhood, Option[GridBounds], RasterExtent) => Tile): RasterRDD[K] = {
    val sc = rasterRDD.sparkContext
    val bcCalc = sc.broadcast(calc)
    val bcNeighborhood = sc.broadcast(n)
    val bcMetadata = sc.broadcast(rasterRDD.metaData)

    val rdd = zipWithNeighbors.map { case (key, center, neighbors) =>
      val calc = bcCalc.value
      val neighborhood = bcNeighborhood.value
      val metadata = bcMetadata.value

      val (neighborhoodTile, analysisArea) =
        TileWithNeighbors(center, neighbors.getNeighbors)

      val res = calc(
        neighborhoodTile,
        neighborhood,
        Some(analysisArea),
        metadata.rasterExtent
      )

      (key, res)
    }

    new RasterRDD(rdd, rasterRDD.metaData)
  }
}
