package geotrellis.layers

import geotrellis.util._

import scala.util.{Failure, Success, Try}


package object util {
  def threadsFromString(str: String): Int =
    str match {
      case "default" => Runtime.getRuntime.availableProcessors
      case s         => Try(s.toInt).getOrElse(Runtime.getRuntime.availableProcessors)
    }
}
