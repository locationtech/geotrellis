package geotrellis.slick

import com.typesafe.config.{ConfigFactory,Config}
import org.scalatest._

import scala.slick.driver.PostgresDriver.simple._

object TestDatabase {
  def newInstance = {
    val config = ConfigFactory.load
    val pguser = config.getString("db.user")
    val pgpass = config.getString("db.password")
    val pgdb = config.getString("db.database")
    val pghost = config.getString("db.host")

    val s = s"jdbc:postgresql://$pghost/$pgdb"
    println(s"Connecting to $s")
    Database.forURL("jdbc:postgresql://" + pghost + "/" + pgdb,
      driver="org.postgresql.Driver",
      user=pguser,
      password=pgpass)

  }
}

trait TestDatabase extends BeforeAndAfterAll { self: Suite =>
  protected var db: Database = null

  override def beforeAll() = {
    db = TestDatabase.newInstance
  }
}
