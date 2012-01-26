package trellis.data

import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet, ResultSetMetaData, Types}
import com.vividsolutions.jts.io.WKBReader
import com.vividsolutions.jts.geom.Geometry
import scala.collection.mutable.{ArrayBuffer, Map}

import trellis.{geometry => tr}
import trellis.geometry.Feature

/**
 * Object containing methods for connecting to Postgres using JDBC.
 */
object Postgres {
  Class.forName("org.postgis.DriverWrapper")

  /**
   * Base URL used to create a postgres URL.
   */
  private def baseURL = "jdbc:postgresql_postGIS://%s:%s/%s"

  /**
   * Generate a postgres URL given a host, port and database.
   */
  def url(host:String, port:Int, db:String) = baseURL.format(host, port, db)

  /**
   * Connect to a given postgres URL.
   */
  def connect(url:String, user:String, pass:String) = {
    DriverManager.getConnection(url, user, pass)
  }

  /**
   * Connect to Postgres and return a PostgresReader instance.
   */
  def postgresReader(url:String, user:String, pass:String) = {
    new PostgresReader(connect(url, user, pass))
  }
}

/**
 * PostgresReader wraps a JDBC connection to a Postgres database.

 * It is NOT threadsafe, although the underlying connection object is. This
 * object is lightweight and you should instantiate one per thread, using the
 * same connection where possible.
 */
class PostgresReader (val conn:Connection) {
  val wkb = new WKBReader()

  /**
   * Prepare an SQL statement with the provided parameters.
   */
  def prepare(sql:String):PreparedStatement = conn.prepareStatement(sql)

  /**
   * Prepare an SQL statement with the provided parameters.
   */
  def prepare(sql:String, params:Array[Any]):PreparedStatement = {
    val stmt = conn.prepareStatement(sql)
    bind(stmt, params)
    stmt
  }

  /**
   * Prepare and run an SQL statement.
   */
  def run(sql:String):ResultSet = prepare(sql).executeQuery()

  /**
   * Prepare and run an SQL statement.
   */
  def run(sql:String, values:Array[Any]):ResultSet = {
    prepare(sql, values).executeQuery()
  }

  /**
   * Binds an array of values to a prepared statement.
   *
   * This method mutates the prepared statement.
   */
  def bind(stmt:PreparedStatement, values:Array[Any]) {
    for (i <- 0 until values.length) bindValue(stmt, i, values(i))
  }

  /**
   * Bind a value into a prepared statement at the specified position.
   *
   * This method mutates the prepared statement.
   */
  def bindValue(stmt:PreparedStatement, i:Int, value:Any) {
    value match {
      case b:Boolean => stmt.setBoolean(i, b)
      case d:Double => stmt.setDouble(i, d)
      case f:Float => stmt.setFloat(i, f)
      case i:Int => stmt.setInt(i, i)
      case l:Long => stmt.setLong(i, l)
      case s:String => stmt.setString(i, s)
      case _ => throw new Exception("can't handle %s".format(value))
    }
  }

  /**
   * Get the name of column i (0-indexed) of the given results.
   */
  def colName(m:ResultSetMetaData, i:Int) = m.getColumnName(i + 1)

  /**
   * Get the name of column i (0-indexed) of the given results.
   */
  def colType(m:ResultSetMetaData, i:Int) = m.getColumnName(i + 1)

  /**
   * Read a column of data from the ResultSet. This method uses reflection on
   * the result set to determine the correct type to use.
   */
  def getColumn[T](r:ResultSet, i:Int):T = getColumn[T](r, r.getMetaData, i)

  /**
   * Read a column of data from the ResultSet, given the ResultSetMetaData
   * object. This method uses reflection on the result set to determine the
   * correct types to use.
   */
  def getColumn[T](r:ResultSet, m:ResultSetMetaData, i:Int):T = {
    val pos = i + 1
    val result = m.getColumnType(pos) match {
      case Types.BLOB => r.getBlob(pos)
      case Types.BOOLEAN => r.getBoolean(pos)
      case Types.DATE => r.getDate(pos)
      case Types.DOUBLE => r.getDouble(pos)
      case Types.INTEGER => r.getInt(pos)
      case Types.VARCHAR => r.getString(pos)
      case Types.BIGINT => r.getInt(pos) //FIXME

      case 1111 => wkb.read(WKBReader.hexToBytes(r.getString(pos)))

      case n => sys.error("unhandled type: %s (%s)".format(colType(m, i), n))
    }
    result.asInstanceOf[T]
  }

  /**
   * Build an Array[Any] from the current row of the ResultSet.
   */
  def rowAsArray(r:ResultSet):Array[Any] = {
    val m = r.getMetaData
    val n = m.getColumnCount
    rowAsArray(r, m, n)
  }
  def rowAsArray(r:ResultSet, m:ResultSetMetaData, n:Int):Array[Any] = {
    val data = Array.ofDim[Any](n)
    for(i <- 0 until n) data(i) = getColumn[Any](r, m, i)
    data
  }

