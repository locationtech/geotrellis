package geotrellis.vectortile

import collection.mutable.ListBuffer

import geotrellis.vector._

/** Interprets the commands from a VectorTile and converts them into
  * Geometries.
  */
object Command {

    val MoveTo: Int = 1
    val LineTo: Int = 2
    val ClosePath: Int = 7

    type GeomType = VectorTile.GeomType
    val POINT = VectorTile.POINT
    val LINESTRING = VectorTile.LINESTRING
    val POLYGON = VectorTile.POLYGON

    case class UnsupportedGeomType(message: String) extends Exception(message)
    case class UnsupportedCommand(message: String) extends Exception(message)
    case class TooFewCommandArgs(message: String) extends Exception(message)
    case class NoGeometryToExtend(message: String) extends Exception(message)

    /** Interprets the commands, converts the resulting data into a geometry,
      * then returns the geometry.
      *
      * @param geomType the type of geometry to expect
      * @param extent the extent of the geometry
      * @param commands the list of commands and arguments to interpret
      * @return the geometry that was described
      */
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
                throw UnsupportedGeomType(s"Geometry ${geomType} not supported.")

        }

    }

    /** A helper function for parse. Builds lists of points out of the
      * commands and arguments.
      *
      * @param scale the scale of the geometry
      * @param commands the commands to interpet
      * @return a list of lists of points
      */
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
                        idx += 2
                    }

                case LineTo =>
                    for(_ <- 0 until count) {
                        if (point_list.isEmpty) {
                            throw NoGeometryToExtend("Source: LineTo")
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
                        idx += 2
                    }

                case ClosePath =>
                    for(_ <- 0 until count) {
                        if (point_list.isEmpty) {
                                throw NoGeometryToExtend("Source: ClosePath")
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
                        s"Unsupported Command ID: ${id}")

            }

        }

        if (!point_list.isEmpty) { point_lists += point_list.toList }

        return point_lists.toList

    }

}

