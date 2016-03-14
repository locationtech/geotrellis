# geotrellis.util

## Introduction ##

The `geotrellis.util` package contains utility functionality that does not properly belong to any single project.
At present, there are only two source files housed in this project:
[`util/src/main/scala/geotrellis/util/Filesystem.scala`](https://github.com/geotrellis/geotrellis/blob/master/util/src/main/scala/geotrellis/util/Filesystem.scala) and
[`util/src/main/scala/geotrellis/util/MethodExtensions.scala`](https://github.com/geotrellis/geotrellis/blob/master/util/src/main/scala/geotrellis/util/MethodExtensions.scala).
The motivation-for and usage-of those two pieces of code is provided below.

### Filesystem.scala ###

The `Filesystem` object contains functions for such tasks as
reading and writing to the local filesystem,
copying and moving files,
and working with path names.

#### Motivation ####

Roughly speaking, the `Filesystem` object contains three types of functions.

The functions `slurp`
-- which reads an entire file from disk and returns its contents as a byte array --
`writeBytes`, `writeText`, and `readText` are all examples one of the three types:
those functions which read and write data.

The functions `move`, `copy`, and `ensureDirectory`
are examples of functions which exist to help with changing the filesystem.

Finally, functions such as `basename`, `split`, and `join`
are used for operating on path names.

Please note that the listing above is non-exhaustive,
the Scaladocs contain a complete listing of functions and more detail about how they are used.

#### Usage ####

Since the functions provided in the object are small and mutually-independent,
using them is fairly uncomplicated.
Please see the source files
[`./spark/src/main/scala/geotrellis/spark/io/file/FileLayerCopier.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/io/file/FileLayerCopier.scala),
[`./spark/src/main/scala/geotrellis/spark/io/file/FileTileReader.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/io/file/FileTileReader.scala), and
[`./spark/src/main/scala/geotrellis/spark/io/file/FileAttributeStore.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/io/file/FileAttributeStore.scala)
for examples of usage.

### MethodExtensions.scala ###

This file contains the `MethodExtensions` trait,
which is the base trait for all implicit classes containing extension methods.

#### Motivation ####

This mechanism is present to allow a more organized code base,
it allows methods to be written in an abstract way so that they do not only apply to one particular chain of types,
it allows functions to be written in an abstract way so that they require extensions rather than types,
and it provides a canonical extension mechanism for users of the library.

#### Usage ####

Examples of how to use the mechanism provided by the `MethodExtensions` trait
are pervasive throughout the Geotrellis codebase.

One example is the addition of `RasterRDD` crop methods in
[`spark/src/main/scala/geotrellis/spark/crop/TileLayerRDDCropMethods.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/crop/TileLayerRDDCropMethods.scala),
made available in [`spark/src/main/scala/geotrellis/spark/crop/Implicits.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/crop/Implicits.scala)
(where the an implicit class with the extension methods is wrapped in a trait),
and [`spark/src/main/scala/geotrellis/spark/package.scala`](https://github.com/geotrellis/geotrellis/blob/master/spark/src/main/scala/geotrellis/spark/package.scala)
(where the trait is mixed into the package object via the line `with crop.Implicits`).

Below we present another example, this one using the fictitious `geotrellis.raster.display` namespace,
to discuss the preferred mechanics for using the `MethodExtensions` functionality.
First, a `trait` or `abstract class` is created which extends `MethodsExtensions`
(compare the example below with `TileLayerRDDCropMethods.scala` linked-to above):
```scala
package geotrellis.raster.display

import geotrellis.util.MethodExtensions


trait DisplayMethods[T <: AnyVal] MethodExtensions[T] {
  def display(): Unit = println(self)
}
```

Next, the implicit class containing the methods is presented as an implicit class within a trait called `Implicit`
(compare the example below with `Implicits.scala` linked-to above):
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