  /**
   * Build a Map[String, Any] from the current row of the ResultSet.
   */
  def rowAsMap(r:ResultSet):Map[String, Any] = {
    val m = r.getMetaData
    val n = m.getColumnCount
    rowAsMap(r, m, n)
  }
  def rowAsMap(r:ResultSet, m:ResultSetMetaData, n:Int):Map[String, Any] = {
    val data = Map.empty[String, Any]
    for(i <- 0 until n) data(colName(m, i)) = getColumn[Any](r, m, i)
    data
  }

  /**
   * Function used to build a T from a ResultSet's row.
   */
  type RowBuilder[T] = Function3[ResultSet, ResultSetMetaData, Int, T]

  /**
   * Creates an Array[T] from the ResultSet's rows, using the given builder.
   */
  def rowsAsArray[T:Manifest](r:ResultSet, f:RowBuilder[T]):Array[T] = {
    val m = r.getMetaData
    val n = m.getColumnCount
    val rows = ArrayBuffer.empty[T]
    while (r.next()) rows.append(f(r, m, n))
    rows.toArray
  }

  /**
   * Creates an Array[Array[Any]] from the ResultSet's rows.
   */
  def rowsAsArrayOfArrays(r:ResultSet) = rowsAsArray(r, rowAsArray _)

  /**
   * Creates an Array[Map[String, Any]] from the ResultSet's rows.
   */
  def rowsAsArrayOfMaps(r:ResultSet) = rowsAsArray(r, rowAsMap _)

  /**
   * Get an array of features from a query.
   */
  def getFeatures(sql:String, geometryIndex:Int):Array[Feature] = {
    getFeatures(sql, Array.empty, geometryIndex, -1)
  }

  /**
   * Get an array of features from a query.
   */
  def getFeatures(sql:String, geometryIndex:Int, valueIndex:Int):Array[Feature] = {
    getFeatures(sql, Array.empty, geometryIndex, valueIndex)
  }

  /**
   * Get an array of features from a parameterized query.
   */
  def getFeatures(sql:String, params:Array[Any], geometryIndex:Int, valueIndex:Int):Array[Feature] = {
    val s = prepare(sql, params)
    val features = buildFeatures(s.executeQuery(), geometryIndex, valueIndex)
    s.close()
    features
  }

  /**
   * Given a ResultSet, build an array of features.
   */
  def buildFeatures(r:ResultSet, geometryIndex:Int, valueIndex:Int) = {
    val m = r.getMetaData()
    val n = m.getColumnCount()

    assert(geometryIndex < n)
    assert(valueIndex < n)

    val features = ArrayBuffer.empty[Feature]
    while (r.next()) {
      features.append(buildFeature(r, m, n, geometryIndex, valueIndex))
    }
    features.toArray
  }

  /**
   * Build a Feature from a row of a ResultSet.
   */
  def buildFeature(r:ResultSet, m:ResultSetMetaData, n:Int, geometryIndex:Int, valueIndex:Int):Feature = {
    val attrs = Map.empty[String, Any]
    for (i <- 0 until n) {
      if (i != geometryIndex && i != valueIndex) {
        attrs(colName(m, i)) = getColumn[Any](r, m, i)
      }
    }

    val geom = getColumn[Geometry](r, m, geometryIndex)
    val value = if (valueIndex >= 0) getColumn[Int](r, m, valueIndex) else 0

    geom.getGeometryType match {
      case "Point" => buildPoint(geom, value, attrs)
      case "LineString" => buildLineString(geom, value, attrs)
      case "Polygon" => buildPolygon(geom, value, attrs)
      case "MultiPolygon" => buildMultiPolygon(geom, value, attrs)
      case "GeometryCollection" => sys.error("GeometryCollection is unsupported")
      case t => sys.error("unsupported type: %s".format(t))
    }
  }

  // helper functions for buildFeature

  def buildPoint(geom:Geometry, value:Int, attrs:Map[String, Any]) = {
    tr.Point(geom.getCoordinates()(0), value, attrs.toMap)
  }

  def buildLineString(geom:Geometry, value:Int, attrs:Map[String, Any]) = {
    tr.LineString(geom.getCoordinates(), value, attrs.toMap)
  }

  def buildPolygon(geom:Geometry, value:Int, attrs:Map[String,Any]) = {
    tr.Polygon(geom.getCoordinates(), value, attrs.toMap)
  }

  def buildMultiPolygon(geom:Geometry, value:Int, attrs:Map[String,Any]) = {
    val n = geom.getNumGeometries()
    val polygons = (0 until n).map {
      i => buildPolygon(geom.getGeometryN(i), value, attrs)
    }
    tr.MultiPolygon(polygons.toArray, value, attrs.toMap)
  }
}
