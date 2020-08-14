/*
 * Copyright 2019 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.proj4.io.wkt

import org.scalatest.funspec.AnyFunSpec

class WKTParserTest extends AnyFunSpec {

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
    val expected = Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(auth))
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
    val expected = Spheroid("GRS 1980", 6378137.0, 298.257222101, None)
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
    val expected = ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
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
    val auth = Authority("EPSG", "7019")
    val spher = Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(auth))
    val authDat = Authority("EPSG", "6140")
    val toWgs84 = ToWgs84(List(-0.991, 1.9072, 0.5129, 0.0257899075194932, -0.009650098960270402, -0.011659943232342112, 0.0))
    val expected = Datum("NAD83 Canadian Spatial Reference System", spher, Some(toWgs84), Some(authDat))
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
    val auth = Authority("EPSG", "7019")
    val spher = Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(auth))
    val authDat = Authority("EPSG", "6140")
    val expected = Datum("NAD83 Canadian Spatial Reference System", spher, None, Some(authDat))
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
    val auth = Authority("EPSG", "7019")
    val spher = Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(auth))
    val toWgs84 = ToWgs84(List(-0.991, 1.9072, 0.5129, 0.0257899075194932, -0.009650098960270402, -0.011659943232342112, 0.0))
    val expected = Datum("NAD83 Canadian Spatial Reference System", spher, Some(toWgs84), None)
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
    val auth = Authority("EPSG", "8901")
    val expected = PrimeM("Greenwich", 0.0, Some(auth))
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
    val auth = Authority("EPSG", "8901")
    val expected = PrimeM("Greenwich", 0.0, None)
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
    val first = Axis("Geodetic longitude", "EAST")
    val second = Axis("Geodetic latitude", "NORTH")
    val expected = TwinAxis(first, second)
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
    val expected = UnitField("degree", 0.017453292519943295, None)
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
    val spherAuth = Authority("EPSG", "7019")
    val spher = Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(spherAuth))
    val toWGS = ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val datumAuth = Authority("EPSG", "6176")
    val datum = Datum("Australian Antarctic Datum 1998", spher, Some(toWGS), Some(datumAuth))
    val primeMAuth = Authority("EPSG", "8901")
    val primeM = PrimeM("Greenwich", 0.0, Some(primeMAuth))
    val auth = Authority("EPSG", "61766413")
    val unitField = UnitField("degree", 0.017453292519943295, None)
    val axisOne = Axis("Geodetic longitude", "EAST")
    val axisTwo = Axis("Geodetic latitude", "NORTH")
    val expected = GeogCS("Australian Antarctic (3D deg)", datum, primeM, unitField, Some(List(axisOne, axisTwo)), Some(auth))
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
    val auth = Authority("EPSG", "9802")
    val expected = Projection("Lambert_Conformal_Conic_2SP", Some(auth))
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
    val expected = Parameter("central_meridian", 3.0)
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
    val param1 = Parameter("central_meridian", 3.0)
    val param2 = Parameter("latitude_of_origin", 42.0)
    val param3 = Parameter("standard_parallel_1", 42.75)
    val param4 = Parameter("false_easting", 1700000.0)
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
    val toWgs84 = ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val spherAuth = Authority("EPSG", "7019")
    val datumAuth = Authority("EPSG", "6171")
    val geogcsAuth = Authority("EPSG", "4171")
    val projectionAuth = Authority("EPSG", "9802")
    val projAuth = Authority("EPSG", "3942")
    val spheroid = Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(spherAuth))
    val datum = Datum("Reseau Geodesique Francais 1993", spheroid, Some(toWgs84), Some(datumAuth))
    val primeMAuth = Authority("EPSG", "8901")
    val primeM = PrimeM("Greenwich", 0.0, Some(primeMAuth))
    val geogcsUnit = UnitField("degree", 0.017453292519943295, None)
    val axisFirst = Axis("Geodetic longitude", "EAST")
    val axisSecond = Axis("Geodetic latitude", "NORTH")
    val geogcs = GeogCS("RGF93", datum, primeM, geogcsUnit, Some(List(axisFirst, axisSecond)), Some(geogcsAuth))
    val projection = Projection("Lambert_Conformal_Conic_2SP", Some(projectionAuth))
    val param1 = Parameter("central_meridian", 3.0)
    val param2 = Parameter("latitude_of_origin", 42.0)
    val param3 = Parameter("standard_parallel_1", 42.75)
    val param4 = Parameter("false_easting", 1700000.0)
    val param5 = Parameter("false_northing", 1200000.0)
    val param6 = Parameter("scale_factor", 1.0)
    val param7 = Parameter("standard_parallel_2", 41.25)
    val unitField = UnitField("m", 1.0, None)
    val axisOne = Axis("Easting", "EAST")
    val axisTwo = Axis("Northing", "NORTH")
    val twins = TwinAxis(axisOne, axisTwo)
    val paramsList = List(param1, param2, param3, param4, param5, param6, param7)
    val expected = ProjCS("RGF93 / CC42", geogcs, projection, Some(paramsList), unitField, Some(twins), None, Some(projAuth))
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
    val vertAuth = Authority("EPSG", "5621")
    val datumAuth = Authority("EPSG", "5215")
    val vertDatum = VertDatum("European Vertical Reference Frame 2007", 2005, Some(datumAuth))
    val axis = Axis("Gravity-related height", "UP")
    val unitField = UnitField("m", 1.0, None)
    val expected = VertCS("EVRF2007 height", vertDatum, unitField, Some(axis), Some(vertAuth))
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
    val axis1 = Axis("x", "EAST")
    val axis2 = Axis("y", "NORTH")
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
    val expected = LocalDatum("Unknown", 0, None)
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
    val datumAuth = Authority("EPSG", "9314")
    val localDatum = LocalDatum("Tombak LNG plant", 0, Some(datumAuth))
    val axis1 = Axis("Plant East", "EAST")
    val axis2 = Axis("Plant North", "WEST")
    val axisList = List(axis1, axis2)
    val localAuth = Authority("EPSG", "5817")
    val unitField = UnitField("m", 1.0, None)
    val expected = LocalCS("Tombak LNG plant", localDatum, unitField, axisList, Some(localAuth))
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
    val spherAuth = Authority("EPSG", "7019")
    val spheroid = Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(spherAuth))
    val toWgs84 = ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val datumAuth = Authority("EPSG", "1035")
    val datum = Datum("Red Geodesica de Canarias 1995", spheroid, Some(toWgs84), Some(datumAuth))
    val primeM = PrimeM("Greenwich", 0.0, Some(Authority("EPSG", "8901")))
    val unitField = UnitField("m", 1.0, None)
    val axis1 = Axis("Geocentric X", "GEOCENTRIC_X")
    val axis2 = Axis("Geocentric Y", "GEOCENTRIC_Y")
    val axis3 = Axis("Geocentric Z", "GEOCENTRIC_Z")
    val auth = Authority("EPSG", "4079")
    val expected = GeocCS("REGCAN95", datum, primeM, unitField, Some(List(axis1, axis2, axis3)), Some(auth))
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
    val authGeogcs = Authority("EPSG", "6258")
    val authSpher = Authority("EPSG", "7019")
    val authPrimeM = Authority("EPSG", "8901")
    val toWgs84 = ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val spher = Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(authSpher))
    val datum = Datum("European Terrestrial Reference System 1989", spher, Some(toWgs84), Some(authGeogcs))
    val primeM = PrimeM("Greenwich", 0.0, Some(authPrimeM))
    val unitField = UnitField("degree", 0.017453292519943295, None)
    val axis1 = Axis("Geodetic latitude", "NORTH")
    val axis2 = Axis("Geodetic longitude", "EAST")
    val geogcs = GeogCS("ETRS89", datum, primeM, unitField, Some(List(axis1, axis2)), Some(Authority("EPSG", "4258")))
    val projection = Projection("Transverse_Mercator", Some(Authority("EPSG", "9807")))
    val param1 = Parameter("central_meridian", 33.0)
    val param2 = Parameter("latitude_of_origin", 0.0)
    val param3 = Parameter("scale_factor", 0.9996)
    val param4 = Parameter("false_easting", 500000.0)
    val param5 = Parameter("false_northing", 0.0)
    val paramsList = List(param1, param2, param3, param4, param5)
    val axisProj1 = Axis("Easting", "EAST")
    val axisProj2 = Axis("Northing", "NORTH")
    val twinsProj = TwinAxis(axisProj1, axisProj2)
    val authProj = Authority("EPSG", "25836")
    val projcs = ProjCS("ETRS89 / UTM zone 36N", geogcs, projection, Some(paramsList), UnitField("m", 1.0, None), Some(twinsProj), None, Some(authProj))
    val authGeoCCS = Authority("EPSG", "6176")
    val vertCSAuth = Authority("EPSG", "5776")
    val vertDatumAuth = Authority("EPSG", "5174")
    val vertDatum = VertDatum("Norway Normal Null 1954", 2005, Some(vertDatumAuth))
    val vertAxis = Axis("Gravity-related height", "UP")
    val vertUnit = UnitField("m", 1.0, None)
    val vertCS = VertCS("NN54 height", vertDatum, vertUnit, Some(vertAxis), Some(vertCSAuth))
    val expected = CompDCS("ETRS89 / UTM zone 36 + NN54 height", projcs, vertCS, Some(authGeoCCS))
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
    val authProjcs = Authority("EPSG", "22033")
    val authSpher = Authority("EPSG", "7012")
    val spher = Spheroid("Clarke 1880 (RGS)", 6378249.145, 293.465, Some(authSpher))
    val toWgs84 = ToWgs84(List(-50.9, -347.6, -231.0, 0.0, 0.0, 0.0, 0.0))
    val authDatum = Authority("EPSG", "6220")
    val datum = Datum("Camacupa", spher, Some(toWgs84), Some(authDatum))
    val authPrimeM = Authority("EPSG", "8901")
    val primeM = PrimeM("Greenwich", 0.0, Some(authPrimeM))
    val unitField = UnitField("degree", 0.017453292519943295, None)
    val axis1 = Axis("Geodetic longitude", "EAST")
    val axis2 = Axis("Geodetic latitude", "NORTH")
    val authGeogcs = Authority("EPSG", "4220")
    val geogcs = GeogCS("Camacupa", datum, primeM, unitField, Some(List(axis1, axis2)), Some(authGeogcs))
    val projection = Projection("Transverse_Mercator", Some(Authority("EPSG", "9807")))
    val param1 = Parameter("central_meridian", 15.0)
    val param2 = Parameter("latitude_of_origin", 0.0)
    val param3 = Parameter("scale_factor", 0.9996)
    val param4 = Parameter("false_easting", 500000.0)
    val param5 = Parameter("false_northing", 10000000.0)
    val paramsList = List(param1, param2, param3, param4, param5)
    val unitProj = UnitField("m", 1.0, None)
    val axisProj1 = Axis("Easting", "EAST")
    val axisProj2 = Axis("Northing", "NORTH")
    val expected = ProjCS("Camacupa / UTM zone 33S", geogcs, projection, Some(paramsList), unitProj, Some(TwinAxis(axisProj1, axisProj2)), None, Some(authProjcs))
    val projcsString = """PROJCS["Camacupa / UTM zone 33S", GEOGCS["Camacupa", DATUM["Camacupa", SPHEROID["Clarke 1880 (RGS)", 6378249.145, 293.465, AUTHORITY["EPSG","7012"]], TOWGS84[-50.9, -347.6, -231.0, 0.0, 0.0, 0.0, 0.0], AUTHORITY["EPSG","6220"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4220"]], PROJECTION["Transverse_Mercator", AUTHORITY["EPSG","9807"]], PARAMETER["central_meridian", 15.0], PARAMETER["latitude_of_origin", 0.0], PARAMETER["scale_factor", 0.9996], PARAMETER["false_easting", 500000.0], PARAMETER["false_northing", 10000000.0], UNIT["m", 1.0], AXIS["Easting", EAST], AXIS["Northing", NORTH], AUTHORITY["EPSG","22033"]]"""
    parseAll(projcs, projcsString) match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should contain NAD27 / Polar Stereographic / CM\\=-98") {
    import WKT._
    val datSpheroid = Spheroid("Clarke 1866", 6378206.4, 294.978698213901, None)
    val toWgs84 = ToWgs84(List(-9, 151, 185))
    val datum = Datum("North_American_Datum_1927", datSpheroid, Some(toWgs84), None)
    val primeM = PrimeM("Greenwich", 0, None)
    val unitFieldGeo = UnitField("degree", 0.0174532925199433, None)
    val geogcs = GeogCS("NAD27", datum, primeM, unitFieldGeo, None, None)
    val projection = Projection("Stereographic", None)
    val param1 = Parameter("latitude_of_origin", 90)
    val param2 = Parameter("central_meridian", -98.0)
    val param3 = Parameter("scale_factor", 0.9996)
    val param4 = Parameter("false_easting", 0)
    val param5 = Parameter("false_northing", 0)
    val unitField = UnitField("Meter", 1, None)
    val auth = Authority("EPSG", "42301")
    val expected = ProjCS("NAD27 / Polar Stereographic / CM\\=-98", geogcs, projection, Some(List(param1, param2, param3, param4, param5)), unitField, None, None, Some(auth))
    assert(contains(expected))
  }

  it("Should return a contain the Geoccs object") {
    import WKT._
    val spherAuth = Authority("EPSG", "7019")
    val spheroid = Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(spherAuth))
    val toWgs84 = ToWgs84(List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val datumAuth = Authority("EPSG", "1035")
    val datum = Datum("Red Geodesica de Canarias 1995", spheroid, Some(toWgs84), Some(datumAuth))
    val primeM = PrimeM("Greenwich", 0.0, Some(Authority("EPSG", "8901")))
    val unitField = UnitField("m", 1.0, None)
    val axis1 = Axis("Geocentric X", "GEOCENTRIC_X")
    val axis2 = Axis("Geocentric Y", "GEOCENTRIC_Y")
    val axis3 = Axis("Geocentric Z", "GEOCENTRIC_Z")
    val auth = Authority("EPSG", "4079")
    val expected = GeocCS("REGCAN95", datum, primeM, unitField, Some(List(axis1, axis2, axis3)), Some(auth))
    assert(contains(expected))
  }

  it("Should parse NAD83(CSRS98) / New Brunswick Stereo") {
    import WKT._
    val spherAuth = Authority("EPSG", "7019")
    val datumSpher = Spheroid("GRS 1980", 6378137.0, 298.257222101, Some(spherAuth))
    val toWgs84 = ToWgs84(List(0, 0, 0))
    val datumAuth = Authority("EPSG", "6140")
    val geoDatum = Datum("NAD83_Canadian_Spatial_Reference_System", datumSpher, Some(toWgs84), Some(datumAuth))
    val primeMAuth = Authority("EPSG", "8901")
    val primeM = PrimeM("Greenwich", 0, Some(primeMAuth))
    val unitAuth = Authority("EPSG", "9108")
    val unitField = UnitField("degree", 0.0174532925199433, Some(unitAuth))
    val geoAuth = Authority("EPSG", "4140")
    val geogcs = GeogCS("NAD83(CSRS98)", geoDatum, primeM, unitField, None, Some(geoAuth))
    val projection = Projection("Oblique_Stereographic", None)
    val param1 = Parameter("latitude_of_origin", 46.5)
    val param2 = Parameter("central_meridian", -66.5)
    val param3 = Parameter("scale_factor", 0.999912)
    val param4 = Parameter("false_easting", 2500000.0)
    val param5 = Parameter("false_northing", 7500000.0)
    val paramList = List(param1, param2, param3, param4, param5)
    val unitProj = UnitField("metre", 1.0, Some(Authority("EPSG", "9001")))
    val auth = Authority("EPSG", "2036")
    val expected = ProjCS("NAD83(CSRS98) / New Brunswick Stereo", geogcs, projection, Some(paramList), unitProj, None, None, Some(auth))
    assert(contains(expected))
  }

  it("Should parse WGS 84 / Pseudo-Mercator with PROJ4 Extension (ProjCS object)") {
    import WKTParser._

    val expected = ProjCS(
      "WGS 84 / Pseudo-Mercator",
      GeogCS("WGS 84",
        Datum("WGS_1984",
          Spheroid("WGS 84", 6378137.0, 298.257223563, Some(Authority("EPSG", "7030"))),
          None, Some(Authority("EPSG", "6326"))
        ),
        PrimeM("Greenwich", 0.0, Some(Authority("EPSG", "8901"))),
        UnitField("degree", 0.0174532925199433, Some(Authority("EPSG", "9122"))),
        None, Some(Authority("EPSG", "4326"))
      ),
      Projection("Mercator_1SP", None),
      Some(List(
        Parameter("central_meridian", 0.0),
        Parameter("scale_factor", 1.0),
        Parameter("false_easting", 0.0),
        Parameter("false_northing", 0.0)
      )),
      UnitField("metre", 1.0, Some(Authority("EPSG", "9001"))),
      Some(TwinAxis(Axis("Easting", "EAST"), Axis("Northing", "NORTH"))),
      Some(ExtensionProj4("+proj=merc +a=6378137 +b=6378137 +lat_ts=0 +lon_0=0 +x_0=0 +y_0=0 +k=1 +units=m +nadgrids=@null +wktext +no_defs")),
      Some(Authority("EPSG", "3857"))
    )

    val projcsString = """PROJCS["WGS 84 / Pseudo-Mercator",GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563,AUTHORITY["EPSG","7030"]],AUTHORITY["EPSG","6326"]],PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],UNIT["degree",0.0174532925199433,AUTHORITY["EPSG","9122"]],AUTHORITY["EPSG","4326"]],PROJECTION["Mercator_1SP"],PARAMETER["central_meridian",0],PARAMETER["scale_factor",1],PARAMETER["false_easting",0],PARAMETER["false_northing",0],UNIT["metre",1,AUTHORITY["EPSG","9001"]],AXIS["Easting",EAST],AXIS["Northing",NORTH],EXTENSION["PROJ4","+proj=merc +a=6378137 +b=6378137 +lat_ts=0 +lon_0=0 +x_0=0 +y_0=0 +k=1 +units=m +nadgrids=@null +wktext +no_defs"],AUTHORITY["EPSG","3857"]]"""

    parseAll(projcs, projcsString) match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse WGS 84 / Pseudo-Mercator with an unknown Extension (ProjCS object)") {
    import WKTParser._

    val expected = ProjCS(
      "WGS 84 / Pseudo-Mercator",
      GeogCS("WGS 84",
        Datum("WGS_1984",
          Spheroid("WGS 84", 6378137.0, 298.257223563, Some(Authority("EPSG", "7030"))),
          None, Some(Authority("EPSG", "6326"))
        ),
        PrimeM("Greenwich", 0.0, Some(Authority("EPSG", "8901"))),
        UnitField("degree", 0.0174532925199433, Some(Authority("EPSG", "9122"))),
        None, Some(Authority("EPSG", "4326"))
      ),
      Projection("Mercator_1SP", None),
      Some(List(
        Parameter("central_meridian", 0.0),
        Parameter("scale_factor", 1.0),
        Parameter("false_easting", 0.0),
        Parameter("false_northing", 0.0)
      )),
      UnitField("metre", 1.0, Some(Authority("EPSG", "9001"))),
      Some(TwinAxis(Axis("Easting", "EAST"), Axis("Northing", "NORTH"))),
      Some(ExtensionAny("UNKNOWN", "str1,str2,str3")),
      Some(Authority("EPSG", "3857"))
    )

    val projcsString = """PROJCS["WGS 84 / Pseudo-Mercator",GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563,AUTHORITY["EPSG","7030"]],AUTHORITY["EPSG","6326"]],PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],UNIT["degree",0.0174532925199433,AUTHORITY["EPSG","9122"]],AUTHORITY["EPSG","4326"]],PROJECTION["Mercator_1SP"],PARAMETER["central_meridian",0],PARAMETER["scale_factor",1],PARAMETER["false_easting",0],PARAMETER["false_northing",0],UNIT["metre",1,AUTHORITY["EPSG","9001"]],AXIS["Easting",EAST],AXIS["Northing",NORTH],EXTENSION["UNKNOWN", "str1,str2,str3"],AUTHORITY["EPSG","3857"]]"""

    parseAll(projcs, projcsString) match {
      case Success(n, _) =>
        assert(n == expected)
      case NoSuccess(msg, _) =>
        info(msg)
        fail()
    }
  }

  it("Should parse GCS_WGS_1984 with and without newlines") {
    val strip = WKTParser(
      """|GEOGCS["GCS_WGS_1984",
         |DATUM["D_WGS_1984", SPHEROID["WGS_1984", 6378137.0, 298.257223563]],
         |PRIMEM["Greenwich", 0.0],
         |UNIT["degree", 0.017453292519943295],
         |AXIS["Longitude", "EAST"],
         |AXIS["Latitude", "NORTH"]]""".stripMargin)

    // single line equivalent
    val single = WKTParser("""GEOGCS["GCS_WGS_1984", DATUM["D_WGS_1984", SPHEROID["WGS_1984", 6378137.0, 298.257223563]],PRIMEM["Greenwich", 0.0],UNIT["degree", 0.017453292519943295],AXIS["Longitude", "EAST"],AXIS["Latitude", "NORTH"]]""")

    assert(strip == single)
  }

  it("Should parse WKT string with proj4 extension") {
    val strip = WKTParser(
      """|PROJCS["WGS 84 / Pseudo-Mercator",
         |GEOGCS["WGS 84",
         |DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563,AUTHORITY["EPSG","7030"]],AUTHORITY["EPSG","6326"]],
         |PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],
         |UNIT["degree",0.0174532925199433,AUTHORITY["EPSG","9122"]],
         |AUTHORITY["EPSG","4326"]],
         |PROJECTION["Mercator_1SP"],
         |PARAMETER["central_meridian",0],
         |PARAMETER["scale_factor",1],
         |PARAMETER["false_easting",0],
         |PARAMETER["false_northing",0],
         |UNIT["metre",1,AUTHORITY["EPSG","9001"]],
         |AXIS["Easting",EAST],
         |AXIS["Northing",NORTH],
         |EXTENSION["PROJ4","+proj=merc +a=6378137 +b=6378137 +lat_ts=0 +lon_0=0 +x_0=0 +y_0=0 +k=1 +units=m +nadgrids=@null +wktext +no_defs"],
         |AUTHORITY["EPSG","3857"]]""".stripMargin)

    // single line equivalent
    val single = WKTParser("""PROJCS["WGS 84 / Pseudo-Mercator",GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563,AUTHORITY["EPSG","7030"]],AUTHORITY["EPSG","6326"]],PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],UNIT["degree",0.0174532925199433,AUTHORITY["EPSG","9122"]],AUTHORITY["EPSG","4326"]],PROJECTION["Mercator_1SP"],PARAMETER["central_meridian",0],PARAMETER["scale_factor",1],PARAMETER["false_easting",0],PARAMETER["false_northing",0],UNIT["metre",1,AUTHORITY["EPSG","9001"]],AXIS["Easting",EAST],AXIS["Northing",NORTH],EXTENSION["PROJ4","+proj=merc +a=6378137 +b=6378137 +lat_ts=0 +lon_0=0 +x_0=0 +y_0=0 +k=1 +units=m +nadgrids=@null +wktext +no_defs"],AUTHORITY["EPSG","3857"]]""")

    assert(strip == single)
  }

  it("Should parse WKT string with an unknown extension") {
    val strip = WKTParser(
      """|PROJCS["WGS 84 / Pseudo-Mercator",
         |GEOGCS["WGS 84",
         |DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563,AUTHORITY["EPSG","7030"]],AUTHORITY["EPSG","6326"]],
         |PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],
         |UNIT["degree",0.0174532925199433,AUTHORITY["EPSG","9122"]],
         |AUTHORITY["EPSG","4326"]],
         |PROJECTION["Mercator_1SP"],
         |PARAMETER["central_meridian",0],
         |PARAMETER["scale_factor",1],
         |PARAMETER["false_easting",0],
         |PARAMETER["false_northing",0],
         |UNIT["metre",1,AUTHORITY["EPSG","9001"]],
         |AXIS["Easting",EAST],
         |AXIS["Northing",NORTH],
         |EXTENSION["UNKNOWN", "str1,str2,str3"],
         |AUTHORITY["EPSG","3857"]]""".stripMargin)

    // single line equivalent
    val single = WKTParser("""PROJCS["WGS 84 / Pseudo-Mercator",GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563,AUTHORITY["EPSG","7030"]],AUTHORITY["EPSG","6326"]],PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],UNIT["degree",0.0174532925199433,AUTHORITY["EPSG","9122"]],AUTHORITY["EPSG","4326"]],PROJECTION["Mercator_1SP"],PARAMETER["central_meridian",0],PARAMETER["scale_factor",1],PARAMETER["false_easting",0],PARAMETER["false_northing",0],UNIT["metre",1,AUTHORITY["EPSG","9001"]],AXIS["Easting",EAST],AXIS["Northing",NORTH],EXTENSION["UNKNOWN", "str1,str2,str3"],AUTHORITY["EPSG","3857"]]""")

    assert(strip == single)
  }
}
