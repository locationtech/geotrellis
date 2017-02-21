package geotrellis.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/**
  * Similar to [[com.typesafe.scalalogging.LazyLogging]]
  * but with @transient logger, to avoid potential serialization issues
  * even with loggers that survive serialization.
  */

trait LazyLogging {
  @transient protected lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName))
}
