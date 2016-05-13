package geotrellis.vectortile

import org.scalatest._

import geotrellis.vector._

class CommandTest extends FunSuite {

    val EXTENT = 4096
    val SCALE = EXTENT / 256

    val POINT = VectorTile.POINT
    val LINESTRING = VectorTile.LINESTRING
    val POLYGON = VectorTile.POLYGON

    val MoveTo = Command.MoveTo // 1
    val LineTo = Command.LineTo // 2
    val ClosePath = Command.ClosePath // 7

    def commandEncode(id: Int, count: Int) = (id & 0x7) | (count << 3)
    def zigZagEncode(n: Int) = ((n << 1) ^ (n >> 31))
    val toPoint: ((Int, Int)) => Point = {
        case (x: Int, y: Int) => Point(x.toDouble, y.toDouble)
    }

    test("Invalid ID") {
        for(id <- 0 until 0x7) {
            if (!(id == MoveTo || id == LineTo || id == ClosePath)) {
                val commands = Seq[Int](
                    commandEncode(id, count = 1)
                )
                intercept[Command.UnsupportedCommand] {
                    Command.parse(POINT, EXTENT, commands)
                }
            }
        }
    }

    test("Valid MoveTo") {
        val coords = Seq[(Int, Int)](
            (20, 50)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE)
        )
        val geometry = Command.parse(POINT, EXTENT, commands)
        assert(geometry == Point(coords(0)._1, coords(0)._2))
    }

    test("Invalid MoveTo: Insufficient Args 0") {
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1)
        )
        intercept[Command.TooFewCommandArgs] {
            val geometry = Command.parse(POINT, EXTENT, commands)
        }
    }

    test("Invalid MoveTo: Insufficient Args 1") {
        val coords = Seq[(Int, Int)](
            (20, 50)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE)
        )
        intercept[Command.TooFewCommandArgs] {
            val geometry = Command.parse(POINT, EXTENT, commands)
        }
    }

    test("Valid Multiple MoveTo") {
        val coords = Seq[(Int, Int)](
            (0, 0),
            (20, 50)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE),
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(1)._1*SCALE),
            zigZagEncode(coords(1)._2*SCALE)
        )
        val geometry = Command.parse(POINT, EXTENT, commands)
        assert(geometry == MultiPoint(
            Point(coords(0)._1, coords(0)._2),
            Point(coords(1)._1, coords(1)._2)
        ))
    }

    test("Valid Iterated MoveTo") {
        val coords = Seq[(Int, Int)](
            (20, 50)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 2),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE)
        )
        val geometry = Command.parse(POINT, EXTENT, commands)
        assert(geometry == MultiPoint(
            Point(coords(0)._1, coords(0)._2),
            Point(coords(0)._1*2, coords(0)._2*2)
        ))
    }

    test("Invalid Multiple MoveTo: Insufficient Args 0") {
        val coords = Seq[(Int, Int)](
            (20, 50)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 2)
        )
        intercept[Command.TooFewCommandArgs] {
            val geometry = Command.parse(POINT, EXTENT, commands)
        }
    }

    test("Invalid Multiple MoveTo: Insufficient Args 1") {
        val coords = Seq[(Int, Int)](
            (20, 50)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 2),
            zigZagEncode(coords(0)._1*SCALE)
        )
        intercept[Command.TooFewCommandArgs] {
            val geometry = Command.parse(POINT, EXTENT, commands)
        }
    }

    test("Valid LineTo") {
        val coords = Seq[(Int, Int)](
            (0, 0),
            (5, 0)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)._1*SCALE),
            zigZagEncode(coords(1)._2*SCALE)
        )
        val geometry = Command.parse(LINESTRING, EXTENT, commands)
        assert(geometry == Line(coords.map(c => toPoint(c))))
    }

    test("Valid Iterated LineTo") {
        val coords = Seq[(Int, Int)](
            (0, 0),
            (5, 0)
        )
        val expected = Seq[(Int, Int)](
            (0, 0),
            (5, 0),
            (10, 0),
            (15, 0)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE),
            commandEncode(id = LineTo, count = 3),
            zigZagEncode(coords(1)._1*SCALE),
            zigZagEncode(coords(1)._2*SCALE),
            zigZagEncode(coords(1)._1*SCALE),
            zigZagEncode(coords(1)._2*SCALE),
            zigZagEncode(coords(1)._1*SCALE),
            zigZagEncode(coords(1)._2*SCALE)
        )
        val geometry = Command.parse(LINESTRING, EXTENT, commands)
        assert(geometry == Line(expected.map(c => toPoint(c))))
    }

    test("Invalid Line: Insufficient Args 0") {
        val coords = Seq[(Int, Int)](
            (0, 0),
            (5, 0)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1)
        )
        intercept[Command.TooFewCommandArgs] {
            val geometry = Command.parse(POINT, EXTENT, commands)
        }
    }

    test("Invalid Line: Insufficient Args 1") {
        val coords = Seq[(Int, Int)](
            (0, 0),
            (5, 0)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)._1*SCALE)
        )
        intercept[Command.TooFewCommandArgs] {
            val geometry = Command.parse(POINT, EXTENT, commands)
        }
    }

    test("Invalid Line: No Geometry To Extend") {
        val coords = Seq[(Int, Int)](
            (5, 0)
        )
        val commands = Seq[Int](
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE)
        )
        intercept[Command.NoGeometryToExtend] {
            val geometry = Command.parse(POINT, EXTENT, commands)
        }
    }

    test("Valid MultiLine") {
        val coords = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5)),
            Seq((-5, -5),
                (0, 5),
                (5, -5))
        )
        val expected = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5)),
            Seq((0, 5),
                (5, 0))
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)(0)._1*SCALE),
            zigZagEncode(coords(0)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(1)._1*SCALE),
            zigZagEncode(coords(0)(1)._2*SCALE),
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(1)(0)._1*SCALE),
            zigZagEncode(coords(1)(0)._2*SCALE),
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(1)(1)._1*SCALE),
            zigZagEncode(coords(1)(1)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)(2)._1*SCALE),
            zigZagEncode(coords(1)(2)._2*SCALE)
        )
        val geometry = Command.parse(LINESTRING, EXTENT, commands)
        assert(geometry == MultiLine(expected.map(
            cs => Line(cs.map(c => toPoint(c))))))
    }

    test("Invalid MultiLine: Insufficient Args 0") {
        val coords = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5)),
            Seq((-5, -5),
                (5, -5))
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)(0)._1*SCALE),
            zigZagEncode(coords(0)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(1)._1*SCALE),
            zigZagEncode(coords(0)(1)._2*SCALE),
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(1)(0)._1*SCALE),
            zigZagEncode(coords(1)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1)
        )
        intercept[Command.TooFewCommandArgs] {
            val geometry = Command.parse(LINESTRING, EXTENT, commands)
        }
    }

    test("Invalid MultiLine: Insufficient Args 1") {
        val coords = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5)),
            Seq((-5, -5),
                (5, -5))
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)(0)._1*SCALE),
            zigZagEncode(coords(0)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(1)._1*SCALE),
            zigZagEncode(coords(0)(1)._2*SCALE),
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(1)(0)._1*SCALE),
            zigZagEncode(coords(1)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)(1)._1*SCALE)
        )
        intercept[Command.TooFewCommandArgs] {
            val geometry = Command.parse(LINESTRING, EXTENT, commands)
        }
    }

    test("Valid ClosePath") {
        val coords = Seq[(Int, Int)](
            (0, 0),
            (0, 5),
            (5, 0),
            (0, -5)
        )
        val expected = Seq[(Int, Int)](
            (0, 0),
            (0, 5),
            (5, 5),
            (5, 0),
            (0, 0)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)._1*SCALE),
            zigZagEncode(coords(1)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(2)._1*SCALE),
            zigZagEncode(coords(2)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(3)._1*SCALE),
            zigZagEncode(coords(3)._2*SCALE),
            commandEncode(id = ClosePath, count = 1)
        )
        val geometry = Command.parse(POLYGON, EXTENT, commands)
        assert(geometry == Polygon(expected.map(c => toPoint(c))))
    }

    test("Invalid ClosePath: No Geometry To Close") {
        val commands = Seq[Int](
            commandEncode(id = ClosePath, count = 1)
        )
        intercept[Command.NoGeometryToExtend] {
            val geometry = Command.parse(POLYGON, EXTENT, commands)
        }
    }

    test("Invalid ClosePath: Bad Polygon Line") {
        val coords = Seq[(Int, Int)](
            (0, 0),
            (0, 5)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)._1*SCALE),
            zigZagEncode(coords(1)._2*SCALE),
            commandEncode(id = ClosePath, count = 1)
        )
        try {
            val geometry = Command.parse(POLYGON, EXTENT, commands)
            assert(false)
        } catch {
            case _: Throwable => assert(true)
        }
    }

    test("Invalid ClosePath: Intersecting Polygon") {
        val coords = Seq[(Int, Int)](
            (0, 0),
            (0, 5),
            (5, -5),
            (0, 5)
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)._1*SCALE),
            zigZagEncode(coords(1)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(2)._1*SCALE),
            zigZagEncode(coords(2)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(3)._1*SCALE),
            zigZagEncode(coords(3)._2*SCALE),
            commandEncode(id = ClosePath, count = 1)
        )
        val geometry = Command.parse(POLYGON, EXTENT, commands)
        // TODO: this is an example of the bug we previously found
        // POLYGON ((0 0, 0 5, 2.5 2.5, 0 0))
        // It might just output the smallest contiguous area,
        // but that's not what we want.
        assert(false)
    }

    test("Valid Multiple ClosePath") {
        val coords = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5),
                (0, -5)),
            Seq((-5, 5),
                (5, 5),
                (0, -5))
        )
        val expected = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5),
                (5, 0),
                (0, 0)),
            Seq((0, 5),
                (5, 10),
                (5, 5),
                (0, 5))
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)(0)._1*SCALE),
            zigZagEncode(coords(0)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(1)._1*SCALE),
            zigZagEncode(coords(0)(1)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(2)._1*SCALE),
            zigZagEncode(coords(0)(2)._2*SCALE),
            commandEncode(id = ClosePath, count = 1),
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(1)(0)._1*SCALE),
            zigZagEncode(coords(1)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)(1)._1*SCALE),
            zigZagEncode(coords(1)(1)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)(2)._1*SCALE),
            zigZagEncode(coords(1)(2)._2*SCALE),
            commandEncode(id = ClosePath, count = 1)
        )
        val geometry = Command.parse(POLYGON, EXTENT, commands)
        assert(geometry == MultiPolygon(expected.map(
            cs => Polygon(cs.map(c => toPoint(c))))))
    }

    test("Invalid Multiple ClosePath: Unclosed Geometry") {
        val coords = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5),
                (0, -5)),
            Seq((-5, 5),
                (5, 5),
                (0, -5))
        )
        val expected = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5),
                (5, 0),
                (0, 0)),
            Seq((0, 5),
                (5, 10),
                (5, 5),
                (0, 5))
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)(0)._1*SCALE),
            zigZagEncode(coords(0)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(1)._1*SCALE),
            zigZagEncode(coords(0)(1)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(2)._1*SCALE),
            zigZagEncode(coords(0)(2)._2*SCALE),
            commandEncode(id = ClosePath, count = 1),
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(1)(0)._1*SCALE),
            zigZagEncode(coords(1)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)(1)._1*SCALE),
            zigZagEncode(coords(1)(1)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)(2)._1*SCALE),
            zigZagEncode(coords(1)(2)._2*SCALE)
        )
        try {
            val geometry = Command.parse(POLYGON, EXTENT, commands)
            assert(false)
        } catch {
            case _: Throwable => assert(true)
        }
    }

    test("Invalid Multiple ClosePath: Bad Polygon Line") {
        val coords = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5),
                (0, -5)),
            Seq((-5, 5),
                (5, 5),
                (0, -5))
        )
        val expected = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5),
                (5, 0),
                (0, 0)),
            Seq((0, 5),
                (5, 10),
                (0, 5))
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)(0)._1*SCALE),
            zigZagEncode(coords(0)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(1)._1*SCALE),
            zigZagEncode(coords(0)(1)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(2)._1*SCALE),
            zigZagEncode(coords(0)(2)._2*SCALE),
            commandEncode(id = ClosePath, count = 1),
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(1)(0)._1*SCALE),
            zigZagEncode(coords(1)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)(1)._1*SCALE),
            zigZagEncode(coords(1)(1)._2*SCALE),
            commandEncode(id = ClosePath, count = 1)
        )
        try {
            val geometry = Command.parse(POLYGON, EXTENT, commands)
            assert(false)
        } catch {
            case _: Throwable => assert(true)
        }
    }

    test("Invalid Command in the Middle") {
        val coords = Seq[Seq[(Int, Int)]](
            Seq((0, 0),
                (5, 5),
                (0, -5)),
            Seq((-5, 5),
                (5, 5),
                (0, -5))
        )
        val commands = Seq[Int](
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(0)(0)._1*SCALE),
            zigZagEncode(coords(0)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(1)._1*SCALE),
            zigZagEncode(coords(0)(1)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)(2)._1*SCALE),
            zigZagEncode(coords(0)(2)._2*SCALE),
            commandEncode(id = 0, count =1),
            commandEncode(id = ClosePath, count = 1),
            commandEncode(id = MoveTo, count = 1),
            zigZagEncode(coords(1)(0)._1*SCALE),
            zigZagEncode(coords(1)(0)._2*SCALE),
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(1)(1)._1*SCALE),
            zigZagEncode(coords(1)(1)._2*SCALE),
            commandEncode(id = ClosePath, count = 1)
        )
        intercept[Command.UnsupportedCommand] {
            val geometry = Command.parse(POLYGON, EXTENT, commands)
        }
    }

}

