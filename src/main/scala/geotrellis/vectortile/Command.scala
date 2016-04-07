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

    case class UnsupportedGeomType(message: String) extends Exception(message)
    case class UnsupportedCommand(message: String) extends Exception(message)
    case class TooFewCommandArgs(message: String) extends Exception(message)
    case class NoGeometry(message:String) extends Exception(message)

    def parse(geomType: GeomType, extent: Int, commands: Seq[Int]): Geometry = {

        val scale: Double = extent / 256.0
        var point_lists: List[List[(Double, Double)]] =
            interpret_commands(scale, commands)

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
                    point_lists = point_lists.filter(_.length >= 2)
                    MultiLine(point_lists.map(
                        (ln: List[(Double, Double)]) => Line(ln)))
                }

            case POLYGON =>
                if (point_lists.length == 1) {
                    Polygon(point_lists.head)
                } else {
                    MultiPolygon(point_lists.map(
                        (pg: List[(Double, Double)]) => Polygon(pg)))
                }

            case _ =>
                throw UnsupportedGeomType("Geometry ${geomType} not supported.")

        }

    }

    private def interpret_commands(scale: Double, commands: Seq[Int]):
    List[List[(Double, Double)]] = {

        val point_lists: ListBuffer[List[(Double, Double)]] =
            ListBuffer.empty[List[(Double, Double)]]
        val point_list: ListBuffer[(Double, Double)] =
            ListBuffer.empty[(Double, Double)]
        var (x: Int, y: Int) = (0, 0)
        var idx: Int = 0

        def zigZagDecode(n: Int) = ((n >> 1) ^ (-(n & 1)))

        while(idx < commands.length) {

            var command = commands(idx)
            var (id, count) = (command & 0x7, command >> 3)
            idx += 1

            id match {

                case MoveTo =>
                    for(_ <- 0 until count) {
                        if (!point_list.isEmpty) {
                            point_lists += point_list.toList
                            point_list.clear
                        }
                        if (idx + 2 > commands.length) {
                            throw TooFewCommandArgs("Source: MoveTo command.")
                        }
                        x += zigZagDecode(commands(idx))
                        y += zigZagDecode(commands(idx+1))
                        point_list += ((x / scale, y / scale))
                    }
                    idx += 2

                case LineTo =>
                    for(_ <- 0 until count) {
                        if (point_list.isEmpty) {
                            throw NoGeometry("Source: LineTo")
                        }
                        if (idx + 2 > commands.length) {
                            throw TooFewCommandArgs("Source: MoveTo command.")
                        }
                        val dx = zigZagDecode(commands(idx))
                        val dy = zigZagDecode(commands(idx+1))
                        x += dx
                        y += dy
                        if (dx != 0 || dy != 0)
                            point_list += ((x / scale, y / scale))
                    }
                    idx += 2

                case ClosePath =>
                    // TODO is it valid to close a path numerous times?
                    for(_ <- 0 until count) {
                        if (point_list.isEmpty) {
                                // this is unexpected and should be logged
                            } else {
                                if (point_list.head != point_list.last) {
                                    point_list += point_list.head
                                }
                                point_lists += point_list.toList
                                point_list.clear
                            }
                    }

                case _ =>
                    throw UnsupportedCommand(
                        "Unsupported Command ID: ${id}")

            }

        }

        if (!point_list.isEmpty) { point_lists += point_list.toList }

        return point_lists.toList

    }

}

