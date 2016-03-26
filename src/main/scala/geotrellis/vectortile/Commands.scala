package geotrellis.vectortile

import collection.mutable.ListBuffer

import geotrellis.vector._

object Command {

    val MoveTo: Int = 1

    val LineTo: Int = 2

    val ClosePath: Int = 7

    type GeomType = vector_tile.Tile.GeomType.EnumVal
    val POINT = vector_tile.Tile.GeomType.POINT
    val LINESTRING = vector_tile.Tile.GeomType.LINESTRING
    val POLYGON = vector_tile.Tile.GeomType.POLYGON

    def parse(geomType: GeomType, extent: Int, commands: Seq[Int]): Geometry = {

        val scale: Double = extent / 256.0

        val point_lists: ListBuffer[List[(Double, Double)]] =
            ListBuffer.empty[List[(Double, Double)]]

        interpret_commands()

        return geomType match {

            case POINT =>
                if (point_lists.length == 1) {
                    Point(point_lists.head.head)
                } else {
                    MultiPoint(point_lists.map(
                        (pt: List[(Double, Double)]) => Point(pt.head)))
                }

            case LINESTRING =>
                if (point_lists.length == 1) {
                    Line(point_lists.head)
                } else {
                    MultiLine(point_lists.map(
                        (ln: List[(Double, Double)]) => Line(ln)))
                }

            case POLYGON =>
                if (point_lists.length == 1) {
                    println(point_lists)
                    Polygon(point_lists.head)
                } else {
                    MultiPolygon(point_lists.map(
                        (pg: List[(Double, Double)]) => Polygon(pg)))
                }

            case _ =>
                // this should probably be logged.
                null

        }


        def interpret_commands() {

            val point_list: ListBuffer[(Double, Double)] =
                ListBuffer.empty[(Double, Double)]
            var (x: Int, y: Int) = (0, 0)
            var idx: Int = 0

            def zigZagDecode(n: Int) = ((n >> 1) ^ (-(n & 1)))

            while(idx < commands.length) {

                var command = commands(idx)
                var (id, count) = (command & 0x7, command >> 3)
                idx += 1

                for(_ <- 0 until count) {

                    id match {

                        case MoveTo =>
                            if (!point_list.isEmpty) {
                                point_lists += point_list.toList
                                point_list.clear
                            }
                            x += zigZagDecode(commands(idx))
                            y += zigZagDecode(commands(idx+1))
                            idx += 2
                            point_list += ((x / scale, y / scale))

                        case LineTo =>
                            if (point_list.isEmpty) {
                                // this is unexpected and should be logged
                            }
                            x += zigZagDecode(commands(idx))
                            y += zigZagDecode(commands(idx+1))
                            idx += 2
                            point_list += ((x / scale, y / scale))

                        case ClosePath =>
                            if (point_list.isEmpty) {
                                // this is unexpected and should be logged
                            } else {
                                point_list += point_list.head
                                point_lists += point_list.toList
                                point_list.clear
                            }

                    }

                }

            }

            if (!point_list.isEmpty) { point_lists += point_list.toList }

        }

        return null

    }

}

