# Raster CellTypes

In the `raster` package, `CellType` is a type alias for a `DataType`
with a `NoDataHandling` policy. This document discusses the semantics of
these types.

## Cell DataType

Each `CellType` (and consequently, each tile) has a level of precision
which is determined by its `DataType`. Precision can be broken down into
integer and floating point `DataType`s. The number of bits per cell can
be further broken down as follows:  

#### Integer CellTypes
-  1 bit representations (`BitCells`)
-  8 bit representations (`ByteCells` and `UByteCells`)
- 16 bit representations (`ShortCells` and `UShortCells`)
- 32 bit representations (`IntCells`)

#### Floating point CellTypes
- 16 bit representations (`FloatCells`)
- 32 bit representations (`DoubleCells`)

In general, you'll want to choose the smallest representation which is fully capable of
representing your domain so that your rasters require as little storage and memory as possible.
If even one in one billion cells in a tile requires Double precision floating point
representation, every cell in the entire tile must be of type `Double` and every cell will,
consequently, require 64 bits of memory. GeoTrellis is a performance-oriented library, so if
you're running into performance bottlenecks like this within GeoTrellis, it is quite likely that
you'll need to either throw more computational resources at your problem or set your sights a bit
lower in terms of precision (perhaps you can live with 32bit floating point values).


## Handling NoData
`NODATA` cells are provided to represent the difference between, e.g.,
some location's thermostat reading 0 degrees and that location's
thermostat being broken such that no data is available. Three options
are available for handling such `NoData` cells. These options are all
subclasses of `NoDataHandling` and determine the behavior of the tiles
to which their `CellType`'s belong.  
**NOTE**: NoData values are represented *within* the range of values
supported by the `CellType`'s `DataType`. This means that for Int32
cells, `NoData` must be a value somewhere between `Int.MinValue` and
`Int.MaxValue`.

#### Constant NoData Handling
The first, and most common, variety of `NoData` handling is
`ConstantNoData`. `CellType`s which inherit this trait make some
assumptions about the value which will represent `NoData`. Because we
can know, at compile time, which value will be used for `NoData`,
macro-based optimizations are possible. That's why this is the default
celltype within GeoTrellis. The values of `NoData` on these `CellType`s
are as follows:

- `BitCells`: N/A
- `ByteCells`: -128
- `UByteCells`: 0
- `ShortCells`: -32768
- `UShortCells`: 0
- `IntCells`: -2147483648
- `FloatCells`: `Float.NaN`
- `DoubleCells`: `Double.NaN`

#### No NoData Handling
Say that your data completely fills the range of possible values for the
`CellType` you happen to be working with. In that case, you have needn't
support a `NoData` value at all. In that case, you'll want a `CellType`
with `NoNoData`. Practically speaking, this is most useful for
`ByteCells` and `UByteCells` due to their small range of possible
values.

#### User Defined NoData Handling
If a specific value is needed to represent `NoData` (this is a rather
special case!), a `CellType` with `UserDefinedNoData` can be used. There
are performance penalties for this decision, however, as the functions
which deal with `NoData` values cannot be inlined at compile time. This
feature is primarily supported for GeoTiff compatibility.


###### A Note on BitCells
Conceptually there's no reason that th `NoData` arrangement above
wouldn't work for `BitCells`. In practice, however, bits just aren't
expressive enough to support the concept of `NoData`. `0` and `1` exhaust `TypeBit`'s
possible values. This means that for `NODATA` to exist, we'd only
be able to represent the difference between cells for which thermostat
measurements exist and those cells for which our thermostat is broken. Effectively then, the
`NODATA` vs `0` distinction in a `TypeBit` tile collapses and we're left with a tile semantically
indistinct from a tile for which the 'data' is whether or not measurements exist at a given
location.

