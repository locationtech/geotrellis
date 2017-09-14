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

import com.typesafe.config.ConfigFactory
import com.vividsolutions.jts.geom
import com.vividsolutions.jts.geom.PrecisionModel
import com.vividsolutions.jts.precision.GeometryPrecisionReducer

import scala.util.Try

private[vector] object GeomFactory {

  val precisionType = Try(ConfigFactory.load().getString("geotrellis.jts.precision.type").toLowerCase).getOrElse("floating")

  val precisionModel = precisionType match {
    case "floating" => new PrecisionModel()
    case "floating_single" => new PrecisionModel(PrecisionModel.Type.FLOATING_SINGLE)
    case "fixed" => 
      val scale = Try(new PrecisionModel(ConfigFactory.load().getDouble("geotrellis.jts.precision.scale"))).getOrElse(1e12)
      new PrecisionModel(scale)
  }

  val factory = new geom.GeometryFactory(precisionModel)

  // 12 digits is maximum to avoid [[TopologyException]], see http://tsusiatsoftware.net/jts/jts-faq/jts-faq.html#D9
  lazy val simplifier = new GeometryPrecisionReducer(new PrecisionModel(1e12))

}
