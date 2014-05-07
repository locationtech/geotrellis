package geotrellis.slick

import org.scalatest._

import geotrellis.feature._
import scala.slick.driver.PostgresDriver

import util._

trait SomePostGisDriver extends PostgresDriver
  with PostGisProjectionSupport
{
  override val Implicit = new Implicits with PostGisImplicits
  override val simple = new Implicits with SimpleQL with PostGisImplicits with PostGisAssistants
}

object SomePostGisDriver extends SomePostGisDriver

class ProjectedGeometrySpec extends FlatSpec with ShouldMatchers with BeforeAndAfter {
  import SomePostGisDriver.simple._

  val pguser = scala.util.Properties.envOrElse("PGUSER","postgres")
  val pgpass = scala.util.Properties.envOrElse("PGPASS","postgres")
  val pgdb = scala.util.Properties.envOrElse("PGDB","chicago_gtfs")
  val pghost = scala.util.Properties.envOrElse("PGHOST","localhost:5432")

  val db = Database.forURL("jdbc:postgresql://" + pghost + "/" + pgdb,
                           driver="org.postgresql.Driver",
                           user=pguser,
                           password=pgpass)


  class City(tag: Tag) extends Table[(Int,String,ProjectedPoint)](tag, "cities") {      
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def geom = column[ProjectedPoint]("geom")

    def * = (id, name, geom)
  }
  val CityTable = TableQuery[City]

  "ProjectedGeometry" should "not make Slick barf" in {    
    db withSession { implicit  s =>    
      try { CityTable.ddl.drop } catch { case e: Throwable =>  }
      CityTable.ddl.create

      CityTable += (0, "Megacity 1", ProjectedPoint(Point(1,1), 43211))

      CityTable.ddl.drop
    }
  }

  class LineRow(tag: Tag) extends Table[(Int,ProjectedLine)](tag, "lines") {      
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def geom = column[ProjectedLine]("geom")

    def * = (id, geom)
  }
  val LineTable = TableQuery[LineRow]

  it should "support PostGIS function mapping" in {
    db withSession { implicit s => 
      try { LineTable.ddl.drop } catch { case e: Throwable =>  }
      LineTable.ddl.create

      LineTable += (0, ProjectedLine(Line(Point(1,1), Point(1,3)), 3131))

      val q = for {
        line <- LineTable
      } yield (line.geom.length)
      
      q.list.head should equal (2.0)
    }
  }

  ignore should "bake together with PostGisSupport" in {
    //TODO - this will not work now because the members clash
    //  the answer is to do imports similar to JodaTime
    
    // trait SomePostGisDriver extends PostgresDriver
    //   with PostGisProjectionSupport
    //   with PostGisSupport
    // {
    //   override val Implicit = new Implicits with PostGisImplicits
    //   override val simple = new Implicits with SimpleQL with PostGisImplicits with PostGisAssistants
    // }
  } 
}