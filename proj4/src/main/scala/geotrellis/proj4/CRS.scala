package geotrellis.proj4

import geotrellis.proj4.io.wkt.WKT
import org.osgeo.proj4j._

import scala.io.Source

object CRS {
  private[proj4] final case class WKT(wkt: String) extends CRS
  private[proj4] final case class WellKnown(authority: String, id: String) extends CRS
  private[proj4] final case class NativeText(params: String) extends CRS
  
  /**
    * Creates a CoordinateReferenceSystem
    * from a PROJ.4 projection parameter string.
    * <p>
    * An example of a valid PROJ.4 projection parameter string is:
    * <pre>
    * +proj=aea +lat_1=50 +lat_2=58.5 +lat_0=45 +lon_0=-126 +x_0=1000000 +y_0=0 +ellps=GRS80 +units=m
    * </pre>
    *
    * @param  proj4Params  A PROJ.4 projection parameter string
    * @return              The specified CoordinateReferenceSystem
    */
  def fromString(proj4Params: String): CRS =
    NativeText(proj4Params)

  /**
    * Returns the numeric EPSG code of a proj4string.
    */
  def getEPSGCode(proj4String: String): Option[Int] =
    NativeText(proj4String).epsgCode

  /**
    * Creates a CoordinateReferenceSystem
    * from a PROJ.4 projection parameter string.
    * <p>
    * An example of a valid PROJ.4 projection parameter string is:
    * <pre>
    * +proj=aea +lat_1=50 +lat_2=58.5 +lat_0=45 +lon_0=-126 +x_0=1000000 +y_0=0 +ellps=GRS80 +units=m
    * </pre>
    *
    * @param  name         A name for this coordinate system.
    * @param  proj4Params  A PROJ.4 projection parameter string
    * @return              The specified CoordinateReferenceSystem
    */
  def fromString(name: String, proj4Params: String): CRS =
    NativeText(proj4Params)

  /**
    * Creates a CoordinateReferenceSystem (CRS) from a
    * well-known-text String.
    */
  def fromWKT(wktString: String): CRS = 
    WKT(wktString)

  /**
    * Creates a CoordinateReferenceSystem (CRS) from a well-known name.
    * CRS names are of the form: "<tt>authority:code</tt>",
    * with the components being:
    * <ul>
    * <li><b><tt>authority</tt></b> is a code for a namespace supported by
    * PROJ.4.
    * Currently supported values are
    * <tt>EPSG</tt>,
    * <tt>ESRI</tt>,
    * <tt>WORLD</tt>,
    * <tt>NA83</tt>,
    * <tt>NAD27</tt>.
    * If no authority is provided, the <tt>EPSG</tt> namespace is assumed.
    * <li><b><tt>code</tt></b> is the id of a coordinate system in the authority namespace.
    * For example, in the <tt>EPSG</tt> namespace a code is an integer value
    * which identifies a CRS definition in the EPSG database.
    * (Codes are read and handled as strings).
    * </ul>
    * An example of a valid CRS name is <tt>EPSG:3005</tt>.
    * <p>
    *
    * @param   name  The name of a coordinate system, with optional authority prefix
    * @return        The CoordinateReferenceSystem corresponding to the given name
   */
  def fromName(name: String): CRS = {
    val (authority, id) = name.span(':' !=)
    if (id == "") 
      WellKnown("EPSG", name)
    else
      WellKnown(authority, id)
  }


  /**
    * Creates a CoordinateReferenceSystem (CRS) from an EPSG code.
    */
  def fromEpsgCode(epsgCode: Int) =
    WellKnown("EPSG", epsgCode.toString)
}

sealed trait CRS extends Serializable {
  def epsgCode: Option[Int] = Backend.lookupEpsgCode(this)
  def isGeographic: Boolean = Backend.isGeographic(this)
  def toNativeString: String = Backend.toNativeString(this)
}

