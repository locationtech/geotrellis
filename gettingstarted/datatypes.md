---
layout: gettingstarted
title: Raster Data Types and Data Conversions

tutorial: gettingstarted
num: 3
---

#### Data Types

GeoTrellis currently supports six basic types of raster data:

* boolean: 1-bit true (1) or false (0)
* byte: signed 8-bit integer
* short: signed 16-bit integer
* int: signed 32-bit integer
* float: 32-bit floating point
* double: 64-bit floating point

The size (in bits) represents the amount of data used on the filesystem or in
memory by each cell. Raster sizes can usually be estimated by the following
equation:

    bits = rows * columns * size-per-cell
    bytes = bits / 8
    megabytes = bytes / 1,048,576

Using this formula, a 3000 x 2000 raster of boolean cells would use 750K, but a
1000 x 1000 double raster would require 64M of space (each double value
requires 512 times as much space as a boolean).

#### Calculation Types

No matter what the underlying data type is, all rasters support both integral
and floating-point calculations. All integral calculations are done in terms of
`Int` and all floating-point calculations are done in terms of `Double`.
Calculations resulting in data which cannot be exactly represented in the
underlying type will be truncated or otherwise reduced. If this behavior is not
desired, manual conversions to a wider type can be performed. 

Some calculations will use the raster's underlying type while others will
specify a particular type. For instance, bitwise operations will always occur
on a cell's integer value.

#### Conversions Between Types

With a few exceptions, rasters retain their underlying type across operations,
and must be explicitly converted to new types in some cases. GeoTrellis
optimizes these conversions by performing them lazily. For instance:


    // Raster contains Int values.
    val raster = server.run(rasterOp)

    // Apply conversion to Double.
    val converted = raster.convert(TypeDouble)

    // No data has been not converted yet; 
    // future operations will apply conversion.

    // result contains Double values.
    val result = converted.map(_ * 0.3)


Along with other forms of laziness, this strategy avoids allocating
intermediate raster objects, conserving memory and allowing faster execution.
