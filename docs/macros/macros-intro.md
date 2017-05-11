# geotrellis.macros

> NOTE: Because scala macros require a separate stage of compilation, they've
> been broken out into their own package in GeoTrellis. Otherwise, the
> functionality to be found there fits most neatly into
> `geotrellis.raster`.

## Why macros?

Macros are complex and harder to read than most code. As such, it is
reasonable to demand justification when they are employed and to be
suspicious of their necessity. Here are some reasons you'll find macros
in GeoTrellis:

#### Boxing and Unboxing

The main purpose for all of the macros employed throughout GeoTrellis
(though mostly in `geotrellis.raster`) is to avoid the JVM's so-called
'boxing' of primitive types. Boxing, in other contexts, is often called
'wrapping' and it involves passing around primitive values (which
normally are lightweight and which require no special setup to work
with) inside objects that are far heavier (a JVM double is 8 bytes while
the boxed variant requires 24 bytes!) and which require processing time
to unwrap.  

#### Readability and consistency of performant code

Above, it was pointed out that macros are harder to read. This is true,
but there are some special circumstances in which their use can improve
readability and help to ensure consistency. When writing performant
code, it is often not possible to stay DRY (Don't Repeat Yourself). This
adds significant burdens to future modifications of shared behavior (you
have to change code *all over the library*) and it reduces readability
by exploding the shere amount of text which must be read to make sense
of a given portion of code.  


## Macros

#### NoData checks

Throughout `geotrellis.raster`, there are lots of checks about whether
or not a given value is data or whether its value represents `NoData`.  

```scala
isData(Int.MinValue)   // false
isNoData(Int.MinValue) // true

isData(Double.NaN)     // false
isNoData(Double.NaN)   // true
```

This macro provides inlined code which checks to see if a given value is
the GeoTrellis-internal notion of `NoData`. `Int.MinValue` and
`Double.NaN` are the two `NoData` values Geotrellis `isData` and
`isNoData` check against.  


#### Type conversion

Similar to the `NoData` checks mentioned above, type conversion macros
inline functionality which converts `NoData` values for different
`CellType`s (see [the documentation about
celltypes](../raster/celltype.md) for more on the different `NoData`
values). This is a boon to performance and it reduces the lines of code
fairly significantly.  

Instead of this:

```scala
val someValue: Int = ???
val asFloat =
  if (someValue == Int.MinValue) Float.NaN
  else someValue.toFloat
```

We can write:
```scala
val someValue: Int = ???
val asFloat = i2f(someValue)
```

#### Tile Macros

Unlike the above macros, tile macros don't appreciably improve
readability. They've been introduced merely to overcome shortcomings in
certain boxing-behaviors in the scala compiler and understanding their
behavior isn't necessary to read/understand the GeoTrellis codebase.

