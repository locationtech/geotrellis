package geotrellis.proj4

import org.scalatest.FunSuite

/**
 * Created by manurip on 6/21/15.
 */
class CRSTest extends FunSuite {

  trait TestSets {

  }
  test("test:fromName"){
    val crs = CRS.fromName("EPSG:4326")

    val proj4string = crs.toProj4String
    val string = "+proj=longlat +datum=WGS84 +no_defs "

    assert(proj4string==string)

  }

  test("test:fromNameToWKT"){
    val wktString = CRS.fromNameToWKT("EPSG:2037")

    assert(wktString=="PROJCS[\"NAD83(CSRS98) / UTM zone 19N\",GEOGCS[\"NAD83(CSRS98)\",DATUM[\"NAD83_Canadian_Spatial_Reference_System\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],TOWGS84[0,0,0],AUTHORITY[\"EPSG\",\"6140\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]],AUTHORITY[\"EPSG\",\"4140\"]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",-69],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",0],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],AUTHORITY[\"EPSG\",\"2037\"]]")
  }


 test("test:getCodeOfProj4String(epsg)"){
    val epsgCodeOfProj4 = CRS.getCodeOfProj4String("EPSG","+proj=longlat +a=6378300.789 +b=6356566.435 +no_defs <>")

    val comparisionCode:String = "EPSG:"+4010
    assert(epsgCodeOfProj4==comparisionCode)
  }

  test("test:getCodeOfProj4String(esri)"){
    val epsgCodeOfProj4 = CRS.getCodeOfProj4String("ESRI","+proj=longlat +ellps=krass +towgs84=-17.51,-108.32,-62.39,0,0,0,0 +no_defs  no_defs <>")

    val comparisionCode:String = "ESRI:"+4147
    assert(epsgCodeOfProj4==comparisionCode)
  }

  test("test:getCodeOfProj4String(nad27)"){
    val nad83CodeOfProj4 = CRS.getCodeOfProj4String("NAD27","proj=tmerc datum=NAD27 lon_0=-110d10 lat_0=31 k=.9999 x_0=152400.3048006096 y_0=0 no_defs <>")

    val comparisionCode:String="NAD27:"+201

    assert(nad83CodeOfProj4==comparisionCode)
  }

  test("test:getCodeOfProj4String(nad83)"){
    val nad83CodeOfProj4 = CRS.getCodeOfProj4String("NAD83","proj=tmerc datum=NAD83 lon_0=-113d45 lat_0=31 k=.9999333333333333 x_0=213360 y_0=0 no_defs <>")

    val comparisionCode:String="NAD83:"+203

    assert(nad83CodeOfProj4==comparisionCode)
  }

  test("test:getCodeOfWKTString"){
    val comparisonCode = "EPSG:3824"

    val epsgCodeOfWKT = CRS.getCodeOfWKTString("EPSG","GEOGCS[\"TWD97\", DATUM[\"Taiwan Datum 1997\", SPHEROID[\"GRS 1980\", 6378137.0, 298.257222101, AUTHORITY[\"EPSG\",\"7019\"]], TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY[\"EPSG\",\"1026\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"3824\"]]")

    assert(comparisonCode==epsgCodeOfWKT)

  }
}
