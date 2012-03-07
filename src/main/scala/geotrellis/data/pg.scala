package geotrellis.data

import java.sql.{Connection, ResultSet, ResultSetMetaData, PreparedStatement, DriverManager}
import java.util._
import org.postgis._
import org.postgresql._

import com.vividsolutions.jts.geom.{Coordinate => JtsCoordinate}
import com.vividsolutions.jts.io.WKBReader
import com.vividsolutions.jts.geom.Geometry

import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.ArrayBuffer

import geotrellis.geometry.Feature

trait BasePgTypes {

  def wkbReader() = new WKBReader()

  def getGeometry(r:ResultSet, pos:Int) = r.getString(pos) match {
    case null => null
    case s => wkbReader().read(WKBReader.hexToBytes(s))
  }

  /**
   * NOTE: the first element is indexed at one, not zero.
   */
  def getColumnValue(r:ResultSet, pos:Int):Any = getColumnValue(r, r.getMetaData(), pos)

  def getColumnValue(r:ResultSet, m:ResultSetMetaData, pos:Int):Any = {
    import java.sql.Types._
    m.getColumnType(pos) match {
      case BLOB => r.getBlob(pos)
      case BOOLEAN => r.getBoolean(pos)
      case DATE => r.getDate(pos)
      case DOUBLE => r.getDouble(pos)
      case INTEGER => r.getInt(pos)
      case VARCHAR => r.getString(pos)
      case BIGINT => r.getLong(pos)
      case BIT => r.getBoolean(pos)
      case 1111 => m.getColumnTypeName(pos) match {
        case "geometry" => getGeometry(r, pos)
        case _ => r.getObject(pos)
      }
      case i => {
        val typname = r.getMetaData().getColumnTypeName(pos)
        throw new Exception("unhandled sql type: %s/%s".format(typname, i))
      }
    }
  }

  def getColumnValuesAsArray(r:ResultSet) = resultsAsArray(r)

  def resultsAsArray(r:ResultSet) = {
    val n = r.getMetaData().getColumnCount()
    val arr = Array.ofDim[Any](n)
    for(i <- 0 until n) {
      arr(i) = getColumnValue(r, i + 1)
    }
    arr
  }

  def getColumnValuesAsMap(r:ResultSet) = resultsAsMap(r)

  def resultsAsMap(r:ResultSet) = {
    val n = r.getMetaData().getColumnCount()
    val map = MMap.empty[String, Any]
    for(pos <- 1 to n) {
      map(r.getMetaData().getColumnName(pos)) = getColumnValue(r, pos)
    }
    map
  }

  /**
   *
   */
  def bindColumnValue(stmt:PreparedStatement, pos:Int, value:Any) {
    val pgstmt = stmt.asInstanceOf[org.postgresql.PGStatement]

    value match {
      case b:Boolean => stmt.setBoolean(pos, b)
      case d:Double => stmt.setDouble(pos, d)
      case f:Float => stmt.setFloat(pos, f)
      case i:Int => stmt.setInt(pos, i)
      case l:Long => stmt.setLong(pos, l)
      case s:String => stmt.setString(pos, s)
      case _ => throw new Exception("can't handle %s".format(value))
    }
  }

  def bindColumnValues(stmt:PreparedStatement, values:Array[Any]) {
    for (i <- 0 until values.length) bindColumnValue(stmt, i + 1, values(i))
  }
}

object PgTypes extends BasePgTypes

class PgTypes extends BasePgTypes


// somewhere else????
object PgUtil {
  Class.forName("org.postgis.DriverWrapper")

  def connect(host:String, port:Int, database:String, username:String, password:String) = {
    val url = "jdbc:postgresql_postGIS://%s:%s/%s".format(host,port,database)
    DriverManager.getConnection(url, username, password)
  }

  def prepare(conn:Connection, sql:String, params:Array[Any]) = {
    val types = new PgTypes
    val stmt = conn.prepareStatement(sql)
    types.bindColumnValues(stmt, params)
    stmt
  }
}

