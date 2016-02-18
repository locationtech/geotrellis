#geotrellis.proj4

This module includes a slightly modified version of the proj4 java port, proj4j, as well as a lightweight scala wrapper around its projection and reprojection facilities. When reading the source keep in mind that `Transform` is just a type alias.  
  
From 'package.scala':
```scala
package object proj4 {
  type Transform = (Double, Double) => (Double, Double)
}
```