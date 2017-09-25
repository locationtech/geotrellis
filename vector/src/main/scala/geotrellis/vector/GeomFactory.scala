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

package geotrellis.vector

import geotrellis.util.LazyLogging

import com.typesafe.config.ConfigFactory
import com.vividsolutions.jts.geom
import com.vividsolutions.jts.geom.PrecisionModel
import com.vividsolutions.jts.precision.GeometryPrecisionReducer

import scala.util.Try

private[vector] object GeomFactory extends LazyLogging {

  val precisionType = {
    val setting = Try(ConfigFactory.load().getString("geotrellis.jts.precision.type").toLowerCase)
    if (setting.isSuccess)
      setting.get
    else {
      logger.warn("No value specified for geotrellis.jts.precision.type; falling back to \"floating\"")
      "floating"
    }
  }

  val precisionModel = precisionType match {
    case "floating" => new PrecisionModel()
    case "floating_single" => new PrecisionModel(PrecisionModel.FLOATING_SINGLE)
    case "fixed" => 
      val scaleFromConfig = Try(ConfigFactory.load().getDouble("geotrellis.jts.precision.scale"))
      val scale = 
        if (scaleFromConfig.isSuccess)
          scaleFromConfig.get
        else {
          logger.warn("No value specified in application.conf for geotrellis.jts.precision.scale; using default")
          1e12
        }
      new PrecisionModel(scale)
    case s => throw new IllegalArgumentException(s"""Unrecognized JTS precision model, ${precisionType}; expected "floating", "floating_single", or "fixed" """)
  }

  val factory = new geom.GeometryFactory(precisionModel)

  // 12 digits is maximum to avoid [[TopologyException]], see https://web.archive.org/web/20160226031453/http://tsusiatsoftware.net/jts/jts-faq/jts-faq.html#D9
  lazy val simplifier = {
    val simplificationPrecision = Try(ConfigFactory.load().getDouble("geotrellis.jts.simplification.scale"))
    val scale = 
      if (simplificationPrecision.isSuccess)
        simplificationPrecision.get
      else {
        logger.warn("No geotrellis.jts.simplification.scale given, assuming default value")
        1e12
      }
    new GeometryPrecisionReducer(new PrecisionModel(scale))
  }

}
