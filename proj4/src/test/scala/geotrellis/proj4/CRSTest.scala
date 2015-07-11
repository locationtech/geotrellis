package geotrellis.proj4

import org.scalatest._
/**
 * Created by manurip on 6/21/15.
 */
class CRSTest extends FunSpec{

  it("should return the proj4string corresponding to the passed EPSG code"){
    val crs = CRS.fromName("EPSG:4326")

    val proj4string = crs.toProj4String
    val string = "+proj=longlat +datum=WGS84 +no_defs "

    assert(proj4string==string)
  }

  it("should return the WKTString corresponding to the passed EPSG code"){
    val crs = CRS.fromName("EPSG:3824")
    val wktString = crs.fromNameToWKT("EPSG:3824")

    assert(wktString=="GEOGCS[\"TWD97\", DATUM[\"Taiwan Datum 1997\", SPHEROID[\"GRS 1980\", 6378137.0, 298.257222101, AUTHORITY[\"EPSG\",\"7019\"]], TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY[\"EPSG\",\"1026\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"3824\"]]")

  }

  it("should return the EPSG code of the passed WKT string"){
    val comparisonCode = "EPSG:3824"

    val epsgCodeOfWKT = CRS.getCodeOfWKTString("EPSG","GEOGCS[\"TWD97\", DATUM[\"Taiwan Datum 1997\", SPHEROID[\"GRS 1980\", 6378137.0, 298.257222101, AUTHORITY[\"EPSG\",\"7019\"]], TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY[\"EPSG\",\"1026\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"3824\"]]")

    assert(comparisonCode==epsgCodeOfWKT)

  }

  it("Should return the WKT string of the passed EPSG code"){
    val crs = CRS.fromName("EPSG:4010")

    val wktString = crs.toWKT()

    assert(wktString=="GEOGCS[\"Unknown datum based upon the Clarke 1880 (Benoit) ellipsoid\", DATUM[\"Not specified (based on Clarke 1880 (Benoit) ellipsoid)\", SPHEROID[\"Clarke 1880 (Benoit)\", 6378300.789, 293.4663155389811, AUTHORITY[\"EPSG\",\"7010\"]], AUTHORITY[\"EPSG\",\"6010\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4010\"]]")
  }

}
