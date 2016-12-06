# geotrellis.util

## Introduction ##

The `geotrellis.util` package contains utility functionality
that does not properly belong to any single project. At present,
there are only two source files housed in this project:
[`util/src/main/scala/geotrellis/util/Filesystem.scala`](https://github.com/geotrellis/geotrellis/blob/master/util/src/main/scala/geotrellis/util/Filesystem.scala) and
[`util/src/main/scala/geotrellis/util/MethodExtensions.scala`](https://github.com/geotrellis/geotrellis/blob/master/util/src/main/scala/geotrellis/util/MethodExtensions.scala).
The motivation-for and usage-of those two pieces of code is provided below.

### Filesystem.scala ###

The `Filesystem` object contains functions for such tasks as
reading and writing to the local filesystem, copying and moving files,
and working with path names.

#### Motivation ####

Roughly speaking, the `Filesystem` object contains three types of
functions:

1. The functions `slurp` -- which reads an entire file from disk and
returns its contents as a byte array - `writeBytes`, `writeText`,
and `readText` are all examples one of the three types: those
functions which read and write data.

2. The functions `move`, `copy`, and `ensureDirectory` are examples
of functions which exist to help with changing the filesystem.

3. Functions such as `basename`, `split`, and `join` are used for
operating on path names.

> NOTE: The listing above is non-exhaustive, the Scaladocs contain a
> complete listing of functions and more detail about how they are used.

#### Usage ####

Since the functions provided in the object are small and mutually-independent,
using them is relatively simple. Please see the source files
[`FileLayerCopier.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/io/file/FileLayerCopier.scala),
[`FileTileReader.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/io/file/FileTileReader.scala),
and
[`FileAttributeStore.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/io/file/FileAttributeStore.scala)
for examples of usage.

### MethodExtensions.scala ###

This file contains the `MethodExtensions` trait, which is the base
trait for all implicit classes containing extension methods.

#### Motivation ####

This mechanism is present to allow a more organized code base, it
allows methods to be written in an abstract way so that they do not
only apply to one particular chain of types, it allows functions to be
written in an abstract way so that they require extensions rather
than types, and it provides a canonical extension mechanism for users
of the library.

#### Usage ####

Examples of how to use the mechanism provided by the `MethodExtensions`
trait are pervasive throughout the Geotrellis code base.

One example is the addition of `RasterRDD` crop methods in
[`spark/src/main/scala/geotrellis/spark/crop/TileLayerRDDCropMethods.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/crop/TileLayerRDDCropMethods.scala),
made available in [`spark/src/main/scala/geotrellis/spark/crop/Implicits.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/crop/Implicits.scala)
(where the an implicit class with the extension methods is wrapped in a trait),
and [`spark/src/main/scala/geotrellis/spark/package.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/package.scala)
(where the trait is mixed into the package object via the line `with
crop.Implicits`).

Below we present another example, this one using the fictitious
`geotrellis.raster.display` namespace, to discuss the preferred
mechanics for using the `MethodExtensions` functionality. First, a
`trait` or `abstract class` is created which extends `MethodsExtensions`
(compare the example below with `TileLayerRDDCropMethods.scala`
linked-to above):
```scala
package geotrellis.raster.display

import geotrellis.util.MethodExtensions

trait DisplayMethods[T <: AnyVal] MethodExtensions[T] {
  def display(): Unit = println(self)
}
```

Next, the implicit class containing the methods is presented as an
implicit class within a trait called `Implicit` (compare the example
below with `Implicits.scala` linked-to above):
```scala
package geotrellis.raster.display


trait Implicits {
  implicit class withDisplayMethods[T <: AnyVal](val self: T)
      extends DisplayMethods[T]
}
```

Finally, the `Implicit` trait is mixed into the package object
(compare the example below with the `package.scala` file linked-to above):
```scala
package object raster
    extends buffer.Implicits
    with display.Implicits // Add Our Extension Methods
    with Serializable
{
...
}
```

There are a number of caveats that one should keep in mind when using this
mechanism to add extension methods, three of those are given below.

If you are going to be working with RDDs - and potentially serializing
implicit parameters of the implicit class that will be extending the trait -
you should define an object that contains the functionality, and the methods
trait or abstract class should call out to that object. Housing the code
in a function in an object, rather than in the method itself, reduces scope
of the Spark "transforming closure" (thereby reducing the number of objects
which must be serialized). An example is
[here](https://github.com/geotrellis/geotrellis/blob/48162b824df222afbd75c6495fa1e4bc00344fd9/spark/src/main/scala/geotrellis/spark/filter/TileLayerRDDFilterMethods.scala#L35),
where [the implicit values associated with the context bounds](http://docs.scala-lang.org/tutorials/FAQ/context-and-view-bounds.html)
`Boundable` and `Component` would have otherwise been captured by the
transformation closure.

If your method extensions need context bounds or other implicit parameters,
the Scala language forbids you from defining them with a trait.
In this case, use an abstract class and extend the abstract class the
same way that you would have done had you used a trait.
An example of that can be found
[here](https://github.com/geotrellis/geotrellis/blob/48162b824df222afbd75c6495fa1e4bc00344fd9/spark/src/main/scala/geotrellis/spark/stitch/StitchRDDMethods.scala#L38).

If you do not require context bounds or other implicit parameters,
but you are defining the extension methods on a core type of the library,
then check to see if the package object already contains an implicit class
that gives method extensions for that core type. If such a class exists,
then use the existing one and do not define a new implicit class in an
Implicits object. An example can be found
[here](https://github.com/geotrellis/geotrellis/blob/48162b824df222afbd75c6495fa1e4bc00344fd9/raster/src/main/scala/geotrellis/raster/package.scala#L70).
In that case, `PngRenderMethods` did not require context bounds and the
`withTileMethods` class already existed, so the former was simply mixed
into the latter.

