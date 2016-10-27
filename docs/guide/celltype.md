# Raster Cell Types

- A `CellType` is a data type plus a policy for handling cell values that
  may contain no data.
- By 'data type' we shall mean the underlying numerical representation
  of a `Tile`'s cells.
- `NoData`, for performance reasons, is not represented as a value outside
  the range of the underlying data type (as, e.g., `None`) - if each cell in some
  tile is a `Byte`, the `NoData` value of that tile will exist within the range
  [`Byte.MinValue` (-128), `Byte.MaxValue` (127)].

|             |     No NoData    |         Constant NoData        |        User Defined NoData        |
|-------------|:----------------:|:------------------------------:|:---------------------------------:|
| BitCells    | `BitCellType`    | N/A                            | N/A                               |
| ByteCells   | `ByteCellType`   | `ByteConstantNoDataCellType`   | `ByteUserDefinedNoDataCellType`   |
| UbyteCells  | `UByteCellType`  | `UByteConstantNoDataCellType`  | `UByteUserDefinedNoDataCellType`  |
| ShortCells  | `ShortCellType`  | `ShortConstantNoDataCellType`  | `ShortUserDefinedNoDataCellType`  |
| UShortCells | `UShortCellType` | `UShortConstantNoDataCellType` | `UShortUserDefinedNoDataCellType` |
| IntCells    | `IntCellType`    | `IntConstantNoDataCellType`    | `IntUserDefinedNoDataCellType`    |
| FloatCells  | `FloatCellType`  | `FloatConstantNoDataCellType`  | `FloatUserDefinedNoDataCellType`  |
| DoubleCells | `DoubleCellType` | `DoubleConstantNoDataCellType` | `DoubleUserDefinedNoDataCellType` |

The above table lists `CellType` `DataType`s in the leftmost column
and `NoData` policies along the top row. A couple of points are worth
making here:

1. Bits are incapable of representing on, off, *and* some `NoData`
   value. As a consequence, there is no such thing as a Bit-backed tile
   which recognizes `NoData`.
2. While the types in the 'No NoData' and 'Constant NoData' are simply
   singleton objects that are passed around alongside tiles, the greater
   configurability of 'User Defined NoData' `CellType`s means that they
   require a constructor specifying the value which will count as
   `NoData`.

Let's look to how this information can be used:
```scala
/** Here's an array we'll use to construct tiles */
val myData = Array(42, 1, 2, 3)

/** The GeoTrellis-default integer CellType */
val defaultCT = IntConstantNoDataCellType
val normalTile = IntArrayTile(myData, 2, 2, defaultCT)

/** A custom, 'user defined' NoData CellType for comparison; we will
treat 42 as `NoData` for this one */
val customCellType = IntUserDefinedNoDataValue(42)
val customTile = IntArrayTile(myData, 2, 2, customCellType)

/** We should expect that the first tile has the value 42 at (0, 0)
because Int.MinValue is the GeoTrellis-default `NoData` value for
integers */
assert(normalTile.get(0, 0) == 42)
assert(normalTile.getDouble(0, 0) == 42.0)

/** Here, the result is less obvious. Under the hood, GeoTrellis is
inspecting the value to be returned at (0, 0) to see if it matches our
custom `NoData` policy and, if it matches (it does), return Int.MinValue
(no matter your underlying type, `get` on a tile will return an `Int`
and `getDouble` will return a `Double`.

The use of Int.MinValue and Double.NaN is a result of those being the
GeoTrellis-blessed values for NoData - [below](#cell-type-performance),
these values appear in the rightmost column */
assert(customTile.get(0, 0) == Int.MinValue)
assert(customTile.getDouble(0, 0) == Double.NaN)
```

## Why you should care

In most programming contexts, it isn't all that useful to think carefully
about the number of bits necessary to represent the data passed around
by a program. A program tasked with keeping track of all the birthdays
in an office or all the accidents on the New Jersey turnpike simply
doesn't benefit from carefully considering whether the allocation of
those extra few bits is *really* worth it. The costs for any lack of
efficiency are more than offset by the savings in development time and
effort. This insight - that computers have become fast enough for us to
be forgiven for many of our programming sins - is, by now, truism.  

