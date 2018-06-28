package geotrellis.proj4.mgrs

import scala.math.{Pi, cos, floor, pow, round, sin, sqrt, tan}

/**
 * A lat/long to Military Grid Reference System (MGRS) conversion utility
 *
 * This implementation is a port of proj4js/mgrs.  See https://github.com/proj4js/mgrs for more.
 */
object MGRS {
  /**
   * Converts a longitude/latitude position to an MGRS grid location.  The accuracy argument indicates
   * the resolution of grid cells that should be used.  The default accuracy of 5 indicates 1m resolution,
   * while an accuracy of 1 indicates 10km resolution, with power of 10 steps in between.
   */
  def longLatToMGRS(long: Double, lat: Double, accuracy: Int = 5): String = {
    if (accuracy < 1 || accuracy > 5)
      throw new IllegalArgumentException("Accuracy for MGRS conversion must be in the range 1 to 5")

    encode(LLtoUTM(long, lat), accuracy)
  }

  /**
   * Converts an MGRS grid specifier to a bounding box in long/lat coordinates.  The return value gives
   * the bounding box as (left, bottom, right, top) positions.  If the accuracy is zero (i.e., no numerical
   * location characters following the zone number, zone letter, and grid square identifiers), the bounding
   * box will be degenerate; use mgrsToLongLat in that case.
   */
  def mgrsToBBox(mgrs: String): (Double, Double, Double, Double) = {
    val decoded = decode(mgrs)
    val ((easting, northing, zoneNumber, zoneLetter), accuracy) = decoded
    val (left, bottom) = UTMtoLL(decoded._1)
    val (right, top) = UTMtoLL((easting+accuracy.toInt, northing+accuracy.toInt, zoneNumber, zoneLetter))
    (left, bottom, right, top)
  }

  /**
   * Converts an MGRS grid specifier to a long/lat position.  This is the arithmetic center of the
   * grid cell specified by the MGRS string.
   */
  def mgrsToLongLat(mgrs: String): (Double, Double) = {
    val (left, bottom, right, top) = mgrsToBBox(mgrs)
    ((left + right)/2, (bottom + top)/2)
  }

  private val NUM_100K_SETS = 6
  private val SET_ORIGIN_COLUMN_LETTERS = "AJSAJS"
  private val SET_ORIGIN_ROW_LETTERS = "AFAFAF"

  private val _A = 65
  private val _I = 73
  private val _O = 79
  private val _V = 86
  private val _Z = 90

  private def degToRad(deg: Double) = deg * Pi / 180.0
  private def radToDeg(rad: Double) = 180.0 * rad / Pi

