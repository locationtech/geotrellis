package geotrellis.slick

import scala.slick.driver.PostgresDriver


trait PostGisDriver extends PostgresDriver
	with PostGisSupport
{
  override val Implicit = new Implicits with PostGisImplicits
  override val simple = new Implicits with SimpleQL with PostGisImplicits with PostGisAssistants
}

object PostGisDriver extends PostGisDriver