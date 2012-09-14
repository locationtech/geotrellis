package geotrellis.data.pg

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

import scala.collection.mutable.Map

import java.sql._
import java.util._
import java.lang._
import org.postgis.{DriverWrapper, PGgeometry}
import org.postgresql._

import geotrellis.data._

// This test is disabled for now because it won't work for most users.

//@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
//class PgSpec extends FunSpec with MustMatchers {
//  Class.forName("org.postgis.DriverWrapper")
//  describe("postgis connection") {
//    it ("should connect to a postgis server at 192.168.16.75") {
//      val url = "jdbc:postgresql_postGIS://192.168.16.75:5432/stroud"
//      val conn = DriverManager.getConnection(url, "jmarcus", "jmarcus")
//      val pgConn = conn.asInstanceOf[PGConnection]
//
//      val s = conn.createStatement(); 
//      val r = s.executeQuery("select the_geom,huc_id from watersheds where huc_id = '02040202'"); 
//      r.next()
//
//      val attrs = PgTypes.getColumnValuesAsMap(r)
//      /* 
//       * Retrieve the geometry as an object then cast it to the geometry type. 
//       * Print things out. 
//       */ 
//      val geom = r.getObject(1).asInstanceOf[PGgeometry]
//      val huc_id = r.getString(2)
//
//      val g = geom.getGeometry()
//      val point = g.getPoint(0)
//      val num_points = g.numPoints
//
//      huc_id must be === "02040202"
//      point.x must be === -8373064.212846053
//      num_points must be === 7
//
//      s.close() 
//      conn.close()
//    }
//
//    it ("should read the features from the table") {
//      val conf = Map("database" -> "stroud",
//                     "host" -> "192.168.16.75",
//                     "port" -> "5432",
//                     "table" -> "watersheds",
//                     "username" -> "jmarcus",
//                     "password" -> "jmarcus")
//
//      val reader = PostgisFeatureReader(conf)
//
//      val sql = "select the_geom, huc_id from watersheds limit 10"
//      val geometryIndex = 1
//      val valueIndex = 2
//      val features = reader.getFeatures(sql, geometryIndex, valueIndex)
//      //features.foreach {
//      //  f => println(f.asInstanceOf[geotrellis.geometry.Polygon].getCoordTuples.toList)
//      //}
//    }
//  }
//}