  /**
   * Converts a set of Longitude and Latitude co-ordinates to UTM
   * using the WGS84 ellipsoid.
   */
  private def LLtoUTM(long: Double, lat: Double): (Int, Int, Int, Char) = {
    val a = 6378137.0                   //ellip.radius;
    val eccSquared = 0.00669438         //ellip.eccsq;
    val k0 = 0.9996
    val latRad = degToRad(lat)
    val longRad = degToRad(long)

    val zoneNumber =
      if (long == 180) {
        //Make sure the longitude 180.00 is in Zone 60
        60
      } else if (lat >= 56.0 && lat < 64.0 && long >= 3.0 && long < 12.0) {
        // Special zone for Norway
        32
      } else if (lat >= 72.0 && lat < 84.0) {
        // Special zones for Svalbard
        if (long >= 0.0 && long < 9.0) {
          31
        } else if (long >= 9.0 && long < 21.0) {
          33
        } else if (long >= 21.0 && long < 33.0) {
          35
        } else if (long >= 33.0 && long < 42.0) {
          37
        } else {
          ((long + 180.0) / 6.0).toInt + 1
        }
      } else
        ((long + 180.0) / 6.0).toInt + 1

    val longOrigin = (zoneNumber.toDouble - 1) * 6 - 180 + 3     // +3 puts origin in middle of zone
    val longOriginRad = degToRad(longOrigin)

    val eccPrimeSquared = (eccSquared) / (1 - eccSquared)

    val _N = a / sqrt(1 - eccSquared * sin(latRad) * sin(latRad))
    val _T = tan(latRad) * tan(latRad)
    val _C = eccPrimeSquared * cos(latRad) * cos(latRad)
    val _A = cos(latRad) * (longRad - longOriginRad)
    val _M = a * ((1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64 - 5 * eccSquared * eccSquared * eccSquared / 256) * latRad
                  - (3 * eccSquared / 8 + 3 * eccSquared * eccSquared / 32 + 45 * eccSquared * eccSquared * eccSquared / 1024) * sin(2 * latRad)
                  + (15 * eccSquared * eccSquared / 256 + 45 * eccSquared * eccSquared * eccSquared / 1024) * sin(4 * latRad)
                  - (35 * eccSquared * eccSquared * eccSquared / 3072) * sin(6 * latRad))

    val utmEasting = k0 * _N * (_A + (1 - _T + _C) * pow(_A, 3) / 6.0
                                + (5 - 18 * _T + _T * _T + 72 * _C - 58 * eccPrimeSquared) * pow(_A, 5) / 120.0) + 500000.0

    val northingOffset =
      if (lat < 0.0)
        10000000.0 //10000000 meter offset for southern hemisphere
      else
        0.0
    val utmNorthing = k0 * (_M + _N * tan(latRad) * (_A * _A / 2 + (5 - _T + 9 * _C + 4 * _C * _C) * pow(_A, 4) / 24.0 + (61 - 58 * _T + _T * _T + 600 * _C - 330 * eccPrimeSquared) * pow(_A, 6) / 720.0)) + northingOffset

    return (round(utmNorthing).toInt, round(utmEasting).toInt, zoneNumber, getLetterDesignator(lat))
  }

  /**
   * Converts UTM coords to long/lat, using the WGS84 ellipsoid.
   */
  private def UTMtoLL(decoded: (Int, Int, Int, Char)): (Double, Double) = {
    val (utmEasting, utmNorthing, zoneNumber, zoneLetter) = decoded

    // check the ZoneNumber is valid
    if (zoneNumber < 0 || zoneNumber > 60) {
      throw new IllegalArgumentException(s"Zone number $zoneNumber is invalid")
    }

    val k0 = 0.9996
    val a = 6378137.0 //ellip.radius
    val eccSquared = 0.00669438 //ellip.eccsq
    var e1 = (1 - sqrt(1 - eccSquared)) / (1 + sqrt(1 - eccSquared))

    // remove 500,000 meter offset for longitude
    val x = utmEasting - 500000.0

    // We must know somehow if we are in the Northern or Southern
    // hemisphere, this is the only time we use the letter So even
    // if the Zone letter isn't exactly correct it should indicate
    // the hemisphere correctly
    val y = if (zoneLetter < 'N') { utmNorthing - 10000000.0 } else utmNorthing

    // There are 60 zones with zone 1 being at West -180 to -174
    val longOrigin = (zoneNumber - 1) * 6 - 180 + 3 // +3 puts origin in middle of zone

    val eccPrimeSquared = (eccSquared) / (1 - eccSquared)

    val _M = y / k0
    val mu = _M / (a * (1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64 - 5 * eccSquared * eccSquared * eccSquared / 256))

    val phi1Rad = mu + (3 * e1 / 2 - 27 * pow(e1, 3) / 32) * sin(2 * mu) + (21 * e1 * e1 / 16 - 55 * pow(e1, 4) / 32) * sin(4 * mu) + (151 * pow(e1, 3) / 96) * sin(6 * mu)
  // double phi1 = ProjMath.radToDeg(phi1Rad);

    val _N1 = a / sqrt(1 - eccSquared * sin(phi1Rad) * sin(phi1Rad))
    val _T1 = tan(phi1Rad) * tan(phi1Rad)
    val _C1 = eccPrimeSquared * cos(phi1Rad) * cos(phi1Rad)
    val _R1 = a * (1 - eccSquared) / pow(1 - eccSquared * sin(phi1Rad) * sin(phi1Rad), 1.5)
    val _D = x / (_N1 * k0);

    val lat = radToDeg(phi1Rad - (_N1 * tan(phi1Rad) / _R1) * (_D * _D / 2 - (5 + 3 * _T1 + 10 * _C1 - 4 * _C1 * _C1 - 9 * eccPrimeSquared) * pow(_D, 4) / 24 + (61 + 90 * _T1 + 298 * _C1 + 45 * _T1 * _T1 - 252 * eccPrimeSquared - 3 * _C1 * _C1) * pow(_D, 6) / 720))

    var long = longOrigin + radToDeg((_D - (1 + 2 * _T1 + _C1) * pow(_D, 3) / 6 + (5 - 2 * _C1 + 28 * _T1 - 3 * _C1 * _C1 + 8 * eccPrimeSquared + 24 * _T1 * _T1) * pow(_D, 5) / 120) / cos(phi1Rad))

    (long, lat)
  }

