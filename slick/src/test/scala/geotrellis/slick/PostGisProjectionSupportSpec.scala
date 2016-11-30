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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest._
import slick.driver.PostgresDriver
import util._

class PostGisProjectionSupportSpec extends FlatSpec with Matchers with TestDatabase with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  object driver extends PostgresDriver with PostGisProjectionSupport {
    override val api = new API with PostGISProjectionAssistants with PostGISProjectionImplicits
  }
  import driver.api._

  class City(tag: Tag) extends Table[(Int,String,Projected[Point])](tag, "cities") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def geom = column[Projected[Point]]("geom")

    def * = (id, name, geom)
  }
  val CityTable = TableQuery[City]

  "ProjectedGeometry" should "not make Slick barf" in {
    try { db.run(CityTable.schema.drop).futureValue } catch { case e: Throwable =>  }
    db.run(CityTable.schema.create).futureValue

    db.run(CityTable.map(c => (c.name, c.geom)) += ("Megacity 1", Projected(Point(1,1), 43211))).futureValue

    db.run(CityTable.schema.drop).futureValue
  }

  class LineRow(tag: Tag) extends Table[(Int,Projected[Line])](tag, "lines") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def geom = column[Projected[Line]]("geom")

    def * = (id, geom)
  }

  it should "support PostGIS function mapping" in {
    val LineTable = TableQuery[LineRow]
    try { db.run(LineTable.schema.drop).futureValue } catch { case e: Throwable =>  }
    db.run(LineTable.schema.create).futureValue

    db.run(LineTable.map(_.geom) += Projected(Line(Point(1,1), Point(1,3)), 3131)).futureValue

    val q = for {
      line <- LineTable
    } yield line.geom.length

    db.run(q.result).futureValue.toList.head should equal (2.0)
  }

  it should "support PostGIS multi points" in {
    class MPRow(tag: Tag) extends Table[(Int,Projected[MultiPoint])](tag, "points") {
      def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      def geom = column[Projected[MultiPoint]]("geom")
      def * = (id, geom)
    }
    val MPTable = TableQuery[MPRow]

    try { db.run(MPTable.schema.drop).futureValue } catch { case e: Throwable =>  }
    db.run(MPTable.schema.create).futureValue

    db.run(MPTable.map(_.geom) += Projected(MultiPoint(Point(1,1), Point(2,2)), 3131)).futureValue

    val q = for {
      mp <- MPTable
    } yield {mp.geom.centroid}

    db.run(q.result).futureValue.toList.head should equal ( Projected(Point(1.5, 1.5), 3131) )
  }

  it should "handle hex strings starting with \\x" in {
    val wkb ="\\x002000000300000f110000000100000005c170b8793ccc8e80415ca9f4683a18dcc170b8793ccc8e8041631bf8457c1091c16ca9f4683a18dc41631bf8457c1091c16ca9f4683a18dc415ca9f4683a18dcc170b8793ccc8e80415ca9f4683a18dc"
    val interpreted = PostGisProjectionSupportUtils.readWktOrWkb(wkb)
    val poly = Polygon(Point(-17532819.799940586, 7514065.628545966), Point(-17532819.799940586, 10018754.171394618), Point(-15028131.257091932, 10018754.171394618), Point(-15028131.257091932, 7514065.628545966), Point(-17532819.799940586, 7514065.628545966))

    interpreted should be (poly)
  }

  it should "handle hex strings starting with 00" in {
    val wkb ="002000000300000f110000000100000005c170b8793ccc8e80415ca9f4683a18dcc170b8793ccc8e8041631bf8457c1091c16ca9f4683a18dc41631bf8457c1091c16ca9f4683a18dc415ca9f4683a18dcc170b8793ccc8e80415ca9f4683a18dc"
    val interpreted = PostGisProjectionSupportUtils.readWktOrWkb(wkb)
    val poly = Polygon(Point(-17532819.799940586, 7514065.628545966), Point(-17532819.799940586, 10018754.171394618), Point(-15028131.257091932, 10018754.171394618), Point(-15028131.257091932, 7514065.628545966), Point(-17532819.799940586, 7514065.628545966))

    interpreted should be (poly)
  }

}
