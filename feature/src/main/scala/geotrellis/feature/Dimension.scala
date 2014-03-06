package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}

sealed trait Dimensions { private[feature] val geom: jts.Geometry }

trait AtLeastOneDimension extends Dimensions
trait AtMostOneDimension extends Dimensions

trait ZeroDimensions extends Dimensions 
                        with AtMostOneDimension

trait OneDimension extends Dimensions 
                       with AtMostOneDimension
                       with AtLeastOneDimension

trait TwoDimensions extends Dimensions
                       with AtLeastOneDimension
