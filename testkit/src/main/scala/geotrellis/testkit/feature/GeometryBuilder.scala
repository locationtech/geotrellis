package geotrellis.testkit.feature

import geotrellis.feature._
import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.util.SineStarFactory
import com.vividsolutions.jts.util.GeometricShapeFactory

object GeometryBuilder {
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

  def withBoundingBox(boundingBox: BoundingBox): GeometryBuilder[T] = {
    factory.setEnvelope(boundingBox.jtsEnvelope)
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
