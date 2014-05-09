/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: Adam Hinz, Azavea
 */

package geotrellis.slick

import org.scalatest._

import geotrellis.feature._
import scala.slick.driver.PostgresDriver

import util._

class PostgisSpec extends FlatSpec with ShouldMatchers {
  val driver = PostgresDriver
  import driver.simple._
  //import support of Subclasses of Geometry
  val postgis = new PostGisSupport(driver)
  import postgis._

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

  val pguser = scala.util.Properties.envOrElse("PGUSER","postgres")
  val pgpass = scala.util.Properties.envOrElse("PGPASS","postgres")
  val pgdb = scala.util.Properties.envOrElse("PGDB","chicago_gtfs")
  val pghost = scala.util.Properties.envOrElse("PGHOST","localhost:5432")

  val db = Database.forURL("jdbc:postgresql://" + pghost + "/" + pgdb,
                           driver="org.postgresql.Driver",
                           user=pguser,
                           password=pgpass)

  "Environment" should "be sane" in {    
    db withSession { implicit s =>     
      try { SimpleCityTable.ddl.drop } catch { case e: Throwable =>  }
      
      val cities = Seq("washington", "london", "paris")

      SimpleCityTable.ddl.create
      SimpleCityTable ++= cities.map{ d => (0, d) }

      val q = for { c <- SimpleCityTable } yield c.name

      q.list should equal (cities)

      val q2 = for { c <- SimpleCityTable if c.id > 1 } yield c
      q2.delete

      { for { c <- SimpleCityTable } yield c }.list.length should equal (1)

      val q3 = for { c <- SimpleCityTable } yield c
      q3.delete

      { for { c <- SimpleCityTable } yield c }.list.length should equal (0)

      SimpleCityTable.ddl.drop
    }
  }

  "Postgis driver" should "be able to insert geoms" in {
    db withSession { implicit s =>
      try { CityTable.ddl.drop } catch { case e: Throwable =>  }

      CityTable.ddl.create
      CityTable ++= data.map{ d => (0, d._1, d._2) }

      val q = for { c <- CityTable } yield (c.name, c.geom)

      q.list should equal (data.toList)
                                  
      CityTable.ddl.drop
    }
  }

  it should "be able to delete all" in {
    db withSession { implicit s =>
      // Make sure things are clean
      // we probably shouldn't need this
      try { CityTable.ddl.drop } catch { case e: Throwable =>  }

      CityTable.ddl.create
      CityTable ++= data.map{ d => (0, d._1, d._2) }      

      val q1 = for { c <- CityTable } yield c
      q1.list.length should equal (data.length)

      val q2 = for { c <- CityTable } yield c
      q2.delete

      val q3 = for { c <- CityTable } yield c
      q3.list.length should equal (0)
                                  
      CityTable.ddl.drop    
    }
  }

  it should "be able to delete with geom where clause" in {
    db withSession { implicit s =>
      // Make sure things are clean
      // we probably shouldn't need this
      try { CityTable.ddl.drop } catch { case e: Throwable =>  }

      CityTable.ddl.create
      CityTable ++= data.map{ d => (0, d._1, d._2) }      

      // 40.30, 78.32 -> Altoona,PA
      val bbox = bboxBuffer(78.32, 40.30, 0.01)
      
      val q = for { c <- CityTable if c.geom @&& bbox } yield c
      q.delete

      val q2 = for { c <- CityTable } yield c.name
      q2.list should equal (data.map(_._1).filter(_ != "Altoona,PA").toList)

      CityTable.forceInsert(4000, "ATown",pt(-55.1,23.3))

      val q3 = for { c <- CityTable if c.id =!= 4000 } yield c
      q3.delete

      val q4 = for { c <- CityTable } yield c.name
      q4.list should equal (List("ATown"))
                                  
      CityTable.ddl.drop    
    }
  }

  it should "be able to query with geo fcns" in {
    db withSession { implicit s =>
      // Make sure things are clean
      // we probably shouldn't need this
      try { CityTable.ddl.drop } catch { case e: Throwable =>  }

      CityTable.ddl.create
      CityTable ++= data.map{ d => (0, d._1, d._2) }      

      // 40.30, 78.32 -> Altoona,PA
      val bbox = bboxBuffer(78.32, 40.30, 0.01)
      
      // Operator
      val q = for {
          c <- CityTable if c.geom @&& bbox // && -> overlaps
        } yield c.name

      q.list should equal (List("Altoona,PA"))
      
      // Function
      val dist = 0.5f
      val q2 = for {
          c1 <- CityTable
          c2 <- CityTable if c1.geom.distance(c2.geom) < dist && c1.name =!= c2.name
        } yield (c1.name, c2.name, c1.geom.distance(c2.geom))

      val q2format = q2.list map {
          case (n1,n2,d) => (n1,n2,"%1.4f" format d)
        }

      val jts = for {
          j1 <- data
          j2 <- data if j1._2.distance(j2._2) < dist && j1._1 != j2._1
        } yield (j1._1, j2._1, "%1.4f" format j1._2.distance(j2._2))

      q2format should equal (jts.toList)
          
      // Output function
      val q3 = for {
          c <- CityTable if c.name === "Reading,PA"
        } yield c.geom.asGeoJSON()

      println(q3.first)
      q3.first should equal ("""{"type":"Point","coordinates":[75.97,40.38]}""")

      CityTable.ddl.drop
    }
  }

  class OptCityRow(tag: Tag) extends Table[(Int,String,Option[Point])](tag, "cities") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def geom = column[Option[Point]]("geom")

    def * = (id, name, geom)
  }
  val OptCity = TableQuery[OptCityRow]

  it should "be able to handle optional fields" in {
    db withSession { implicit s =>
      try { OptCity.ddl.drop } catch { case e: Throwable => }

      OptCity.ddl.create

      val cities = Seq((0, "washington",Some(pt(-77.02,38.53))),
                       (0, "london", None),
                       (0, "paris", Some(pt(2.3470,48.8742))))

      OptCity ++= cities

      val q1 = for {
          c <- OptCity if c.geom isNull
        } yield (c.name, c.geom)

      q1.list should equal (List(("london",None)))

      val q2 = for {
          c <- OptCity if c.geom isNotNull
        } yield c.name

      q2.list should equal (List("washington","paris"))

      OptCity.ddl.drop
    }
  }

  it should "be able to query with geo fcns on null fields" in {
    db withSession { implicit s =>
      // Make sure things are clean
      // we probably shouldn't need this
      try { OptCity.ddl.drop } catch { case e: Throwable =>  }

      val data2 = data.map {
          case (s,g) => (0, s, Some(g))
        }

      OptCity.ddl.create
      OptCity ++= data2

      // 40.30, 78.32 -> Altoona,PA
      val bbox = bboxBuffer(78.32, 40.30, 0.01)
      
      val q = for {
          c <- OptCity if c.geom @&& bbox // && -> overlaps
        } yield c.name

      q.list should equal (List("Altoona,PA"))

      OptCity.ddl.drop
    }
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
    db withSession { implicit s => 
      try { LineTable.ddl.drop } catch { case e: Throwable =>  }
      LineTable.ddl.create

      LineTable += (0, Line(Point(1,1), Point(1,2)))

      val q = for {
        line <- LineTable
      } yield (line.geom.length)

      println(q.selectStatement)
      println(q.list)
    }
  }
}
