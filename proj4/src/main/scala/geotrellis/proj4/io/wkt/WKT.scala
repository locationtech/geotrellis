package geotrellis.proj4.io.wkt

import org.osgeo.proj4j.NotFoundException

import scala.io.Source

/**
* @author Manuri Perera
*/
object WKT {
  /**
   * Returns the WKT representation given an EPSG code in the format EPSG:[number]
   * @param name
   * @return
   */
  def fromEPSGCode(name :String): String ={
    val code = name.split(":")(1)

    val dataStream=Source.fromFile("proj4/src/main/resources/wkt/epsg.properties").getLines().toStream.dropWhile(line=>line.split("=")(0)!=code)

    if(dataStream.length>0) {
      val array = dataStream(0).split("=")
      array(1)
    }
    else throw new NotFoundException("Unable to find WKT representation of "+name)
  }


  /**
   * Returns the numeric code of a WKT string given the authority
   * @param wktString
   * @return
   */
  def getEPSGCode(wktString:String)={
    val file = "proj4/src/main/resources/wkt/epsg.properties"

    val dataStream = Source.fromFile(file).getLines().toStream.dropWhile(line=>line.split("=")(1)!=wktString)

    if(dataStream.length>0){
      val array = dataStream(0).split("=")
      "EPSG:"+array(0)
    }
    else throw new NotFoundException("Unable to find the EPSG code of "+wktString)
  }

}
