package geotrellis.raster.op.focal

import geotrellis.raster._
import geotrellis.engine._

trait FocalOperation {self: RasterSource =>

  def zipWithNeighbors: Op[Seq[(Op[Tile], TileNeighbors)]] =
    (self.tiles, self.rasterDefinition).map { (seq, rd) =>
      val tileLayout = rd.tileLayout

      val colMax = tileLayout.tileCols - 1
      val rowMax = tileLayout.tileRows - 1

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

        (tile, SeqTileNeighbors(tileSeq))
      }
    }

  protected
  def focal(n: Neighborhood)
           (calc: (Tile, Neighborhood, Option[GridBounds]) => FocalCalculation[Tile]) =
  {
    val tileOps: Op[Seq[Op[Tile]]] =
      zipWithNeighbors.map{ //map into the Op
        _.map { case (t: Op[Tile], ns: TileNeighbors) => //map over every Op[Tile] and their neighbors

          //Now we're mapping into tile and it's neighbors, in parallel
          (t, ns.getNeighbors).map { case (center: Tile, neighbors: Seq[Option[Tile]]) =>
            val (neighborhoodTile, analysisArea) = TileWithNeighbors(center, neighbors)
            calc(neighborhoodTile, n, Some(analysisArea)).execute()
          }
        }
      }

    new RasterSource(rasterDefinition, tileOps)
  }

  protected
  def focalWithExtent(n: Neighborhood)
                     (calc: (Tile, Neighborhood, Option[GridBounds], RasterExtent) => FocalCalculation[Tile]) =
  {
    val tileOps: Op[Seq[Op[Tile]]] =
      (rasterDefinition, zipWithNeighbors).map{ case (rd, tiles) =>
        tiles.map { case (t: Op[Tile], ns: TileNeighbors) => //map over every Op[Tile] and their neighbors
          //Now we're mapping into tile and it's neighbors, in parallel
          (t, ns.getNeighbors).map { case (center: Tile, neighbors: Seq[Option[Tile]]) =>
            val (neighborhoodTile, analysisArea) = TileWithNeighbors(center, neighbors)
            //TODO - here we get the full RasterExtent, should it be RasterExtent of the tile/neighborhoodTile ?
            calc(neighborhoodTile, n, Some(analysisArea), rd.rasterExtent).execute()
          }
        }
      }

    new RasterSource(rasterDefinition, tileOps)
  }
}
