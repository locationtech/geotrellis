package geotrellis.proj4

import java.io.{DataOutputStream, File}

import org.osgeo.proj4j._

import scala.io.Source

object CRS {
  private val crsFactory = new CRSFactory

  /**
   * Creates a [[CoordinateReferenceSystem]] (CRS) from a well-known name.
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
   * @param name the name of a coordinate system, with optional authority prefix
   * @return the [[CoordinateReferenceSystem]] corresponding to the given name
   */
  def fromName(name: String): CRS =
    new CRS { val crs = crsFactory.createFromName(name) }

  /**
   * Creates a [[CoordinateReferenceSystem]]
   * from a PROJ.4 projection parameter string.
   * <p>
   * An example of a valid PROJ.4 projection parameter string is:
   * <pre>
   * +proj=aea +lat_1=50 +lat_2=58.5 +lat_0=45 +lon_0=-126 +x_0=1000000 +y_0=0 +ellps=GRS80 +units=m
   * </pre>
   * @param proj4Params a PROJ.4 projection parameter string
   * @return the specified [[CoordinateReferenceSystem]]
   */
  def fromString(proj4Params: String): CRS =
    new CRS { val crs = crsFactory.createFromParameters(null, proj4Params) }

  /**
   * Creates a [[CoordinateReferenceSystem]]
   * from a PROJ.4 projection parameter string.
   * <p>
   * An example of a valid PROJ.4 projection parameter string is:
   * <pre>
   * +proj=aea +lat_1=50 +lat_2=58.5 +lat_0=45 +lon_0=-126 +x_0=1000000 +y_0=0 +ellps=GRS80 +units=m
   * </pre>
   * @param name a name for this coordinate system.
   * @param proj4Params a PROJ.4 projection parameter string
   * @return the specified [[CoordinateReferenceSystem]]
   */
  def fromString(name: String, proj4Params: String): CRS =
    new CRS{ val crs = crsFactory.createFromParameters(name, proj4Params) }


  /**
   * Creates a [[CoordinateReferenceSystem]] (CRS) from a well-known-text String.
   * @param wktString
   * @return
   */
  def fromWKT(wktString :String): CRS ={
    val epsgCode = getCodeOfWKTString("EPSG",wktString)

    fromName(epsgCode)
  }


  /**
   * Returns the numeric code of a WKT string given the authority
   * @param authority
   * @param wktString
   * @return
   */
  def getCodeOfWKTString(authority:String,wktString:String)={
    val filePrefix = "proj4/src/main/resources/wkt/"
    val file = getFile(filePrefix,authority)+".properties"

    val dataStream = Source.fromFile(file).getLines().toStream.dropWhile(line=>line.split("=")(1)!=wktString)

    if(dataStream.length>0){
      val array = dataStream(0).split("=")
      authority+":"+array(0)
    }
    else "Enter a valid String"
  }

  private def getFile(filePrefix:String,authority:String)=authority match{
    case "EPSG"=>filePrefix+"epsg"
    case "ESRI"=>filePrefix+"esri"
    case "NAD27"=>filePrefix+"nad27"
    case "NAD83"=>filePrefix+"nad83"
  }
}



trait CRS extends Serializable {

  val Epsilon = 1e-8

  private[proj4] val crs: CoordinateReferenceSystem

  lazy val epsgCode: Option[String]= getEPSGCodeOfProj4String(toProj4String+" <>")

  protected def factory = CRS.crsFactory

  /** Override this function to handle reprojecting to another CRS in a more performant way */
  def alternateTransform(dest: CRS): Option[(Double, Double) => (Double, Double)] =
    None

  def toProj4String: String =
    crs.getParameterString

  // TODO: Do these better once more things are ported
  override
  def hashCode = toProj4String.hashCode

  override
  def equals(o: Any): Boolean =
    o match {
      case other: CRS => compareProj4Strings(other.toProj4String, toProj4String)
      case _ => false
    }

  private def compareProj4Strings(p1: String, p2: String) = {
    def toProj4Map(s: String): Map[String, String] =
      s.trim.split(" ").map( x =>
        if (x.startsWith("+")) x.substring(1) else x).map(x => {
          val index = x.indexOf('=')
          if (index != -1) (x.substring(0, index) -> Some(x.substring(index + 1)))
          else (x -> None)
        }).groupBy(_._1).map { case (a, b) => (a, b.head._2) }
        .filter { case (a, b) => !b.isEmpty }.map { case (a, b) => (a -> b.get) }
        .map { case (a, b) => if (b == "latlong") (a -> "longlat") else (a, b) }
        .filter { case (a, b) => (a != "to_meter" || b != "1.0") }

    val m1 = toProj4Map(p1)
    val m2 = toProj4Map(p2)

    m1.map {
      case (key, v1) => m2.get(key) match {
        case Some(v2) => compareValues(v1, v2)
        case None => false
      }
    }.forall(_ != false)
  }

  private def compareValues(s1: String, s2: String) = {
    def isNumber(s: String) = s.filter(c => !List('.', '-').contains(c)) forall Character.isDigit

    val s2IsNumber = isNumber(s1)
    val s1IsNumber = isNumber(s2)

    if (s1IsNumber == s2IsNumber) {
      if (s1IsNumber) math.abs(s1.toDouble - s2.toDouble) < Epsilon
      else s1 == s2
    } else false
  }

  /**
   * Returns the WKT representation of the Coordinate Reference System
   * @return
   */
  def toWKT(): String ={
    epsgCode match {
      case None=>"Unable to convert"
      case Some(string) => fromNameToWKT(epsgCode.get)
    }
  }

  /**
   * Returns the WKT representation given an EPSG code in the format EPSG:[number]
   * @param name
   * @return
   */
  def fromNameToWKT(name :String): String ={
    val code = name.split(":")(1)

    val dataStream=Source.fromFile("proj4/src/main/resources/wkt/epsg.properties").getLines().toStream.dropWhile(line=>line.split("=")(0)!=code)

    if(dataStream.length>0) {
      val array = dataStream(0).split("=")
      array(1)
    }
    else "Please enter a valid code"
  }

  /**
   * Returns the numeric EPSG code of a proj4string
   * @param proj4String
   * @return
   */
  def getEPSGCodeOfProj4String(proj4String: String):Option[String]={
    val filePrefix = "proj4/src/main/resources/nad/"

    def code(line:String):Option[String]={
      val array = line.split(" ")
      val length = array(0).length
      Some("EPSG:"+array(0).substring(1,length-1))
    }

    val dataStream =Source.fromFile(filePrefix+"epsg").getLines().toStream.dropWhile(line=>line.startsWith("#") || "+proj"+((line).split("proj"))(1)!=proj4String)

    if(dataStream.length>0){
      code(dataStream(0))
    }
    else {
      None
    }

  }
}
