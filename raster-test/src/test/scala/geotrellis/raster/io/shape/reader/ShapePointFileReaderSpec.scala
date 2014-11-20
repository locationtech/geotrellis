package geotrellis.raster.io.shape.reader

import geotrellis.testkit._

import org.scalatest._

/**
  * Tests reading .shp files.
  */
class ShapePointFileReaderSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with TestEngine {

  describe("should read shape point files correctly") {

    /*
     * Since this file is rather big (160 polygons),
     *  I'm comparing the values with the GeoTools reader's hashcodes
     * instead. Apparently the GeoTools reader says that the type "polygon"
     * is a MultiPolygon, so I just took all the lines in each MultiPolygon,
     * hashed them, and then I check that my polygons lines has the same
     * hash code.
     */
    it("should read demographics.dbf correctly") {
      val correctLineSets = Array(
        Set(-1500308061), Set(394500365), Set(-1591898469), Set(-636129849),
        Set(2099665837), Set(1231487165), Set(112309256), Set(751940365),
        Set(1788887353), Set(-818812548), Set(763146183), Set(1447798275),
        Set(-1820753902), Set(1467869601), Set(-1626943280), Set(-131580369),
        Set(1763306247), Set(-685424389), Set(593602561), Set(-1297024179),
        Set(127364772), Set(-2088394373), Set(-1497638470), Set(-2019213835),
        Set(207278681), Set(-1553433517), Set(91591460), Set(135932894),
        Set(2046339012), Set(-13703143), Set(1686060625), Set(-1493775709),
        Set(513350380), Set(-536944549), Set(-945928523, 1193428387),
        Set(-398313850), Set(1507245305), Set(-417299070), Set(1236947366),
        Set(1199381030), Set(1168779386), Set(2003466961), Set(-1087923756),
        Set(-1508967665), Set(-474531864), Set(-175758515), Set(144997813),
        Set(-747363264), Set(-403143120), Set(2105613397), Set(-1685925251),
        Set(660853426), Set(-986379910), Set(-962941293), Set(-1900179320),
        Set(-2022136464), Set(-935923504), Set(-1945857047), Set(1671027464),
        Set(681649179, 1760574422), Set(-700012422), Set(-1235892644),
        Set(-2081755649), Set(1583688547), Set(1984475039), Set(-626336834),
        Set(1209941187), Set(647660047), Set(1034573690), Set(1522729125),
        Set(-272326885), Set(-1506984948), Set(600294865), Set(-1709397131),
        Set(1494342209), Set(950666474), Set(-1912558302), Set(-1928895966),
        Set(-157730769), Set(105077527), Set(-1623010412), Set(1134057993),
        Set(1630258597), Set(-1900815019), Set(-679913187, -940426072),
        Set(1818086056), Set(-995495057), Set(-1087888989), Set(-1667624299),
        Set(982580601), Set(1133377346), Set(1965731728), Set(-1554873840),
        Set(-304464848), Set(1563960613), Set(-634698627), Set(-332296619),
        Set(1222640800), Set(1249481907), Set(1946657137),
        Set(312114006, -1263413160), Set(110844419), Set(-1213820678),
        Set(-1216244490), Set(1779567067), Set(432951756), Set(-1490567945),
        Set(1148600312), Set(1735758194), Set(-1700663549), Set(-511811648),
        Set(-2142278248), Set(-471236689), Set(-693202443), Set(102243779),
        Set(-328366637), Set(-1042343383), Set(455844285, -598184550),
        Set(-426299372, 101176247), Set(-2030841998), Set(1763874556),
        Set(-31254079), Set(1213607719), Set(584829751), Set(1121472236),
        Set(-598192697), Set(-2094697044), Set(2022334282), Set(667222493),
        Set(-154269733), Set(-972606532), Set(948438301), Set(672976367),
        Set(-2036334458), Set(1210863521), Set(-1474938793), Set(763540284),
        Set(1899740858), Set(406499728), Set(667671441), Set(1468767949),
        Set(37440961), Set(-1114887818), Set(-1203309722), Set(-419669350),
        Set(72237257), Set(-1319270443), Set(37556672), Set(-2050962132),
        Set(518465054), Set(136650072), Set(-672958999, -2014944721, -395621507),
        Set(1598638566), Set(-1019959301), Set(-417655962), Set(-711595506),
        Set(1569989940), Set(-1673100106), Set(-409568899), Set(-1166088463)
      )


      val path = "raster-test/data/shapefiles/demographics/demographics.shp"
      val shapePointFile = ShapePointFileReader(path).read

      shapePointFile.size should be (160)
      for (i <- 0 until shapePointFile.size)
        shapePointFile(i) match {
          case PolygonPointRecord(p) =>
            withClue(s"failed on index: $i with polygon $p: ") {
              val lines = p.holes :+ p.exterior
              lines.map(_.hashCode).toSet should be (correctLineSets(i))
            }
          case _ => fail
        }
    }
  }
}
