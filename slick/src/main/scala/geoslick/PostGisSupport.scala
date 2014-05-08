package geotrellis.slick

import scala.slick.driver.JdbcDriver
import scala.slick.lifted.Column
import scala.reflect.ClassTag
import scala.slick.ast.{ScalaBaseType}
import scala.slick.jdbc.{PositionedResult, PositionedParameters}

import geotrellis.feature._
import geotrellis.feature.io._

/** based on [[package com.github.tminglei.slickpg.PgPostGISSupport]] */
class PostGisSupport(override val driver: JdbcDriver) extends PostGisExtensions { 
  import PostGisSupportUtils._

  type GEOMETRY           = Geometry
  type POINT              = Point
  type LINESTRING         = Line
  type POLYGON            = Polygon
  type GEOMETRYCOLLECTION = GeometryCollection

  implicit val geometryTypeMapper = new GeometryJdbcType[GEOMETRY]
  implicit val pointTypeMapper = new GeometryJdbcType[POINT]
  implicit val lineTypeMapper = new GeometryJdbcType[LINESTRING]
  implicit val polygonTypeMapper = new GeometryJdbcType[POLYGON]
  implicit val geometryCollectionTypeMapper = new GeometryJdbcType[GEOMETRYCOLLECTION]  
  implicit val multiPointTypeMapper = new GeometryJdbcType[MultiPoint]
  implicit val multiPolygonTypeMapper = new GeometryJdbcType[MultiPolygon]
  implicit val multiLineTypeMapper = new GeometryJdbcType[MultiLine]

  implicit def geometryColumnExtensionMethods[G1 <: GEOMETRY](c: Column[G1]) = 
    new GeometryColumnExtensionMethods[G1, G1](c)
  
  implicit def geometryOptionColumnExtensionMethods[G1 <: GEOMETRY](c: Column[Option[G1]]) = 
    new GeometryColumnExtensionMethods[G1, Option[G1]](c)

  
  class GeometryJdbcType[T <: Geometry : ClassTag] extends driver.DriverJdbcType[T] {
    override def scalaType = ScalaBaseType[T]
    
    override def sqlTypeName: String = "geometry"
    
    override def hasLiteralForm: Boolean = false
    
    override def valueToSQLLiteral(v: T) = toLiteral(v)

    def zero: T = null.asInstanceOf[T]

    def sqlType: Int = java.sql.Types.OTHER

    def setValue(v: T, p: PositionedParameters) = p.setBytes(WKB.write(v))

    def setOption(v: Option[T], p: PositionedParameters) = if (v.isDefined) setValue(v.get, p) else p.setNull(sqlType)

    def nextValue(r: PositionedResult): T = r.nextStringOption().map(fromLiteral[T]).getOrElse(zero)

    def updateValue(v: T, r: PositionedResult) = r.updateBytes(WKB.write(v))
  }
}

object PostGisSupportUtils {  
  def toLiteral(geom: Geometry): String = WKT.write(geom)

  def fromLiteral[T <: Geometry](value: String): T = {
    splitRSIDAndWKT(value) match {
      case (srid, wkt) => { //TODO - SRID is ignored
        if (wkt.startsWith("00") || wkt.startsWith("01"))
          WKB.read[T](wkt)
        else 
          WKT.read[T](wkt)
      }
    }
  }

  /** copy from [[org.postgis.PGgeometry#splitSRID]] */
  private def splitRSIDAndWKT(value: String): (Int, String) = {
    if (value.startsWith("SRID=")) {
      val index = value.indexOf(';', 5) // srid prefix length is 5
      if (index == -1) {
        throw new java.sql.SQLException("Error parsing Geometry - SRID not delimited with ';' ")
      } else {
        val srid = Integer.parseInt(value.substring(0, index))
        val wkt = value.substring(index + 1)
        (srid, wkt)
      }
    } else (-1, value)
  }
}