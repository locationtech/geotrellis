package geotrellis.proj4.io.wkt

import geotrellis.proj4.CRS
import org.scalatest.FunSpec

/**
 * @author Manuri Perera
 */
class WKTTest extends FunSpec {
  it("should return the WKTString corresponding to the passed EPSG code"){
    val crs = CRS.fromName("EPSG:3824")
    val wktString = WKT.fromEPSGCode("3824")

    assert(wktString=="GEOGCS[\"TWD97\", DATUM[\"Taiwan Datum 1997\", SPHEROID[\"GRS 1980\", 6378137.0, 298.257222101, AUTHORITY[\"EPSG\",\"7019\"]], TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY[\"EPSG\",\"1026\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"3824\"]]")

  }

  it("should return the EPSG code of the passed WKT string"){
    val comparisonCode = "EPSG:3824"

    val epsgCodeOfWKT = WKT.getEPSGCode("GEOGCS[\"TWD97\", DATUM[\"Taiwan Datum 1997\", SPHEROID[\"GRS 1980\", 6378137.0, 298.257222101, AUTHORITY[\"EPSG\",\"7019\"]], TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY[\"EPSG\",\"1026\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"3824\"]]")

    assert(comparisonCode==epsgCodeOfWKT)

  }

}