  /**
   * Calculates the MGRS letter designator for the given latitude.
   */
  private def getLetterDesignator(lat: Double): Char = {
    if (80 <= lat && lat <= 84)
      'X'
    else if (-80 <= lat && lat < 80) {
      val letters = "CDEFGHJKLMNPQRSTUVWX"
      letters.charAt((lat + 80).toInt/8)
    } else
      throw new IllegalArgumentException(s"Latitude value, $lat, is out of range")
  }

  /**
   * Encodes a UTM location as MGRS string.
   */
  private def encode(utm: (Int, Int, Int, Char), accuracy: Int): String = {
    val (northing, easting, zoneNumber, zoneLetter) = utm
    val formatString = "%0" + accuracy.toString + "d"
    val seasting = easting.formatted(formatString)
    val snorthing = northing.formatted(formatString)

    s"${zoneNumber}${zoneLetter}${get100kID(easting, northing, zoneNumber)}${seasting.drop(seasting.length - 5).take(accuracy)}${snorthing.drop(snorthing.length - 5).take(accuracy)}"
  }

  /**
   * Get the two letter 100k designator for a given UTM easting,
   * northing and zone number value.
   */
  private def get100kID(easting: Int, northing: Int, zoneNumber: Int) = {
    val setParm = get100kSetForZone(zoneNumber)
    val setColumn = (easting / 100000)
    val setRow = (northing / 100000) % 20
    getLetter100kID(setColumn, setRow, setParm)
  }

  /**
   * Given a UTM zone number, figure out the MGRS 100K set it is in.
   */
  private def get100kSetForZone(i: Int) = {
    val setParm = i % NUM_100K_SETS
    if (setParm == 0)
      NUM_100K_SETS
    else
      setParm
  }

