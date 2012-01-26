package trellis.data

import com.vividsolutions.jts.geom.Geometry

import org.postgis.{DriverWrapper, PGgeometry}

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PostgresTest extends FunSuite {
  Class.forName("org.postgis.DriverWrapper")

  if (false) {
    val host = "127.0.0.1"
    val port = 5432
    val db = "trellis-test"

    val user = "test"
    val pass = "test"

    val url = "jdbc:postgresql_postGIS://127.0.0.1:5432/trellis-test"

    test("url creation") {
      assert(Postgres.url(host, port, db) === url)
    }

    test("basic query") {
      val reader = Postgres.postgresReader(url, user, pass)
      
      val s = reader.prepare("select pk, name from points order by name")
      val r = s.executeQuery()
      r.next()
      val m = reader.rowAsMap(r)
      val a = reader.rowAsArray(r)

      assert(m("name") === "alpha")
      assert(a(1) == m("name"))

      s.close() 
      reader.conn.close()
    }

    test("basic points") {
      val reader = Postgres.postgresReader(url, user, pass)
      
      val sql = "select pk, the_geom from points order by name"
      val features = reader.getFeatures(sql, 1, 0)

      println(features(0))
      assert(features.length === 3)

      import trellis.geometry.Point

      val p0 = features(0).asInstanceOf[Point]

      assert(p0.x === 10)
      assert(p0.y === -20)

      reader.conn.close()
    }
  }
}
