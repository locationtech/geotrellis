/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.slick

import geotrellis.vector._

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import slick.driver.PostgresDriver
import util._

import java.util.Locale


class PostgisSpec extends FlatSpec with Matchers with TestDatabase with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  object driver extends PostgresDriver with PostGisSupport {
    override val api = new API with PostGISAssistants with PostGISImplicits
  }
  import driver.api._
  //import support of Subclasses of Geometry

  class SimpleCity(tag: Tag) extends Table[(Int,String)](tag, "simple_cities") {
    def id = column[Int]("id", O.PrimaryKey,  O.AutoInc)
    def name = column[String]("name")

    def * = (id, name)
  }
  val SimpleCityTable = TableQuery[SimpleCity]


  class City(tag: Tag) extends Table[(Int,String,Point)](tag, "cities") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def geom = column[Point]("geom")

    def * = (id, name, geom)
  }
  val CityTable = TableQuery[City]

  def createSchema() =
    try {
      db.run(CityTable.schema.create).futureValue
    } catch {
      case _: Throwable =>
        println("A script for setting up the PSQL environment necessary to run these tests can be found at scripts/slickTestDB.sh - requires a working docker setup")
    }

  def dropSchema()  =    db.run(CityTable.schema.drop).futureValue
  "Environment" should "be sane" in {

    try { db.run(SimpleCityTable.schema.drop).futureValue } catch { case e: Throwable =>  }

    val cities = Seq("washington", "london", "paris")

    db.run(SimpleCityTable.schema.create).futureValue
    db.run(SimpleCityTable.map(c => c.name) ++= cities).futureValue

    val q = for { c <- SimpleCityTable } yield c.name
    db.run(q.result).futureValue.toList should equal (cities)

    val q2 = for { c <- SimpleCityTable if c.id > 1 } yield c
    db.run(q2.delete).futureValue
    db.run( { for { c <- SimpleCityTable } yield c }.result).futureValue.toList.length should equal (1)

    val q3 = for { c <- SimpleCityTable } yield c
    db.run(q3.delete).futureValue
    db.run({ for { c <- SimpleCityTable } yield c }.result).futureValue.toList.length should equal (0)

    db.run(SimpleCityTable.schema.drop).futureValue
  }

  "Postgis driver" should "be able to insert geoms" in {
    try { db.run(CityTable.schema.drop).futureValue } catch { case e: Throwable =>  }

    createSchema()
    db.run(CityTable.map(c => (c.name, c.geom)) ++= data.map { d => (d._1, d._2) }).futureValue

    val q = for { c <- CityTable } yield (c.name, c.geom)

    db.run(q.result).futureValue.toList should equal (data.toList)

    dropSchema()
  }

  it should "be able to delete all" in {

    // Make sure things are clean
    // we probably shouldn't need this
    try { db.run(CityTable.schema.drop).futureValue } catch { case e: Throwable =>  }

    createSchema()
    db.run(CityTable.map(c => (c.name, c.geom)) ++= data.map { d => (d._1, d._2) }).futureValue

    val q1 = for { c <- CityTable } yield c
    db.run(q1.result).futureValue.toList.length should equal (data.length)

    val q2 = for { c <- CityTable } yield c
    db.run(q2.delete).futureValue

    val q3 = for { c <- CityTable } yield c
    db.run(q3.result).futureValue.toList.length should equal (0)

    dropSchema()
  }

  it should "be able to delete with geom where clause" in {
    // Make sure things are clean
    // we probably shouldn't need this
    try { db.run(CityTable.schema.drop).futureValue } catch { case e: Throwable =>  }

    createSchema()
    db.run(CityTable.map(c => (c.name, c.geom)) ++= data.map { d => (d._1, d._2) }).futureValue

    // 40.30, 78.32 -> Altoona,PA
    val bbox = bboxBuffer(78.32, 40.30, 0.01)

    val q = for {c <- CityTable if c.geom @&& bbox} yield c
    db.run(q.delete).futureValue

    val q2 = for { c <- CityTable } yield c.name

    db.run(q2.result).futureValue.toList should equal (data.map(_._1).filter(_ != "Altoona,PA").toList)

    db.run(CityTable.forceInsert(4000, "ATown",pt(-55.1,23.3))).futureValue

    val q3 = for { c <- CityTable if c.id =!= 4000 } yield c
    db.run(q3.delete).futureValue

    val q4 = for { c <- CityTable } yield c.name
    db.run(q4.result).futureValue.toList should equal (List("ATown"))

    dropSchema()
  }

  it should "be able to query with geo fcns" in {
    // Make sure things are clean
    // we probably shouldn't need this
    try { db.run(CityTable.schema.drop).futureValue } catch { case e: Throwable =>  }

    createSchema()
    db.run(CityTable.map(c => (c.name, c.geom)) ++= data.map { d => (d._1, d._2) }).futureValue

    // 40.30, 78.32 -> Altoona,PA
    val bbox = bboxBuffer(78.32, 40.30, 0.01)

    // Operator
    val q = for {
      c <- CityTable if c.geom @&& bbox // && -> overlaps
    } yield c.name


    db.run(q.result).futureValue.toList should equal (List("Altoona,PA"))

    // Function
    val dist = 0.5f
    val q2 = for {
      c1 <- CityTable
      c2 <- CityTable if c1.geom.distance(c2.geom) < dist && c1.name =!= c2.name
    } yield (c1.name, c2.name, c1.geom.distance(c2.geom))

    val q2format = db.run(q2.result).futureValue.toList map {
      case (n1,n2,d) => (n1,n2, "%1.4f".formatLocal(Locale.ENGLISH, d))
    }

    val jts = for {
      j1 <- data
      j2 <- data if j1._2.distance(j2._2) < dist && j1._1 != j2._1
    } yield (j1._1, j2._1, "%1.4f".formatLocal(Locale.ENGLISH, j1._2.distance(j2._2)))

    q2format should equal (jts.toList)

    // Output function
    val q3 = for {
      c <- CityTable if c.name === "Reading,PA"
    } yield c.geom.asGeoJSON()

    println(db.run(q3.result).futureValue.head)  // todo checki if this is correct
    db.run(q3.result).futureValue.head should equal ("""{"type":"Point","coordinates":[75.97,40.38]}""")  // it should be first

    dropSchema()
  }

  class OptCityRow(tag: Tag) extends Table[(Int,String,Option[Point])](tag, "cities") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def geom = column[Option[Point]]("geom")

    def * = (id, name, geom)
  }
  val OptCity = TableQuery[OptCityRow]

  it should "be able to handle optional fields" in {
    try { db.run(OptCity.schema.drop).futureValue } catch { case e: Throwable => }

    db.run(OptCity.schema.create).futureValue

    val cities = Seq(
      ("washington",Some(pt(-77.02,38.53))),
      ("london", None),
      ("paris", Some(pt(2.3470,48.8742)))
    )

    db.run(OptCity.map(c => (c.name, c.geom)) ++= cities).futureValue

    val q1 = for {
      c <- OptCity if !(c.geom isDefined)
    } yield (c.name, c.geom)
    db.run(q1.result).futureValue.toList should equal (List(("london", None)))

    val q2 = for {
      c <- OptCity if c.geom isDefined
    } yield c.name

    db.run(q2.result).futureValue.toList should equal (List("washington", "paris"))

    db.run(OptCity.schema.drop).futureValue
  }

  it should "be able to query with geo fcns on null fields" in {
    // Make sure things are clean
    // we probably shouldn't need this
    try { db.run(OptCity.schema.drop).futureValue } catch { case e: Throwable =>  }

    val data2 = data.map { case (s, g) => s -> Some(g)}

   db.run(OptCity.schema.create).futureValue
   db.run(OptCity.map(c => (c.name, c.geom)) ++= data2).futureValue

    // 40.30, 78.32 -> Altoona,PA
    val bbox = bboxBuffer(78.32, 40.30, 0.01)

    val q = for {
      c <- OptCity if c.geom @&& bbox // && -> overlaps
    } yield c.name

    db.run(q.result).futureValue should equal (List("Altoona,PA"))

    db.run(OptCity.schema.drop).futureValue
  }

  it should "be able to handle generic geom fields" in {
    // if this compiles we're golden
    class Foo(tag: Tag) extends Table[(Int,String,Option[Geometry])](tag, "foo") {

      def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      def name = column[String]("name")
      def geom = column[Option[Geometry]]("geom")

      def * = (id, name, geom)
    }

    class Bar(tag: Tag) extends Table[(Int,String,Geometry)](tag, "bar") {
      def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      def name = column[String]("name")
      def geom = column[Geometry]("geom")

      def * = (id, name, geom)
    }
  }

  class LineRow(tag: Tag) extends Table[(Int,Line)](tag, "lines") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def geom = column[Line]("geom")

    def * = (id, geom)
  }
  val LineTable = TableQuery[LineRow]

  it should "wrap PostGIS functions on Geometry Fields" in {
    try { db.run(LineTable.schema.drop).futureValue } catch { case e: Throwable =>  }
    db.run(LineTable.schema.create).futureValue

    db.run(LineTable.map(_.geom) += Line(Point(1,1), Point(1,2))).futureValue

    val q = for {
      line <- LineTable
    } yield line.geom.length

    println(q.result.statements)
    println(db.run(q.result).futureValue.toList)
  }
}