  /**
   * Get the two-letter MGRS 100k designator given information
   * translated from the UTM northing, easting and zone number.
   *
   * column : the column index as it relates to the MGRS
   *          100k set spreadsheet, created from the UTM easting.
   *          Values are 1-8.
   * row    : the row index as it relates to the MGRS 100k set
   *          spreadsheet, created from the UTM northing value. Values
   *          are from 0-19.
   * parm   : the set block, as it relates to the MGRS 100k set
   *          spreadsheet, created from the UTM zone. Values are from
   *          1-60.
   *
   * Returns the two letter MGRS 100k code.
   */
  private def getLetter100kID(column: Int, row: Int, parm: Int): String = {
    // colOrigin and rowOrigin are the letters at the origin of the set
    val index = parm - 1
    val colOrigin = SET_ORIGIN_COLUMN_LETTERS.charAt(index).toInt
    val rowOrigin = SET_ORIGIN_ROW_LETTERS.charAt(index).toInt

    // colInt and rowInt are the letters to build to return
    var colInt = colOrigin + column - 1
    var rowInt = rowOrigin + row
    var rollover = false

    if (colInt > _Z) {
      colInt = colInt - _Z + _A - 1
      rollover = true
    }

    if (colInt == _I || (colOrigin < _I && colInt > _I) || ((colInt > _I || colOrigin < _I) && rollover))
      colInt += 1

    if (colInt == _O || (colOrigin < _O && colInt > _O) || ((colInt > _O || colOrigin < _O) && rollover)) {
      colInt += 1

      if (colInt == _I)
        colInt += 1
    }

    if (colInt > _Z)
      colInt = colInt - _Z + _A - 1

    if (rowInt > _V) {
      rowInt = rowInt - _V + _A - 1
      rollover = true
    } else {
      rollover = false
    }

    if (((rowInt == _I) || ((rowOrigin < _I) && (rowInt > _I))) || (((rowInt > _I) || (rowOrigin < _I)) && rollover))
      rowInt += 1

    if (((rowInt == _O) || ((rowOrigin < _O) && (rowInt > _O))) || (((rowInt > _O) || (rowOrigin < _O)) && rollover)) {
      rowInt += 1

      if (rowInt == _I) {
        rowInt += 1
      }
    }

    if (rowInt > _V)
      rowInt = rowInt - _V + _A - 1

    "" + colInt.toChar + rowInt.toChar
  }

  /**
   * Decode the UTM parameters from a MGRS string.
   *
   * mgrsString : an UPPERCASE coordinate string
   *
   * Returns a pair of a 4-tuple giving (easting, northing, zoneLetter, zoneNumber) and and an integer accuracy (in meters)
   */
  private def decode(mgrsString: String): ((Int, Int, Int, Char), Double) = {

    if (mgrsString.length == 0) {
      throw new IllegalArgumentException("Cannot generate MGRS from empty string")
    }

    val length = mgrsString.length

    var sb = ""
    var i = 0

    // get Zone number
    while (! Range('A', 'Z', 1).contains(mgrsString.charAt(i))) {
      if (i >= 2) {
        throw new IllegalArgumentException (s"Bad MGRS conversion from $mgrsString")
      }
      sb += mgrsString.charAt(i)
      i += 1
    }

    if (i == 0 || i + 3 > length) {
      // A good MGRS string has to be 4-5 digits long,
      // ##AAA/#AAA at least.
      throw new IllegalArgumentException (s"Bad MGRS conversion from $mgrsString")
    }

    val zoneNumber = sb.toInt
    val zoneLetter = mgrsString.charAt(i)

    // Should we check the zone letter here? Why not.
    if (zoneLetter <= 'B' || zoneLetter >= 'Y' || zoneLetter == 'I' || zoneLetter == 'O') {
      throw new IllegalArgumentException(s"MGRS zone letter $zoneLetter not allowed in $mgrsString")
    }

    i += 1
    val hunK = mgrsString.drop(i).take(2)
    var set = get100kSetForZone(zoneNumber)
    i += 2

    var east100k = getEastingFromChar(hunK.charAt(0), set)
    var north100k = getNorthingFromChar(hunK.charAt(1), set)

    // We have a bug where the northing may be 2000000 too low.
    // How do we know when to roll over?
    while (north100k < getMinNorthing(zoneLetter)) {
      north100k += 2000000
    }

    // calculate the char index for easting/northing separator
    val remainder = length - i

    if (remainder % 2 != 0) {
      throw new IllegalArgumentException("MGRS must have an even number of digits after the zone letter/100km letters in $mgrsString")
    }

    val sep = remainder / 2

    val (sepEasting, sepNorthing, accuracyBonus) =
      if (sep > 0) {
        val aB = 100000.0 / pow(10, sep)
        val sepEastingString = mgrsString.substring(i, i + sep)
        val sepNorthingString = mgrsString.substring(i + sep)
        (sepEastingString.toDouble * aB, sepNorthingString.toDouble * aB, aB)
      } else {
        (0.0, 0.0, 0.0)
      }

    val easting = sepEasting + east100k
    val northing = sepNorthing + north100k

    ((easting.toInt, northing.toInt, zoneNumber, zoneLetter), accuracyBonus)
  }