An exception to this freedom from thinking too hard about
implementation details is any software that tries, in earnest, to
provide the tools for reading, writing, and working with large arrays of
data. Rasters certainly fit the bill. Even relatively modest rasters
can be made up of millions of underlying cells. Additionally,
the semantics of a raster imply that each of these cells shares
an underlying data type. These points - that rasters are made up
of a great many cells and that they all share a backing
data type - jointly suggest that a decision regarding the underlying
data type could have profound consequences. More on these consequences
[below](#cell-type-performance).  

Compliance with the GeoTIFF standard is another reason that management
of cell types is important for GeoTrellis. The most common format for
persisting a raster is the [GeoTIFF](https://trac.osgeo.org/geotiff/).
A GeoTIFF  is simply an array of data along with some useful tags
(hence the 'tagged' of 'tagged image file format'). One of these
tags specifies the size of each cell and how those bytes should be
interpreted (i.e. whether the data for a byte includes its
sign - positive or negative - or whether it counts up from 0 - and
is therefore said to be 'unsigned').  

In addition to keeping track of the memory used by each cell in a tile,
the cell type is where decisions about which values count as data (and
which, if any, are treated


## Cell Type Performance

There are at least two major reasons for giving some thought to the
types of data you'll be working with in a raster: persistence and
performance.  

Persistence is simple enough: smaller datatypes end up taking less space
on disk. If you're going to represent a region with only `true`/`false`
values on a raster whose values are `Double`s, 63/64 bits will be wasted.
Naively, this means somewhere around 63 times less data than if the most
compact form possible had been chosen (the use of `BitCells` would
be maximally efficient for representing the bivalent nature of boolean
values). See the chart below for a sense of the relative sizes of these
cell types.  

The performance impacts of cell type selection matter in both a local
and a distributed (spark) context. Locally, the memory footprint will mean
that as larger cell types are used, smaller amounts of data can be held in
memory and worked on at a given time and that more CPU cache misses are to be
expected. This latter point - that CPU cache misses will increase - means that
more time spent shuffling data from the memory to the processor (which
is often a performance bottleneck). When running programs that
leverage spark for compute distribution, larger data types mean more
data to serialize and more data send over the (very slow, relatively
speaking) network.  

In the chart below, `DataType`s are listed in the leftmost column and
important characteristics for deciding between them can be found to the
right. As you can see, the difference in size can be quite stark depending on
the cell type that a tile is backed by. That extra space is the price
paid for representing a larger range of values. Note that bit cells
lack the sufficient representational resources to have a `NoData` value.  

|             | Bits / Cell | 512x512 Raster (mb) |     Range (inclusive)     | GeoTrellis NoData Value |
|-------------|:-----------:|---------------------|:-------------------------:|-------------------------|
| BitCells    | 1           | 0.032768            | [0, 1]                    |                     N/A |
| ByteCells   | 8           | 0.262144            | [-128, 128]               |                    -128 |
| UbyteCells  | 8           | 0.262144            | [0, 255]                  |                       0 |
| ShortCells  | 16          | 0.524288            | [-32768, 32767]           |                  -32768 |
| UShortCells | 16          | 0.524288            | [0, 65535]                |                       0 |
| IntCells    | 32          | 1.048576            | [-2147483648, 2147483647] |             -2147483648 |
| FloatCells  | 32          | 1.048576            | [-3.40E38, 3.40E38]       |               Float.NaN |
| DoubleCells | 64          | 2.097152            | [-1.79E308, 1.79E308]     |              Double.NaN |

One final point is worth making in the context of `CellType`
performance: the `Constant` types are able to depend upon macros which
inline comparisons and conversions. This minor difference can certainly
be felt while iterating through millions and millions of cells. If possible, Constant
`NoData` values are to be preferred. For convenience' sake, we've
attempted to make the GeoTrellis-blessed `NoData` values as unobtrusive
as possible a priori.  

## Cell Type Conversion/Interpretation

There are two distinct flavors of 'conversion' which GeoTrellis supports
for moving between `CellType`s: `convert` and `interpretAs`. This
section will try to limit any confusion about just what differentiates
these two methods and which should be used under what circumstances.  

In previous sections, we've said that the `CellType` is just a piece of
metadata carried around alongside a `Tile` which helps GeoTrellis to
keep track of how that `Tile`'s array should be interacted with. The
distinction between `interpretAs` and `convert` relates to how smart
GeoTrellis should be while swapping out one `CellType` for another.  

Broadly, `convert` assumes that your `Tile`'s `CellType` is accurate and
that you'd like the semantics of your `Tile` to remain invariant under
the conversion in question. For example, imagine that we've got
categorical data whose cardinality is equal to the cardinality of `Byte`
(254 assuming we reserved a spot for `NoData`). Let's fiat, too, that
the `CellType` we're using is `ByteConstantNoData`. What happens if we want
to add a 255th category? Unless we abandon `NoData` (usually not the
right move), it would seem we're out of options so long as we use
`ByteCells`. Instead, we should call `convert` on that tile and tell it
that we'd like to transpose all `Byte` values to `Short` values. All of
the numbers will remain the same with the exception of any
`Byte.MinValue` cells, which will be turned into `Short.MinValue` in
accordance with the new `CellType`'s chosen `NoData` value. This frees
up quite a bit of extra room for categories and allows us to continue
working with our data in nearly the same manner as before conversion.  

`interpretAs` is a method that was written to resolve a different
problem. If your `Tile` is associated with an incorrect `CellType` (as
can often happen when reading GeoTIFFs that lack proper, accurate headers),
`interpretAs` provides a means for attaching the correct metadata to
your `Tile` *without trusting the pre-interpretation metadata*. The
"conversion" carried out through `interpretAs` does *not* try to do
anything intelligent. There can be no guarantee that meaning
is preserved through reinterpretation - in fact, the primary use case
for `interpretAs` is to attach the correct metadata to a `Tile` which is
improperly labelled for whatever reason.  

An interesting consequence is that you can certainly move between
data types (not just policies for handling `NoData`) by way of
`interpretAs` but that, because the original metadata is not accurate,
the default, naive conversion (`_.toInt`, `_.toFloat`, etc.) must be
depended upon.  

```scala
/** getRaw is a method that allows us to see the values regardless of
if, semantically, they are properly treated as non-data. We use it here
simply to expose the mechanics of the transformation 'under the hood' */

val myData = Array(42, 2, 3, 4)
val tileBefore = IntArrayTile(myData, 2, 2, IntUserDefinedNoDataValue(42))

/** While the value in (0, 0) is NoData, it is now 1 instead of 42
(which matches our new CellType's expectations) */
val converted = tileBefore.convert(IntUserDefinedNoData(1))
assert(converted.getRaw.get(0, 0) !== converted.get(0, 0))

/** Here, the first value is still 42. But because the NoData value is
now 1, the first value is no longer treated as NoData */
(which matches our new CellType's expectations) */
val interpreted = tileBefore.interpretAs(IntUserDefinedNoData(1))
assert(interpreted.getRaw.get(0, 0) == interpreted.get(0, 0))

```

TL;DR: If your `CellType` is just wrong, reinterpret the meaning of your
underlying cells with a call to `interpretAs`. If you trust your
`CellType` and wish for its semantics to be preserved through
transformation, use `convert`.
