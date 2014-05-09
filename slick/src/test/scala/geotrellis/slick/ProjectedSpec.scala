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
 */

package geotrellis.slick

import org.scalatest._

import geotrellis.feature._
import scala.slick.driver.PostgresDriver

import util._


class ProjectedSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter {
  val driver = PostgresDriver
  import driver.simple._
  //import support for Subclasses of ProjectedGeometry
  val projected = new PostGisProjectionSupport(driver)
  import projected._

  val pguser = scala.util.Properties.envOrElse("PGUSER","postgres")
  val pgpass = scala.util.Properties.envOrElse("PGPASS","postgres")
  val pgdb = scala.util.Properties.envOrElse("PGDB","chicago_gtfs")
  val pghost = scala.util.Properties.envOrElse("PGHOST","localhost:5432")

  val db = Database.forURL("jdbc:postgresql://" + pghost + "/" + pgdb,
                           driver="org.postgresql.Driver",
                           user=pguser,
                           password=pgpass)


  class City(tag: Tag) extends Table[(Int,String,Projected[Point])](tag, "cities") {      
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def geom = column[Projected[Point]]("geom")

    def * = (id, name, geom)
  }
  val CityTable = TableQuery[City]

  "ProjectedGeometry" should "not make Slick barf" in {    
    db withSession { implicit  s =>    
      try { CityTable.ddl.drop } catch { case e: Throwable =>  }
      CityTable.ddl.create

      CityTable += (0, "Megacity 1", Projected(Point(1,1), 43211))

      CityTable.ddl.drop
    }
  }

  class LineRow(tag: Tag) extends Table[(Int,Projected[Line])](tag, "lines") {      
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def geom = column[Projected[Line]]("geom")

    def * = (id, geom)
  }
  val LineTable = TableQuery[LineRow]

  it should "support PostGIS function mapping" in {
    db withSession { implicit s => 
      try { LineTable.ddl.drop } catch { case e: Throwable =>  }
      LineTable.ddl.create

      LineTable += (0, Projected(Line(Point(1,1), Point(1,3)), 3131))

      val q = for {
        line <- LineTable
      } yield (line.geom.length)
      
      q.list.head should equal (2.0)
    }
  }
}