trait Backend {
  def lookupEpsgCode(crs: CRS): Option[Int]
  def getTransform(src: CRS, dst: CRS): Transform
  def isGeographic(crs: CRS): Boolean
  def toNativeString(crs: CRS): String
}

object Proj4Backend extends Backend {
  def lookupEpsgCode(crs: CRS): Option[Int] = 
    crs match {
      case CRS.WKT(wkt) => scala.util.Try(WKT.getEPSGCode(wkt).toInt).toOption
      case CRS.WellKnown(auth, id) if auth equalsIgnoreCase "EPSG" => scala.util.Try(id.toInt).toOption
      case CRS.WellKnown(_, _) => None
      case CRS.NativeText(params) => getEPSGCode(params)
    }

  def getTransform(src: CRS, dst: CRS): Transform = {
    val t = new BasicCoordinateTransform(parse(src), parse(dst))

    (x: Double, y: Double) => {
      val srcP = new ProjCoordinate(x, y)
      val destP = new ProjCoordinate
      t.transform(srcP, destP)
      (destP.x, destP.y)
    }
  }

  def isGeographic(crs: CRS): Boolean = parse(crs).isGeographic

  def toNativeString(crs: CRS): String = parse(crs).getParameterString

  private val crsFactory = new CRSFactory
  private val filePrefix = "/geotrellis/proj4/nad/"
  private lazy val proj4ToEPSGMap = new Memoize[String, Option[String]](readEPSGCodeFromFile)
  private def parse(crs: CRS): CoordinateReferenceSystem = 
    crs match {
      case CRS.WKT(wkt) =>
        crsFactory.createFromName(WKT.getEPSGCode(wkt))
      case CRS.WellKnown(auth, id) =>
        crsFactory.createFromName(s"$auth:$id")
      case CRS.NativeText(params) =>
        crsFactory.createFromParameters(null, params)
    }

  private def getEPSGCode(proj4String: String): Option[Int] =
    proj4ToEPSGMap(proj4String).map(_.toInt)

  private def readEPSGCodeFromFile(proj4String: String): Option[String] = {
    val stream = getClass.getResourceAsStream(s"${filePrefix}epsg")
    try {
      Source.fromInputStream(stream)
        .getLines
        .find { line =>
          !line.startsWith("#") && {
            val proj4Body = line.split("proj")(1)
            s"+proj$proj4Body" == proj4String
          }
        }.flatMap { l =>
          val array = l.split(" ")
          val length = array(0).length
          Some(array(0).substring(1, length - 1))
        }
    } finally {
      stream.close()
    }
  }
}

object SISBackend extends Backend {
  def lookupEpsgCode(crs: CRS): Option[Int] =
    Option(org.apache.sis.referencing.IdentifiedObjects.lookupEPSG(parse(crs))).map(_.intValue)

  def getTransform(src: CRS, dst: CRS): Transform = {
    val op = org.apache.sis.referencing.CRS.findOperation(parse(src), parse(dst), null)
    val tx = op.getMathTransform
    (x: Double, y: Double) => {
      val pos = new org.apache.sis.geometry.DirectPosition2D(x, y)
      tx.transform(pos, pos)
      (pos.x, pos.y)
    }
  }

  def isGeographic(crs: CRS): Boolean =
    parse(crs).isInstanceOf[org.opengis.referencing.crs.GeographicCRS]

  def toNativeString(crs: CRS): String = {
    val locale = java.util.Locale.ROOT
    val timezone = java.util.TimeZone.getTimeZone("GMT+0")
    (new org.apache.sis.io.wkt.WKTFormat(locale, timezone)).format(crs)
  }

  private def parse(crs: CRS): org.opengis.referencing.crs.CoordinateReferenceSystem =
    crs match {
      case CRS.WKT(wkt) => org.apache.sis.referencing.CRS.fromWKT(wkt)
      case CRS.WellKnown(auth, id) => org.apache.sis.referencing.CRS.forCode(s"$auth:$id")
      case CRS.NativeText(text) => org.apache.sis.referencing.CRS.fromWKT(text)
    }
}