  /**
   * Given the first letter from a two-letter MGRS 100k zone, and given the
   * MGRS table set for the zone number, figure out the easting value that
   * should be added to the other, secondary easting value.
   *
   * e   : The first letter from a two-letter MGRS 100´k zone.
   * set : The MGRS table set for the zone number.
   *
   * Returns the easting value for the given letter and set.
   */
  private def getEastingFromChar(e: Char, set: Int): Double = {
    // colOrigin is the letter at the origin of the set for the column
    var curCol = SET_ORIGIN_COLUMN_LETTERS.charAt(set - 1).toInt
    var eastingValue = 100000.0
    var rewindMarker = false

    while (curCol != e.toInt) {
      curCol += 1
      if (curCol == _I) {
        curCol += 1
      }
      if (curCol == _O) {
        curCol += 1
      }
      if (curCol > _Z) {
        if (rewindMarker) {
          throw new IllegalArgumentException("Bad character $e in getEastingFromChar")
        }
        curCol = _A
        rewindMarker = true
      }
      eastingValue += 100000.0
    }

    eastingValue
  }

  /**
   * Given the second letter from a two-letter MGRS 100k zone, and given the
   * MGRS table set for the zone number, figure out the northing value that
   * should be added to the other, secondary northing value. You have to
   * remember that Northings are determined from the equator, and the vertical
   * cycle of letters mean a 2000000 additional northing meters. This happens
   * approx. every 18 degrees of latitude. This method does *NOT* count any
   * additional northings. You have to figure out how many 2000000 meters need
   * to be added for the zone letter of the MGRS coordinate.
   *
   * n   : Second letter of the MGRS 100k zone
   * set : The MGRS table set number, which is dependent on the UTM zone number.
   *
   * Returns the northing value for the given letter and set.
   */
  private def getNorthingFromChar(n: Char, set: Int): Double = {

    if (n > 'V') {
      throw new IllegalArgumentException("Invalid northing, $n, passed to getNorthingFromChar")
    }

    // rowOrigin is the letter at the origin of the set for the column
    var curRow = SET_ORIGIN_ROW_LETTERS.charAt(set - 1).toInt
    var northingValue = 0.0
    var rewindMarker = false

    while (curRow != n.toInt) {
      curRow += 1
      if (curRow == _I) {
        curRow += 1
      }
      if (curRow == _O) {
        curRow += 1
      }
      // fixing a bug making whole application hang in this loop
      // when 'n' is a wrong character
      if (curRow > _V) {
        if (rewindMarker) { // making sure that this loop ends
          throw new IllegalArgumentException("Bad character, $n, passed to getNorthingFromChar")
        }
        curRow = _A
        rewindMarker = true
      }
      northingValue += 100000.0
    }

    northingValue
  }

  /**
   * The function getMinNorthing returns the minimum northing value of a MGRS
   * zone.
   *
   * Ported from Geotrans' c Lattitude_Band_Value structure table.
   *
   * zoneLetter : The MGRS zone to get the min northing for.
   */
  private def getMinNorthing(zoneLetter: Char): Double = {
    zoneLetter match {
      case 'C' => 1100000.0
      case 'D' => 2000000.0
      case 'E' => 2800000.0
      case 'F' => 3700000.0
      case 'G' => 4600000.0
      case 'H' => 5500000.0
      case 'J' => 6400000.0
      case 'K' => 7300000.0
      case 'L' => 8200000.0
      case 'M' => 9100000.0
      case 'N' => 0.0
      case 'P' => 800000.0
      case 'Q' => 1700000.0
      case 'R' => 2600000.0
      case 'S' => 3500000.0
      case 'T' => 4400000.0
      case 'U' => 5300000.0
      case 'V' => 6200000.0
      case 'W' => 7000000.0
      case 'X' => 7900000.0
      case _ => throw new IllegalArgumentException(s"Invalid zone letter, $zoneLetter")
    }
  }

}
