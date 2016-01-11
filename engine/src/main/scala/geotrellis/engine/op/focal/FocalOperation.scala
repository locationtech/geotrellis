package geotrellis.engine.op.focal

import geotrellis.engine._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.op.focal._

trait FocalOperation extends RasterSourceMethods {

  def zipWithNeighbors: Op[Seq[(Op[Raster[Tile]], TileNeighbors)]] =
    (rasterSource.tiles, rasterSource.rasterDefinition).map { (seq, rd) =>
      val tileLayout = rd.tileLayout
      val tileExtents = rd.tileExtents

      val colMax = tileLayout.layoutCols - 1
      val rowMax = tileLayout.layoutRows - 1

      def getTile(tileCol: Int, tileRow: Int): Option[Op[Tile]] =
        if(0 <= tileCol && tileCol <= colMax &&
          0 <= tileRow && tileRow <= rowMax) {
          Some(seq(tileRow * (colMax + 1) + tileCol))
        } else { None }

      seq.zipWithIndex.map { case (tile, i) =>
        val col = i % (colMax + 1)
        val row = i / (colMax + 1)

        // get tileCols, tileRows, & list of relative neighbor coordinate tuples
        val tileSeq = Seq(
          /* North */
          getTile(col, row - 1),
          /* NorthEast */
          getTile(col + 1, row - 1),
          /* East */
          getTile(col + 1, row),
          /* SouthEast */
          getTile(col + 1, row + 1),
          /* South */
          getTile(col, row + 1),
          /* SouthWest */
          getTile(col - 1, row + 1),
          /* West */
          getTile(col - 1, row),
          /* NorthWest */
          getTile(col - 1, row - 1)
        )

        (tile.map(Raster(_, tileExtents(i))), SeqTileNeighbors(tileSeq))
      }
    }

  protected
  def focal(n: Neighborhood)
           (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile) =
  {
    val tileOps: Op[Seq[Op[Tile]]] =
      zipWithNeighbors.map{ //map into the Op
        _.map { case (t: Op[Raster[Tile]], ns: TileNeighbors) => //map over every Op[Tile] and their neighbors

          //Now we're mapping into tile and it's neighbors, in parallel
          (t, ns.getNeighbors).map { case (Raster(center: Tile, _), neighbors: Seq[Option[Tile]]) =>
            val (neighborhoodTile, analysisArea) = TileWithNeighbors(center, neighbors)
            calc(neighborhoodTile, n, Some(analysisArea))
          }
        }
      }

    new RasterSource(rasterSource.rasterDefinition, tileOps)
  }

  protected
  def focalWithExtent(n: Neighborhood)
                     (calc: (Tile, Neighborhood, Option[GridBounds], RasterExtent) => Tile) =
  {
    val tileOps: Op[Seq[Op[Tile]]] =
      (rasterSource.rasterDefinition, zipWithNeighbors).map{ case (rd, rastersWithNeighbors) =>
        rastersWithNeighbors.map { case (r: Op[Raster[Tile]], ns: TileNeighbors) => //map over every Op[Tile] and their neighbors
          //Now we're mapping into tile and it's neighbors, in parallel
          (r, ns.getNeighbors).map { case (Raster(center: Tile, extent: Extent), neighbors: Seq[Option[Tile]]) =>
            val (neighborhoodTile, analysisArea) = TileWithNeighbors(center, neighbors)
            calc(neighborhoodTile, n, Some(analysisArea), RasterExtent(extent, center.cols, center.rows))
          }
        }
      }

    new RasterSource(rasterSource.rasterDefinition, tileOps)
  }
}
