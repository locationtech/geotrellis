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

package geotrellis.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/**
  * Similar to [[com.typesafe.scalalogging.LazyLogging]]
  * but with @transient logger, to avoid potential serialization issues
  * even with loggers that survive serialization.
  */

trait LazyLogging {
  @transient protected lazy val logger: Logger = LazyLogging(this)
}

object LazyLogging {
  private[geotrellis]
  def apply(source: Any): Logger =
    Logger(LoggerFactory.getLogger(source.getClass.getName))
}
