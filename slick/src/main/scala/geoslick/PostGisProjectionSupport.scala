package geotrellis.slick

import scala.slick.driver.JdbcDriver
import scala.slick.lifted.Column
import scala.reflect.ClassTag
import scala.slick.ast.{ScalaBaseType}
import scala.slick.jdbc.{PositionedResult, PositionedParameters}

import geotrellis.feature._
import geotrellis.feature.io._

/** 
 * This class provides column types and extension methods to work with Geometry columns
 *  associated with an SRID in PostGIS.
 *
 * Sample Usage: 
 * <code>
 * val PostGIS = new PostGisProjectionSupport(PostgresDriver)
 * import PostGIS._
 * 
 * class City(tag: Tag) extends Table[(Int,String,Projected[Point])](tag, "cities") {      
 *   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
 *   def name = column[String]("name")
 *   def geom = column[Projected[Point]]("geom")
 *   def * = (id, name, geom)
 * }
 * </code>
 *
 * based on [[package com.github.tminglei.slickpg.PgPostGISSupport]]
 */
class PostGisProjectionSupport(override val driver: JdbcDriver) extends PostGisExtensions { 
  import PostGisProjectionSupportUtils._

  type GEOMETRY           = Projected[Geometry]
  type POINT              = Projected[Point]
  type LINESTRING         = Projected[Line]
  type POLYGON            = Projected[Polygon]
  type GEOMETRYCOLLECTION = Projected[GeometryCollection]

  implicit val geometryTypeMapper           = new ProjectedGeometryJdbcType[GEOMETRY]
  implicit val pointTypeMapper              = new ProjectedGeometryJdbcType[POINT]
  implicit val lineTypeMapper               = new ProjectedGeometryJdbcType[LINESTRING]
  implicit val polygonTypeMapper            = new ProjectedGeometryJdbcType[POLYGON]
  implicit val geometryCollectionTypeMapper = new ProjectedGeometryJdbcType[GEOMETRYCOLLECTION]
  // implicit val multiPointTypeMapper = new ProjectedGeometryJdbcType[ProjectedGeometry[MultiPoint], MultiPoint]
  // implicit val multiPolygonTypeMapper = new ProjectedGeometryJdbcType[ProjectedGeometry[MultiPolygon], MultiPolygon]
  // implicit val multiLineTypeMapper = new ProjectedGeometryJdbcType[ProjectedGeometry[MultiLine], MultiLine]

  implicit def geometryColumnExtensionMethods[G1 <: GEOMETRY](c: Column[G1]) = 
    new GeometryColumnExtensionMethods[G1, G1](c)
  
  implicit def geometryOptionColumnExtensionMethods[G1 <: GEOMETRY](c: Column[Option[G1]]) = 
    new GeometryColumnExtensionMethods[G1, Option[G1]](c)

  class ProjectedGeometryJdbcType[T <: Projected[Geometry] :ClassTag] extends driver.DriverJdbcType[T] {
    override def scalaType = ScalaBaseType[T]
    
    override def sqlTypeName: String = "geometry"
    
    override def hasLiteralForm: Boolean = false
    
    override def valueToSQLLiteral(v: T) = toLiteral(v)

    def zero: T = null.asInstanceOf[T]

    def sqlType: Int = java.sql.Types.OTHER

    def setValue(v: T, p: PositionedParameters) = p.setBytes(WKB.write(v.geom, v.srid))

    def setOption(v: Option[T], p: PositionedParameters) = if (v.isDefined) setValue(v.get, p) else p.setNull(sqlType)

    def nextValue(r: PositionedResult): T = r.nextStringOption().map(fromLiteral[T]).getOrElse(zero)

    def updateValue(v: T, r: PositionedResult) = r.updateBytes(WKB.write(v.geom, v.srid))
  }
}

object PostGisProjectionSupportUtils {  
  def toLiteral(pg: Projected[Geometry]): String = WKT.write(pg.geom)

  def fromLiteral[T <: Projected[_]](value: String): T = 
    splitRSIDAndWKT(value) match {
      case (srid, wkt) =>
        val geom =
          if (wkt.startsWith("00") || wkt.startsWith("01"))
            WKB.read[Geometry](wkt)
          else 
            WKT.read[Geometry](wkt)

        Projected(geom, srid).asInstanceOf[T]
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