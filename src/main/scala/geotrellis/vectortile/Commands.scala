package geotrellis.vectortile

import geotrellis.vector._

object Command {

    val MoveTo: Int = 1

    val LineTo: Int = 2

    val ClosePath: Int = 7

    type GeomType = vector_tile.Tile.GeomType.EnumVal

    def parse(geomType: GeomType, commands: Seq[Int]): Geometry = {

        // TODO finish parsing
        return null

    }

}

