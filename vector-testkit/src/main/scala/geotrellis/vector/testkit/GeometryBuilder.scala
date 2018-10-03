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

package geotrellis.vector.testkit

import geotrellis.vector._
import org.locationtech.jts.{geom => jts}
import org.locationtech.jts.geom.util.SineStarFactory
import org.locationtech.jts.util.GeometricShapeFactory

object GeometryBuilder {
  implicit def builderToGeom[T <: Geometry](b: GeometryBuilder[T]): T = b.build

  def polygon(f: GeometricShapeFactory => jts.Polygon): GeometryBuilder[Polygon] =
    new GeometryBuilder[Polygon] {
      val factory = new GeometricShapeFactory
      def build() = f(factory)
    }

  def line(f: GeometricShapeFactory => jts.LineString): GeometryBuilder[Line] =
    new GeometryBuilder[Line] {
      val factory = new GeometricShapeFactory
      def build() = f(factory)
    }
}

trait GeometryBuilder[T <: Geometry] {
  val factory: GeometricShapeFactory

  def build(): T

  def withEnvelope(extent: Extent): GeometryBuilder[T] = {
    factory.setEnvelope(new jts.Envelope(extent.xmin, extent.xmax, extent.ymin, extent.ymax))
    this
  }

  def withLowerLeftAt(p: Point): GeometryBuilder[T] = {
    factory.setBase(new jts.Coordinate(p.x, p.y))
    this
  }

  def setCenter(p: Point): GeometryBuilder[T] = {
    factory.setCentre(new jts.Coordinate(p.x, p.y))
    this
  }

  def withPointCount(count: Int): GeometryBuilder[T] = {
    factory.setNumPoints(count)
    this
  }

  def withSize(size: Double): GeometryBuilder[T] = {
    factory.setSize(size)
    this
  }

  def withWidth(width: Double): GeometryBuilder[T] = {
    factory.setWidth(width)
    this
  }

  def withHeight(height: Double): GeometryBuilder[T] = {
    factory.setSize(height)
    this
  }

  def rotatedBy(radians: Double): GeometryBuilder[T] = {
    factory.setRotation(radians)
    this
  }
}



object Rectangle { def apply() = GeometryBuilder.polygon(_.createRectangle) }
object Circle { def apply() = GeometryBuilder.polygon(_.createCircle) }
object Ellipse { def apply() = GeometryBuilder.polygon(_.createEllipse) }
object Squircle { def apply() = GeometryBuilder.polygon(_.createSquircle) }
object SuperCircle { def apply(power: Double) = GeometryBuilder.polygon(_.createSupercircle(power)) }

object Arc { 
  def apply(startAngle: Double, extent: Double) = 
    GeometryBuilder.line(_.createArc(startAngle, extent))
}

object ArcPolygon {
  def apply(startAngle: Double, extent: Double) =
    GeometryBuilder.polygon(_.createArcPolygon(startAngle, extent))
}

object SineStar {
  def apply(numArms: Int = 8, armLengthRatio: Double = 0.5): GeometryBuilder[Polygon] =
    new GeometryBuilder[Polygon] {
      val factory = {
        val f = new SineStarFactory
        f.setNumArms(numArms)
        f.setArmLengthRatio(armLengthRatio)
        f
      }

      def build(): Polygon = Polygon(factory.createSineStar.asInstanceOf[jts.Polygon])
    }
}
