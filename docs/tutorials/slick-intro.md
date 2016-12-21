The slick module of GeoTrellis is a combination of Minglei Tu's [excellent
slick-pg project](https://github.com/tminglei/slick-pg) and some
modifications which streamline its use in persisting
`geotrellis.vector.Geometry` instances on Postgres/PostGIS.

In the following example, we walk through the steps that you'd take if you
wanted to create a catalog of city halls backed by Postgres:

```scala
import geotrellis.slick._
import geotrellis.vector._

// You want to be able to store a collection of city halls.
// To this end, you'll want a case class for holding your data.
case class CityHall(
  id: Int,
  city: String,
  location: Projected[Point]
)

// You'll also need to define a mapping from your case class to your DB
class CityHalls(tag: Tag) extends Table[CityHall](tag, "cityhalls") {
  def id = column[Int]("id", O.PrimaryKey)
  def city = column[String]("city")
  def location = column[Projected[Point]]("location")
  def * = (id, city, location)  // unapply for your CityHall instance
}

// Some companion object methods on CityHall
object CityHall {
  // Somehow, you'll have to instantiate a database
  val db: Database = ???

  // Here, the mapping defined in CityHalls is registered
  private val cityhalls = TableQuery[CityHalls]

  /** The following lines are useful if you'd like to be able to create
    * instances of cityhall which have correct IDs from the start
    */
  private val creationQuery =
    cityhalls returning cityhalls.map(_.id) into ((cityhall,id) => cityhall.copy(id=id))
  def create(name: String, location: Projected[Point]): Future[CityHall] = {
    val action = creationQuery += CityHall(0, name, location)
    db.run(action)
  }
}

// We can now create a persisted CityHall
CityHall.create("Philly", Point(39.9524, -75.164).withSRID(3857))
```
