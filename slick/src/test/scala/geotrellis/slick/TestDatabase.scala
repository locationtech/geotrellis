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

import com.typesafe.config.{ConfigFactory,Config}
import org.scalatest._

import slick.driver.PostgresDriver.api._

object TestDatabase {
  def newInstance = {
    val config = ConfigFactory.load
    val pguser = config.getString("db.user")
    val pgpass = config.getString("db.password")
    val pgdb = config.getString("db.database")
    val pghost = config.getString("db.host")

    val s = s"jdbc:postgresql://$pghost/$pgdb"
    println(s"Connecting to $s")

    Database.forURL(
      "jdbc:postgresql://" + pghost + "/" + pgdb,
      driver="org.postgresql.Driver",
      user=pguser,
      password=pgpass
    )
  }
}

trait TestDatabase extends BeforeAndAfterAll { self: Suite =>
  protected var db: Database = null

  override def beforeAll() = {
    val config = ConfigFactory.load
    db = TestDatabase.newInstance
  }
}