object PostgisFeatureReader {
  Class.forName("org.postgis.DriverWrapper")
 
  def apply(conf:scala.collection.Map[String, Any] ) =  {
    new PostgisFeatureReader( 
      conf.getOrElse("database", "default").asInstanceOf[String],
      conf.getOrElse("host", "localhost").asInstanceOf[String],
      conf.getOrElse("port", "5432").asInstanceOf[String],
      conf.get("table").get.asInstanceOf[String],
      conf.get("username").get.asInstanceOf[String],
      conf.getOrElse("password", "").asInstanceOf[String]
    )
  }
}


case class PostgisFeatureReader(database:String, host:String, port:String, 
                           table:String, username:String, password:String) { 
  val url = "jdbc:postgresql_postGIS://%s:%s/%s".format(host,port,database)
  val conn = DriverManager.getConnection(url, username, password)

  val types = new PgTypes

  def buildFeature(r:ResultSet, geometryIndex:Int, valueIndex:Int, meta:ResultSetMetaData, ncols:Int) = {
    val geom = types.getGeometry(r, geometryIndex)
    val value = if (valueIndex >= 1) r.getInt(valueIndex) else 0
    val attrs = MMap.empty[String, Any]
    for(i <- 1 to ncols) {
      if (i != geometryIndex && i != valueIndex) {
        val name = meta.getColumnName(i)
        val value = types.getColumnValue(r, meta, i)
        attrs(name) = value
      }
    }

    val coords = geom.getCoordinates()
    //printf("GOT %d COORDS\n", coords.length)

    val feature = geom.getGeometryType() match {
      case "Point" => geotrellis.geometry.Point(coords(0), value, attrs.toMap)
      case "Polygon" => {
        buildPolygon(coords, value, attrs)
      }
      case "MultiPolygon" =>
        geotrellis.geometry.MultiPolygon(
          (0 until geom.getNumGeometries()).map(n => buildPolygon(geom.getGeometryN(n).getCoordinates(),value,attrs)).toArray,
          value, attrs.toMap)
      case "LineString" => geotrellis.geometry.LineString(coords, value, attrs.toMap)
      case "GeometryCollection" => throw new Exception("not supported")
      case t => throw new Exception("unknown type: %s".format(t))
    }

    feature
  }

  def buildPolygon(coords: Array[JtsCoordinate], value: Int, attrs: MMap[String,Any]) = {
    val last = coords(coords.length - 1)
    
    // XXX XXX XXX terrible hacks await!
    if (coords(0).equals2D(last)) {
      geotrellis.geometry.Polygon(coords, value, attrs.toMap)
    } else {
      val coords2 = Array.ofDim[JtsCoordinate](coords.length + 1)
      var i = 0
      while (i < coords.length) {
        coords2(i) = coords(i)
        i += 1
      }
      coords2(coords.length) = coords(0)
      geotrellis.geometry.Polygon(coords2, value, attrs.toMap)
    }    
  }

  def buildFeatures(r:ResultSet, geometryIndex:Int, valueIndex:Int) = {
    val meta = r.getMetaData()
    val ncols = meta.getColumnCount()

    assert(geometryIndex <= ncols)
    assert(valueIndex <= ncols)

    val features = ArrayBuffer.empty[Feature]

    while (r.next()) {
      // buildFeature doesn't seem to be threadsafe
      val feature = this.synchronized {
        buildFeature(r, geometryIndex, valueIndex, meta, ncols)
      }
      features.append(feature)
    }

    features
  }

  def getFeatures(sql:String, geometryIndex:Int, valueIndex:Int):ArrayBuffer[Feature] = {
    getFeatures(sql, Array.empty, geometryIndex, valueIndex)
  }

  def getFeatures(sql:String, values:Array[Any], geometryIndex:Int, valueIndex:Int) = {
    val s = PgUtil.prepare(conn, sql, values)
    val r = s.executeQuery()
    val features = buildFeatures(r, geometryIndex, valueIndex)
    s.close()
    features
  }

}
