package geotrellis.spark.io

import scala.collection.immutable
import scala.concurrent.duration._
import akka.util.Timeout

package object cassandra {

  final val DefaultHost = "127.0.0.1"

  implicit val DefaultTimeout = Timeout(5.seconds)
}
