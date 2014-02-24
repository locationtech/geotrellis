package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}

sealed trait Dimensions { private[feature] val geom: jts.Geometry }

trait AtLeastOneDimensions extends Dimensions
trait AtMostOneDimensions extends Dimensions

trait ZeroDimensions extends Dimensions 
                        with AtMostOneDimensions

trait OneDimensions extends Dimensions 
                       with AtMostOneDimensions
                       with AtLeastOneDimensions

trait TwoDimensions extends Dimensions
                       with AtLeastOneDimensions
