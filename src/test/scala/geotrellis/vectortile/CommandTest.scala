package geotrellis.vectortile

import org.scalatest._

import geotrellis.vector._

class CommandTest extends FunSuite {

    val EXTENT = 4096
    val SCALE = EXTENT / 256

    val POINT = vector_tile.Tile.GeomType.POINT
    val LINESTRING = vector_tile.Tile.GeomType.LINESTRING
    val POLYGON = vector_tile.Tile.GeomType.POLYGON

    val MoveTo = Command.MoveTo // 1
    val LineTo = Command.LineTo // 2
    val ClosePath = Command.ClosePath // 7

    def commandEncode(id: Int, count: Int) = (id & 0x7) | (count << 3)
    def zigZagEncode(n: Int) = ((n << 1) ^ (n >> 31))
    val toPoint: ((Int, Int)) => Point = {
        case (x: Int, y: Int) => Point(x.toDouble, y.toDouble)
    }

    test("Invalid") {
        val commands = Seq[Int](
            commandEncode(id = 0, count = 1)
        )
        intercept[Command.UnsupportedCommand] {
            Command.parse(POINT, EXTENT, commands)
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

    test("Invalid MultiPoint: Insufficient Args 1") {
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

    test("Valid Line") {
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

    test("Invalid Line: No Geometry") {
        val coords = Seq[(Int, Int)](
            (5, 0)
        )
        val commands = Seq[Int](
            commandEncode(id = LineTo, count = 1),
            zigZagEncode(coords(0)._1*SCALE),
            zigZagEncode(coords(0)._2*SCALE)
        )
        intercept[Command.NoGeometry] {
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

    test("Invalid MultiLine: Insufficient Args 1") {
        assert(false)
        // NYI
    }

    test("Valid ClosePath") {
        // NYI
    }

    test("Invalid ClosePath") {
        // NYI
    }

    // TODO also test Invalid commands in the middle
    // TODO also test unexpected commands in the middle

}

