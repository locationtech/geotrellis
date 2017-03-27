package geotrellis.proj4.io.wkt
import org.scalatest.FunSpec

class WKTParserTest extends FunSpec {

  it("Should parse a simple string") {
    import WKTParser._
    val expected = "EPSG"
    parseAll(string, """"EPSG"""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse a complex string") {
    import WKTParser._
    val expected = "Barbados 1938 / Barbados National Grid"
    parseAll(string, """"Barbados 1938 / Barbados National Grid"""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse a complex string with parens") {
    import WKTParser._
    val expected = "RGR92 (3D deg)"
    parseAll(string, """"RGR92 (3D deg)"""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse a comma") {
    import WKTParser._
    val expected = None
    parseAll(comma, """,""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse a space and a comma") {
    import WKTParser._
    val expected = None
    parseAll(comma, """, """) match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse a negative double") {
    import WKTParser._
    val expected = -220.3
    parseAll(double, "-220.3") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse int") {
    import WKTParser._
    val expected = 12
    parseAll(int, "12") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse a negative int") {
    import WKTParser._
    val expected = -12
    parseAll(int, "-12") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse a value that is an int") {
    import WKTParser._
    val expected = 12
    parseAll(value, "12") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse a value that is a double") {
    import WKTParser._
    val expected = 12.123
    parseAll(value, "12.123") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse a value that is a simple string") {
    import WKTParser._
    val expected = "helloWorld"
    parseAll(value, """"helloWorld"""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse a value that is a complex string") {
    import WKTParser._
    val expected = "Barbados 1938 / Barbados National Grid"
    parseAll(value, """"Barbados 1938 / Barbados National Grid"""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return an Authority object") {
    import WKTParser._
    val expected = Authority("EPSG", "7019")
    parseAll(authority, """AUTHORITY["EPSG","7019"]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return an Spheroid object that has an authority") {
    import WKTParser._
    val auth = Authority("EPSG", "7019")
    val expected = new Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(auth))
    parseAll(spheroid, """SPHEROID["GRS 1980", 6378137.0, 298.257222101, AUTHORITY["EPSG","7019"]]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return an Spheroid object that has no authority") {
    import WKTParser._
    val expected = new Spheroid("GRS 1980", 6378137.0, 298.257222101, None)
    parseAll(spheroid, """SPHEROID["GRS 1980", 6378137.0, 298.257222101]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return an ToWGS84 object") {
    import WKTParser._
    val expected = new ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    parseAll(toWgs, """TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return an Datum object with toWgs84 and authority") {
    import WKTParser._
    val auth = new Authority("EPSG", "7019")
    val spher = new Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(auth))
    val authDat = new Authority("EPSG", "6140")
    val toWgs84 = new ToWgs84(List(-0.991, 1.9072, 0.5129, 0.0257899075194932, -0.009650098960270402, -0.011659943232342112, 0.0))
    val expected = new Datum("NAD83 Canadian Spatial Reference System", spher, Some(toWgs84), Some(authDat))
    parseAll(datum, """DATUM["NAD83 Canadian Spatial Reference System", SPHEROID["GRS 1980", 6378137.0, 298.257222101, AUTHORITY["EPSG","7019"]], TOWGS84[-0.991, 1.9072, 0.5129, 0.0257899075194932, -0.009650098960270402, -0.011659943232342112, 0.0], AUTHORITY["EPSG","6140"]]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return an Datum object without toWgs84 and with authority") {
    import WKTParser._
    val auth = new Authority("EPSG", "7019")
    val spher = new Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(auth))
    val authDat = new Authority("EPSG", "6140")
    val expected = new Datum("NAD83 Canadian Spatial Reference System", spher, None, Some(authDat))
    parseAll(datum, """DATUM["NAD83 Canadian Spatial Reference System", SPHEROID["GRS 1980", 6378137.0, 298.257222101, AUTHORITY["EPSG","7019"]], AUTHORITY["EPSG","6140"]]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return an Datum object with toWgs84 and without authority") {
    import WKTParser._
    val auth = new Authority("EPSG", "7019")
    val spher = new Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(auth))
    val toWgs84 = new ToWgs84(List(-0.991, 1.9072, 0.5129, 0.0257899075194932, -0.009650098960270402, -0.011659943232342112, 0.0))
    val expected = new Datum("NAD83 Canadian Spatial Reference System", spher, Some(toWgs84), None)
    parseAll(datum, """DATUM["NAD83 Canadian Spatial Reference System", SPHEROID["GRS 1980", 6378137.0, 298.257222101, AUTHORITY["EPSG","7019"]], TOWGS84[-0.991, 1.9072, 0.5129, 0.0257899075194932, -0.009650098960270402, -0.011659943232342112, 0.0]]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return an PrimeM object with authority") {
    import WKTParser._
    val auth = new Authority("EPSG", "8901")
    val expected = new PrimeM("Greenwich", 0.0, Some(auth))
    parseAll(primeM, """PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return an PrimeM object without authority") {
    import WKTParser._
    val auth = new Authority("EPSG", "8901")
    val expected = new PrimeM("Greenwich", 0.0, None)
    parseAll(primeM, """PRIMEM["Greenwich", 0.0]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return TwinAxis object") {
    import WKTParser._
    val first = new Axis("Geodetic longitude", "EAST")
    val second = new Axis("Geodetic latitude", "NORTH")
    val expected = new TwinAxis(first, second)
    parseAll(twinAxis, """AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return UnitField object") {
    import WKTParser._
    val expected = new UnitField("degree", 0.017453292519943295, None)
    parseAll(unitField, """UNIT["degree", 0.017453292519943295]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return Geogcs object") {
    import WKTParser._
    val spherAuth = new Authority("EPSG", "7019")
    val spher = new Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(spherAuth))
    val toWGS = new ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val datumAuth = new Authority("EPSG", "6176")
    val datum = new Datum("Australian Antarctic Datum 1998", spher, Some(toWGS), Some(datumAuth))
    val primeMAuth = new Authority("EPSG", "8901")
    val primeM = new PrimeM("Greenwich", 0.0, Some(primeMAuth))
    val auth = new Authority("EPSG", "61766413")
    val unitField = new UnitField("degree", 0.017453292519943295, None)
    val axisOne = new Axis("Geodetic longitude", "EAST")
    val axisTwo = new Axis("Geodetic latitude", "NORTH")
    val expected = new GeogCS("Australian Antarctic (3D deg)", datum, primeM, unitField, Some(List(axisOne, axisTwo)), Some(auth))
    val geo = """GEOGCS["Australian Antarctic (3D deg)", DATUM["Australian Antarctic Datum 1998", SPHEROID["GRS 1980", 6378137.0, 298.257222101, AUTHORITY["EPSG","7019"]], TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY["EPSG","6176"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG", "8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","61766413"]]"""
    parseAll(geogcs, geo) match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return Projection object") {
    import WKTParser._
    val auth = new Authority("EPSG", "9802")
    val expected = new Projection("Lambert_Conformal_Conic_2SP", Some(auth))
    parseAll(projection, """PROJECTION["Lambert_Conformal_Conic_2SP", AUTHORITY["EPSG","9802"]]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return Parameter object") {
    import WKTParser._
    val expected = new Parameter("central_meridian", 3.0)
    parseAll(parameter, """PARAMETER["central_meridian", 3.0]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return a list of Parameter objects") {
    import WKTParser._
    val param1 = new Parameter("central_meridian", 3.0)
    val param2 = new Parameter("latitude_of_origin", 42.0)
    val param3 = new Parameter("standard_parallel_1", 42.75)
    val param4 = new Parameter("false_easting", 1700000.0)
    val expected = List(param1, param2, param3, param4)
    parseAll(parameterList, """PARAMETER["central_meridian", 3.0], PARAMETER["latitude_of_origin", 42.0], PARAMETER["standard_parallel_1", 42.75], PARAMETER["false_easting", 1700000.0]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return a Projcs objects") {
    import WKTParser._
    val toWgs84 = new ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val spherAuth = new Authority("EPSG","7019")
    val datumAuth = new Authority("EPSG","6171")
    val geogcsAuth = new Authority("EPSG","4171")
    val projectionAuth = new Authority("EPSG","9802")
    val projAuth = new Authority("EPSG","3942")
    val spheroid = new Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(spherAuth))
    val datum = new Datum("Reseau Geodesique Francais 1993", spheroid, Some(toWgs84), Some(datumAuth))
    val primeMAuth = new Authority("EPSG","8901")
    val primeM = new PrimeM("Greenwich", 0.0, Some(primeMAuth))
    val geogcsUnit = new UnitField("degree", 0.017453292519943295, None)
    val axisFirst = new Axis("Geodetic longitude", "EAST")
    val axisSecond = new Axis("Geodetic latitude", "NORTH")
    val geogcs = new GeogCS("RGF93", datum, primeM, geogcsUnit, Some(List(axisFirst, axisSecond)), Some(geogcsAuth))
    val projection = new Projection("Lambert_Conformal_Conic_2SP", Some(projectionAuth))
    val param1 = new Parameter("central_meridian", 3.0)
    val param2 = new Parameter("latitude_of_origin", 42.0)
    val param3 = new Parameter("standard_parallel_1", 42.75)
    val param4 = new Parameter("false_easting", 1700000.0)
    val param5 = new Parameter("false_northing", 1200000.0)
    val param6 = new Parameter("scale_factor", 1.0)
    val param7 = new Parameter("standard_parallel_2", 41.25)
    val unitField = new UnitField("m", 1.0, None)
    val axisOne = new Axis("Easting", "EAST")
    val axisTwo = new Axis("Northing", "NORTH")
    val twins = new TwinAxis(axisOne, axisTwo)
    val paramsList = List(param1, param2, param3, param4, param5, param6, param7)
    val expected = new ProjCS("RGF93 / CC42", geogcs, projection, Some(paramsList), unitField, Some(twins), Some(projAuth))
    val projcsString = """PROJCS["RGF93 / CC42", GEOGCS["RGF93", DATUM["Reseau Geodesique Francais 1993", SPHEROID["GRS 1980", 6378137.0, 298.257222101, AUTHORITY["EPSG","7019"]], TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY["EPSG","6171"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4171"]], PROJECTION["Lambert_Conformal_Conic_2SP", AUTHORITY["EPSG","9802"]], PARAMETER["central_meridian", 3.0], PARAMETER["latitude_of_origin", 42.0], PARAMETER["standard_parallel_1", 42.75], PARAMETER["false_easting", 1700000.0], PARAMETER["false_northing", 1200000.0], PARAMETER["scale_factor", 1.0], PARAMETER["standard_parallel_2", 41.25], UNIT["m", 1.0], AXIS["Easting", EAST], AXIS["Northing", NORTH], AUTHORITY["EPSG","3942"]]"""
    parseAll(projcs, projcsString) match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return VertCS object") {
    import WKTParser._
    val vertAuth = new Authority("EPSG","5621")
    val datumAuth = new Authority("EPSG","5215")
    val vertDatum = new VertDatum("European Vertical Reference Frame 2007", 2005, Some(datumAuth))
    val axis = new Axis("Gravity-related height", "UP")
    val unitField = new UnitField("m", 1.0, None)
    val expected = new VertCS("EVRF2007 height", vertDatum, unitField, Some(axis), Some(vertAuth))
    parseAll(vertcs, """VERT_CS["EVRF2007 height", VERT_DATUM["European Vertical Reference Frame 2007", 2005, AUTHORITY["EPSG","5215"]], UNIT["m", 1.0], AXIS["Gravity-related height", UP], AUTHORITY["EPSG","5621"]]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return a list of Axis objects") {
    import WKTParser._
    val axis1 = new Axis("x", "EAST")
    val axis2 = new Axis("y", "NORTH")
    val expected = List(axis1, axis2)
    parseAll(getAxisList, """AXIS["x", EAST], AXIS["y", NORTH]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return a LocalDatum object no authority") {
    import WKTParser._
    val expected = new LocalDatum("Unknown", 0, None)
    parseAll(localDatum, """LOCAL_DATUM["Unknown", 0]""") match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return a LocalCS object") {
    import WKTParser._
    val datumAuth = new Authority("EPSG","9314")
    val localDatum = new LocalDatum("Tombak LNG plant", 0, Some(datumAuth))
    val axis1 = new Axis("Plant East", "EAST")
    val axis2 = new Axis("Plant North", "WEST")
    val axisList = List(axis1, axis2)
    val localAuth = new Authority("EPSG","5817")
    val unitField = new UnitField("m", 1.0, None)
    val expected = new LocalCS("Tombak LNG plant", localDatum, unitField, axisList, Some(localAuth))
    val localCSString = """LOCAL_CS["Tombak LNG plant", LOCAL_DATUM["Tombak LNG plant", 0, AUTHORITY["EPSG","9314"]], UNIT["m", 1.0], AXIS["Plant East", EAST], AXIS["Plant North", WEST], AUTHORITY["EPSG","5817"]]"""
    parseAll(localcs, localCSString) match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return a Geoccs object") {
    import WKTParser._
    val spherAuth = new Authority("EPSG","7019")
    val spheroid = new Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(spherAuth))
    val toWgs84 = new ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val datumAuth = new Authority("EPSG","1035")
    val datum = new Datum("Red Geodesica de Canarias 1995", spheroid, Some(toWgs84), Some(datumAuth))
    var primeM = new PrimeM("Greenwich", 0.0, Some(new Authority("EPSG","8901")))
    val unitField = new UnitField("m", 1.0, None)
    val axis1 = new Axis("Geocentric X", "GEOCENTRIC_X")
    val axis2 = new Axis("Geocentric Y", "GEOCENTRIC_Y")
    val axis3 = new Axis("Geocentric Z", "GEOCENTRIC_Z")
    val auth = new Authority("EPSG","4079")
    val expected = new GeocCS("REGCAN95", datum, primeM, unitField, Some(List(axis1, axis2, axis3)), Some(auth))
    val geoCCSString = """GEOCCS["REGCAN95", DATUM["Red Geodesica de Canarias 1995", SPHEROID["GRS 1980", 6378137.0, 298.257222101, AUTHORITY["EPSG","7019"]], TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY["EPSG","1035"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["m", 1.0], AXIS["Geocentric X", GEOCENTRIC_X], AXIS["Geocentric Y", GEOCENTRIC_Y], AXIS["Geocentric Z", GEOCENTRIC_Z], AUTHORITY["EPSG","4079"]]"""
    parseAll(geoccs, geoCCSString) match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }


  it("Should return a Compdcs object") {
    import WKTParser._
    val authGeogcs = new Authority("EPSG","6258")
    val authSpher = new Authority("EPSG","7019")
    val authPrimeM = new Authority("EPSG","8901")
    val toWgs84 = new ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val spher = new Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(authSpher))
    val datum = new Datum("European Terrestrial Reference System 1989", spher, Some(toWgs84), Some(authGeogcs))
    val primeM = new PrimeM("Greenwich", 0.0, Some(authPrimeM))
    val unitField = new UnitField("degree", 0.017453292519943295, None)
    val axis1 = new Axis("Geodetic latitude", "NORTH")
    val axis2 = new Axis("Geodetic longitude", "EAST")
    val geogcs = new GeogCS("ETRS89", datum, primeM, unitField, Some(List(axis1, axis2)), Some(new Authority("EPSG","4258")))
    val projection = new Projection("Transverse_Mercator", Some(new Authority("EPSG","9807")))
    val param1 = new Parameter("central_meridian", 33.0)
    val param2 = new Parameter("latitude_of_origin", 0.0)
    val param3 = new Parameter("scale_factor", 0.9996)
    val param4 = new Parameter("false_easting", 500000.0)
    val param5 = new Parameter("false_northing", 0.0)
    val paramsList = List(param1, param2, param3, param4, param5)
    val axisProj1 = new Axis("Easting", "EAST")
    val axisProj2 = new Axis("Northing", "NORTH")
    val twinsProj = new TwinAxis(axisProj1, axisProj2)
    val authProj = new Authority("EPSG","25836")
    val projcs = new ProjCS("ETRS89 / UTM zone 36N", geogcs, projection, Some(paramsList), new UnitField("m", 1.0, None), Some(twinsProj), Some(authProj))
    val authGeoCCS = new Authority("EPSG","6176")
    val vertCSAuth = new Authority("EPSG","5776")
    val vertDatumAuth = new Authority("EPSG","5174")
    val vertDatum = new VertDatum("Norway Normal Null 1954", 2005, Some(vertDatumAuth))
    val vertAxis = new Axis("Gravity-related height", "UP")
    val vertUnit = new UnitField("m", 1.0, None)
    val vertCS = new VertCS("NN54 height", vertDatum, vertUnit, Some(vertAxis), Some(vertCSAuth))
    val expected = new CompDCS("ETRS89 / UTM zone 36 + NN54 height", projcs, vertCS, Some(authGeoCCS))
    val geoCCSString = """COMPD_CS["ETRS89 / UTM zone 36 + NN54 height", PROJCS["ETRS89 / UTM zone 36N", GEOGCS["ETRS89", DATUM["European Terrestrial Reference System 1989", SPHEROID["GRS 1980", 6378137.0, 298.257222101, AUTHORITY["EPSG","7019"]], TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY["EPSG","6258"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic latitude", NORTH], AXIS["Geodetic longitude", EAST], AUTHORITY["EPSG","4258"]], PROJECTION["Transverse_Mercator", AUTHORITY["EPSG","9807"]], PARAMETER["central_meridian", 33.0], PARAMETER["latitude_of_origin", 0.0], PARAMETER["scale_factor", 0.9996], PARAMETER["false_easting", 500000.0], PARAMETER["false_northing", 0.0], UNIT["m", 1.0], AXIS["Easting", EAST], AXIS["Northing", NORTH], AUTHORITY["EPSG","25836"]], VERT_CS["NN54 height", VERT_DATUM["Norway Normal Null 1954", 2005, AUTHORITY["EPSG","5174"]], UNIT["m", 1.0], AXIS["Gravity-related height", UP], AUTHORITY["EPSG","5776"]], AUTHORITY["EPSG","6176"]]"""
    parseAll(compdcs, geoCCSString) match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should return a Projcs object special one") {
    import WKTParser._
    val authProjcs = new Authority("EPSG","22033")
    val authSpher = new Authority("EPSG","7012")
    val spher = new Spheroid("Clarke 1880 (RGS)", 6378249.145, 293.465, Some(authSpher))
    val toWgs84 = new ToWgs84(List(-50.9, -347.6, -231.0, 0.0, 0.0, 0.0, 0.0))
    val authDatum = new Authority("EPSG","6220")
    val datum = new Datum("Camacupa", spher, Some(toWgs84), Some(authDatum))
    val authPrimeM = new Authority("EPSG","8901")
    val primeM = new PrimeM("Greenwich", 0.0, Some(authPrimeM))
    val unitField = new UnitField("degree", 0.017453292519943295, None)
    val axis1 = new Axis("Geodetic longitude", "EAST")
    val axis2 = new Axis("Geodetic latitude", "NORTH")
    val authGeogcs = new Authority("EPSG","4220")
    val geogcs = new GeogCS("Camacupa", datum, primeM, unitField, Some(List(axis1, axis2)), Some(authGeogcs))
    val projection = new Projection("Transverse_Mercator", Some(new Authority("EPSG","9807")))
    val param1 = new Parameter("central_meridian", 15.0)
    val param2 = new Parameter("latitude_of_origin", 0.0)
    val param3 = new Parameter("scale_factor", 0.9996)
    val param4 = new Parameter("false_easting", 500000.0)
    val param5 = new Parameter("false_northing", 10000000.0)
    val paramsList = List(param1, param2, param3, param4, param5)
    val unitProj = new UnitField("m", 1.0, None)
    val axisProj1 = new Axis("Easting", "EAST")
    val axisProj2 = new Axis("Northing", "NORTH")
    val expected = new ProjCS("Camacupa / UTM zone 33S", geogcs, projection, Some(paramsList), unitProj, Some(new TwinAxis(axisProj1, axisProj2)), Some(authProjcs))
    val projcsString = """PROJCS["Camacupa / UTM zone 33S", GEOGCS["Camacupa", DATUM["Camacupa", SPHEROID["Clarke 1880 (RGS)", 6378249.145, 293.465, AUTHORITY["EPSG","7012"]], TOWGS84[-50.9, -347.6, -231.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY["EPSG","6220"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4220"]], PROJECTION["Transverse_Mercator", AUTHORITY["EPSG","9807"]], PARAMETER["central_meridian", 15.0], PARAMETER["latitude_of_origin", 0.0], PARAMETER["scale_factor", 0.9996], PARAMETER["false_easting", 500000.0], PARAMETER["false_northing", 10000000.0], UNIT["m", 1.0], AXIS["Easting", EAST], AXIS["Northing", NORTH], AUTHORITY["EPSG","22033"]]"""
    parseAll(projcs, projcsString) match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should contain NAD27 / Polar Stereographic / CM=-98") {
    import WKT._
    val datSpheroid = new Spheroid("Clarke 1866", 6378206.4, 294.978698213901, None)
    val toWgs84 = new ToWgs84(List(-9, 151, 185))
    val datum = new Datum("North_American_Datum_1927", datSpheroid, Some(toWgs84), None)
    val primeM = new PrimeM("Greenwich", 0, None)
    val unitFieldGeo = new UnitField("degree", 0.0174532925199433, None)
    val geogcs = new GeogCS("NAD27", datum, primeM, unitFieldGeo, None, None)
    val projection = new Projection("Stereographic", None)
    val param1 = new Parameter("latitude_of_origin", 90)
    val param2 = new Parameter("central_meridian", -98.0)
    val param3 = new Parameter("scale_factor", 0.9996)
    val param4 = new Parameter("false_easting", 0)
    val param5 = new Parameter("false_northing",0)
    val unitField = new UnitField("Meter", 1, None)
    val auth = new Authority("EPSG", "42301")
    val expected = new ProjCS("NAD27 / Polar Stereographic / CM=-98", geogcs, projection, Some(List(param1, param2, param3, param4, param5)), unitField, None, Some(auth))
    assert(contains(expected))
  }

  it("Should return a contain the Geoccs object") {
    import WKT._
    val spherAuth = new Authority("EPSG","7019")
    val spheroid = new Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(spherAuth))
    val toWgs84 = new ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val datumAuth = new Authority("EPSG","1035")
    val datum = new Datum("Red Geodesica de Canarias 1995", spheroid, Some(toWgs84), Some(datumAuth))
    var primeM = new PrimeM("Greenwich", 0.0, Some(new Authority("EPSG","8901")))
    val unitField = new UnitField("m", 1.0, None)
    val axis1 = new Axis("Geocentric X", "GEOCENTRIC_X")
    val axis2 = new Axis("Geocentric Y", "GEOCENTRIC_Y")
    val axis3 = new Axis("Geocentric Z", "GEOCENTRIC_Z")
    val auth = new Authority("EPSG","4079")
    val expected = new GeocCS("REGCAN95", datum, primeM, unitField, Some(List(axis1, axis2, axis3)), Some(auth))
    assert(contains(expected))
  }

  it("Should parse NAD83(CSRS98) / New Brunswick Stereo") {
    import WKT._
    val spherAuth = new Authority("EPSG","7019")
    val datumSpher = new Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(spherAuth))
    val toWgs84 = new ToWgs84(List(0, 0, 0))
    val datumAuth = new Authority("EPSG","6140")
    val geoDatum = new Datum("NAD83_Canadian_Spatial_Reference_System", datumSpher, Some(toWgs84), Some(datumAuth))
    val primeMAuth = new Authority("EPSG","8901")
    val primeM = new PrimeM("Greenwich", 0, Some(primeMAuth))
    val unitAuth = new Authority("EPSG","9108")
    val unitField = new UnitField("degree", 0.0174532925199433, Some(unitAuth))
    val geoAuth = new Authority("EPSG","4140")
    val geogcs = new GeogCS("NAD83(CSRS98)", geoDatum, primeM, unitField, None, Some(geoAuth))
    val projection = new Projection("Oblique_Stereographic", None)
    val param1 = new Parameter("latitude_of_origin", 46.5)
    val param2 = new Parameter("central_meridian", -66.5)
    val param3 = new Parameter("scale_factor", 0.999912)
    val param4 = new Parameter("false_easting", 2500000.0)
    val param5 = new Parameter("false_northing", 7500000.0)
    val paramList = List(param1, param2, param3, param4, param5)
    val unitProj = new UnitField("metre", 1.0, Some(new Authority("EPSG", "9001")))
    val auth = new Authority("EPSG", "2036")
    val expected = new ProjCS("NAD83(CSRS98) / New Brunswick Stereo", geogcs, projection, Some(paramList), unitProj, None, Some(auth))
    assert(contains(expected))
  }

}
