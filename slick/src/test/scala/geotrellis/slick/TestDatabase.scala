package geotrellis.slick

import com.typesafe.config.{ConfigFactory,Config}
import org.scalatest._

import slick.driver.PostgresDriver.api._

object TestDatabase {
  def newInstance = {
    val config = ConfigFactory.load
    val pguser = "postgres"
    val pgpass = "postgres"
    val pgdb = "travis_ci_test"
    val pghost = "localhost"

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
