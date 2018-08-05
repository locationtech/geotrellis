===============
 Core Concepts
===============

Geographical Information Systems (GIS), like any specialized field, has
a wealth of jargon and unique concepts. When represented in software,
these concepts can sometimes be skewed or expanded from their original
forms. We give a thorough definition of many of the core concepts here,
while referencing the Geotrellis objects and source files backing them.

This document aims to be informative to new and experienced GIS users
alike. If GIS is brand, brand new to you, `this
document <https://www.gislounge.com/what-is-gis/>`__ is a useful high
level overview.

Basic Terms
===========

-  **Tile:** A grid of numeric *cells* that represent some data on the
   Earth.
-  **Cell:** A single unit of data in some grid, also called a
   *Location* in GIS.
-  **Layer:** or "Tile Layer", this is a grid (or cube) of *Tiles*.
-  **Zoom Layer:** a *Tile Layer* at some zoom level.
-  **Key:** Used to index a *Tile* in a grid (or cube) of them.
-  **Key Index:** Used to transform higher-dimensional *Keys* into one
   dimension.
-  **Metadata:** or "Layer Metadata", stores information critical to
   Tile Layer IO.
-  **Layout Definition:** A description of a Tile grid (its dimensions,
   etc).
-  **Extent:** or "Bounding Box", represents some area on the Earth.
-  **Raster:** A *Tile* with an *Extent*.
-  **Vector:** or "Geometry", these are Point, Line, and Polygon data.
-  **Feature:** A *Geometry* with some associated metadata.
-  **RDD:** "Resilient Distributed Datasets" from `Apache
   Spark <http://spark.apache.org/>`__. Can be thought of as a highly
   distributed Scala ``Seq``.

These definitions are expanded upon in other sections of this document.

.. raw:: html

   <hr>

Tile Layers
===========

Tile layers (of Rasters or otherwise) are represented in GeoTrellis with
the type ``RDD[(K, V)] with Metadata[M]``. This type is used extensively
across the code base, and its contents form the deepest compositional
hierarchy we have:

.. figure:: images/type-composition.png
   :alt:

In this diagram:

-  ``CustomTile``, ``CustomMetadata``, and ``CustomKey`` don't exist,
   they represent types that you could write yourself for your
   application.
-  The ``K`` seen in several places is the same ``K``.
-  The type ``RDD[(K, V)] with Metadata[M]`` is a Scala *Anonymous
   Type*. In this case, it means ``RDD`` from Apache Spark with extra
   methods injected from the ``Metadata`` trait. This type is sometimes
   aliased in GeoTrellis as ``ContextRDD``.
-  ``RDD[(K, V)]`` resembles a Scala ``Map[K, V]``, and in fact has
   further ``Map``-like methods injected by Spark when it takes this
   shape. See Spark's
   `PairRDDFunctions <http://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.rdd.PairRDDFunctions>`__
   Scaladocs for those methods. **Note:** Unlike ``Map``, the ``K``\ s
   here are **not** guaranteed to be unique.

TileLayerRDD
------------

A common specification of ``RDD[(K, V)] with Metadata[M]`` in GeoTrellis
is as follows:

.. code-block:: scala

    type TileLayerRDD[K] = RDD[(K, Tile)] with Metadata[TileLayerMetadata[K]]

This type represents a grid (or cube!) of ``Tile``\ s on the earth,
arranged according to some ``K``. Features of this grid are:

-  Grid location ``(0, 0)`` is the top-leftmost ``Tile``.
-  The ``Tile``\ s exist in *some* CRS. In ``TileLayerMetadata``, this
   is kept track of with an actual ``CRS`` field.
-  In applications, ``K`` is mostly ``SpatialKey`` or ``SpaceTimeKey``.

Tile Layer IO
-------------

Layer IO requires a `Tile Layer Backend <./tile-backends.html>`__. Each
backend has an ``AttributeStore``, a ``LayerReader``, and a
``LayerWriter``.

Example setup (with our ``File`` system backend):

.. code-block:: scala

    import geotrellis.spark._
    import geotrellis.spark.io._
    import geotrellis.spark.io.file._

    val catalogPath: String = ...  /* Some location on your computer */

    val store: AttributeStore = FileAttributeStore(catalogPath)

    val reader = FileLayerReader(store)
    val writer = FileLayerWriter(store)

Writing an entire layer:

.. code-block:: scala

    /* Zoom level 13 */
    val layerId = LayerId("myLayer", 13)

    /* Produced from an ingest, etc. */
    val rdd: TileLayerRDD[SpatialKey] = ...

    /* Order your Tiles according to the Z-Curve Space Filling Curve */
    val index: KeyIndex[SpatialKey] = ZCurveKeyIndexMethod.createIndex(rdd.metadata.bounds)

    /* Returns `Unit` */
    writer.write(layerId, rdd, index)

Reading an entire layer:

.. code-block:: scala

    /* `.read` has many overloads, but this is the simplest */
    val sameLayer: TileLayerRDD[SpatialKey] = reader.read(layerId)

Querying a layer (a "filtered" read):

.. code-block:: scala

    /* Some area on the earth to constrain your query to */
    val extent: Extent = ...

    /* There are more types that can go into `where` */
    val filteredLayer: TileLayerRDD[SpatialKey] =
      reader.query(layerId).where(Intersects(extent)).result

.. raw:: html

   <hr>

Keys and Key Indexes
====================

Keys
----

As mentioned in the `Tile Layers <#tile-layers>`__ section, grids (or
cubes) of ``Tile``\ s on the earth are organized by keys. This key,
often refered to generically as ``K``, is typically a ``SpatialKey`` or
a ``SpaceTimeKey``:

.. code-block:: scala

    case class SpatialKey(col: Int, row: Int)

    case class SpaceTimeKey(col: Int, row: Int, instant: Long)

although there is nothing stopping you from `defining your own key type
<extending-geotrellis.html#custom-keys>`__.

Assuming some tile layer ``Extent`` on the earth, ``SpatialKey(0, 0)``
would index the top-leftmost ``Tile`` in the Tile grid.

When doing Layer IO, certain optimizations can be performed if we know
that ``Tile``\ s stored near each other in a filesystem or database
(like Accumulo or HBase) are also spatially-close in the grid they're
from. To make such a guarantee, we use a ``KeyIndex``.

Key Indexes
-----------

A ``KeyIndex`` is a GeoTrellis ``trait`` that represents `Space Filling
Curves <https://en.wikipedia.org/wiki/Space-filling_curve>`__. They are a
means by which to translate multi-dimensional indices into a
single-dimensional one, while maintaining spatial locality. In GeoTrellis,
we use these chiefly when writing Tile Layers to one of our `Tile Layer
Backends <./tile-backends.html>`__.

Although ``KeyIndex`` is often used in its generic ``trait`` form, we
supply three underlying implementations.

Z-Curve
~~~~~~~

.. figure:: https://upload.wikimedia.org/wikipedia/commons/c/cd/Four-level_Z.svg
   :alt:

The Z-Curve is the simplest ``KeyIndex`` to use (and implement). It can
be used with both ``SpatialKey`` and ``SpaceTimeKey``.

.. code-block:: scala

    val b0: KeyBounds[SpatialKey] = ... /* from `TileLayerRDD.metadata.bounds` */
    val b1: KeyBounds[SpaceTimeKey] = ...

    val i0: KeyIndex[SpatialKey] = ZCurveKeyIndexMethod.createIndex(b0)
    val i1: KeyIndex[SpaceTimeKey] = ZCurveKeyIndexMethod.byDay().createIndex(b1)

    val k: SpatialKey = ...
    val oneD: Long = i0.toIndex(k) /* A SpatialKey's 2D coords mapped to 1D */

Hilbert
~~~~~~~

.. figure:: https://upload.wikimedia.org/wikipedia/commons/a/a5/Hilbert_curve.svg
   :alt:

Another well-known curve, available for both ``SpatialKey`` and
``SpaceTimeKey``.

.. code-block:: scala

    val b: KeyBounds[SpatialKey] = ...

    val index: KeyIndex[SpatialKey] = HilbertKeyIndexMethod.createIndex(b)

Index Resolution Changes Index Order
++++++++++++++++++++++++++++++++++++

Changing the resolution (in bits) of the index causes a rotation and/or
reflection of the points with respect to curve-order. Take, for example
the following code (which is actually derived from the testing
codebase):

.. code-block:: scala

    HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,y2k), SpaceTimeKey(2,2,y2k.plusMillis(1)),2,1)

The last two arguments are the index resolutions. If that were changed
to:

.. code-block:: scala

    HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,y2k), SpaceTimeKey(2,2,y2k.plusMillis(1)),3,1)

The index-order of the points would be different. The reasons behind
this are ultimately technical, though you can imagine how a naive
implementation of an index for, say, a 10x10 matrix (in terms of 100
numbers) would need to be reworked if you were to change the number of
cells (100 would no longer be enough for an 11x11 matrix and the pattern
for indexing you chose may no longer make sense). Obviously, this is
complex and beyond the scope of GeoTrellis' concerns, which is why we
lean on Google's ``uzaygezen`` library.

Beware the 62-bit Limit
+++++++++++++++++++++++

Currently, the spatial and temporal resolution required to index the
points, expressed in bits, must sum to 62 bits or fewer.

For example, the following code appears in
``HilbertSpaceTimeKeyIndex.scala``:

.. code-block:: scala

    @transient
    lazy val chc = {
      val dimensionSpec =
        new MultiDimensionalSpec(
          List(
            xResolution,
            yResolution,
            temporalResolution
          ).map(new java.lang.Integer(_))
        )
    }

where ``xResolution``, ``yResolution`` and ``temporalResolution`` are
numbers of bits required to express possible locations in each of those
dimensions. If those three integers sum to more than 62 bits, an error
will be thrown at runtime.

Row Major
~~~~~~~~~

.. figure:: ./images/row-major.png
   :alt:

Row Major is only available for ``SpatialKey``, but provides the fastest
``toIndex`` lookup of the three curves. It doesn't however, give good
locality guarantees, so should only be used when locality isn't as
important to your application.

.. code-block:: scala

    val b: KeyBounds[SpatialKey] = ...

    val index: KeyIndex[SpatialKey] = RowMajorKeyIndexMethod.createIndex(b)

.. raw:: html

   <hr>

Tiles
=====

``Tile`` is a core GeoTrellis primitive. As mentioned in `Tile
Layers <#tile-layers>`__, a common specification of
``RDD[(K, V)] with Metadata[M]`` is:

.. code-block:: scala

    type TileLayerRDD[K] = RDD[(K, Tile)] with Metadata[TileLayerMetadata[K]]

What is a ``Tile`` exactly? Below is a diagram of our ``Tile`` type
hierarchy. As you can see, any ``Tile`` (via ``CellGrid``) is
effectively a grid of data cells:

.. figure:: ./images/tile-hierarchy.png
   :alt:

The ``Tile`` trait has operations you'd expect for traversing and
transforming this grid, like:

-  ``map: (Int => Int) => Tile``
-  ``foreach: (Int => Unit) => Unit``
-  ``combine: Tile => ((Int, Int) => Int) => Tile``
-  ``color: ColorMap => Tile``

Critically, a ``Tile`` must know how big it is, and what its underlying
`Cell Type <#cell-types>`__ is:

-  ``cols: Int``
-  ``rows: Int``
-  ``cellType: CellType``

Fundamentally, the union of a ``Tile`` and ``Extent`` is how GeoTrellis
defines a ``Raster``:

.. code-block:: scala

    case class Raster[+T <: CellGrid](tile: T, extent: Extent) extends CellGrid

For performance reasons, we have opted for ``Tile`` to hold its
``CellType`` as opposed to making ``Tile`` polymorphic on its underlying
numeric type, for example like ``trait Tile[T]``. The large type
hierarchy above is what results from this decision. For more
information, see `our notes on Tile
performance <../architecture/high-performance-scala.html#the-tile-hierarchy>`__.

.. raw:: html

   <hr>

Cell Types
==========

What is a Cell Type?
--------------------

-  A ``CellType`` is a data type plus a policy for handling cell values
   that may contain no data.
-  By 'data type' we shall mean the underlying numerical representation
   of a ``Tile``'s cells.
-  ``NoData``, for performance reasons, is not represented as a value
   outside the range of the underlying data type (as, e.g., ``None``) -
   if each cell in some tile is a ``Byte``, the ``NoData`` value of that
   tile will exist within the range [``Byte.MinValue`` (-128),
   ``Byte.MaxValue`` (127)].
-  If attempting to convert between ``CellTypes``, see `this
   note <./faq/#how-do-i-convert-a-tiles-celltype>`__ on ``CellType``
   conversions.

+-------------+--------------------+----------------------------------+-------------------------------------+
|             | No NoData          | Constant NoData                  | User Defined NoData                 |
+=============+====================+==================================+=====================================+
| BitCells    | ``BitCellType``    | N/A                              | N/A                                 |
+-------------+--------------------+----------------------------------+-------------------------------------+
| ByteCells   | ``ByteCellType``   | ``ByteConstantNoDataCellType``   | ``ByteUserDefinedNoDataCellType``   |
+-------------+--------------------+----------------------------------+-------------------------------------+
| UbyteCells  | ``UByteCellType``  | ``UByteConstantNoDataCellType``  | ``UByteUserDefinedNoDataCellType``  |
+-------------+--------------------+----------------------------------+-------------------------------------+
| ShortCells  | ``ShortCellType``  | ``ShortConstantNoDataCellType``  | ``ShortUserDefinedNoDataCellType``  |
+-------------+--------------------+----------------------------------+-------------------------------------+
| UShortCells | ``UShortCellType`` | ``UShortConstantNoDataCellType`` | ``UShortUserDefinedNoDataCellType`` |
+-------------+--------------------+----------------------------------+-------------------------------------+
| IntCells    | ``IntCellType``    | ``IntConstantNoDataCellType``    | ``IntUserDefinedNoDataCellType``    |
+-------------+--------------------+----------------------------------+-------------------------------------+
| FloatCells  | ``FloatCellType``  | ``FloatConstantNoDataCellType``  | ``FloatUserDefinedNoDataCellType``  |
+-------------+--------------------+----------------------------------+-------------------------------------+
| DoubleCells | ``DoubleCellType`` | ``DoubleConstantNoDataCellType`` | ``DoubleUserDefinedNoDataCellType`` |
+-------------+--------------------+----------------------------------+-------------------------------------+

The above table lists ``CellType`` ``DataType``\ s in the leftmost
column and ``NoData`` policies along the top row. A couple of points are
worth making here:

1. Bits are incapable of representing on, off, *and* some ``NoData``
   value. As a consequence, there is no such thing as a Bit-backed tile
   which recognizes ``NoData``.
2. While the types in the 'No NoData' and 'Constant NoData' are simply
   singleton objects that are passed around alongside tiles, the greater
   configurability of 'User Defined NoData' ``CellType``\ s means that
   they require a constructor specifying the value which will count as
   ``NoData``.

Let's look to how this information can be used:

.. code-block:: scala

    /** Here's an array we'll use to construct tiles */
    val myData = Array(42, 1, 2, 3)

    /** The GeoTrellis-default integer CellType
     *   Note that it represents `NoData` values with the smallest signed
     *   integer possible with 32 bits (Int.MinValue or -2147483648).
     */
    val defaultCT = IntConstantNoDataCellType
    val normalTile = IntArrayTile(myData, 2, 2, defaultCT)

    /** A custom, 'user defined' NoData CellType for comparison; we will
     *   treat 42 as NoData for this one rather than Int.MinValue
     */
    val customCellType = IntUserDefinedNoDataValue(42)
    val customTile = IntArrayTile(myData, 2, 2, customCellType)

    /** We should expect that the first (default celltype) tile has the value 42 at (0, 0)
     *   This is because 42 is just a regular value (as opposed to NoData)
     *   which means that the first value will be delivered without surprise
     */
    assert(normalTile.get(0, 0) == 42)
    assert(normalTile.getDouble(0, 0) == 42.0)

    /** Here, the result is less obvious. Under the hood, GeoTrellis is
     *   inspecting the value to be returned at (0, 0) to see if it matches our
     *   `NoData` policy and, if it matches (it does, we defined NoData as
     *   42 above), return Int.MinValue (no matter your underlying type, `get`
     *   on a tile will return an `Int` and `getDouble` will return a `Double`).
     *
     *   The use of Int.MinValue and Double.NaN is a result of those being the
     *   GeoTrellis-blessed values for NoData - below, you'll find a chart that
     *   lists all such values in the rightmost column
     */
    assert(customTile.get(0, 0) == Int.MinValue)
    assert(customTile.getDouble(0, 0) == Double.NaN)

A point which is perhaps not intuitive is that ``get`` will *always*
return an ``Int`` and ``getDouble`` will *always* return a ``Double``.
Representing NoData demands, therefore, that we map other celltypes'
``NoData`` values to the native, default ``Int`` and ``Double``
``NoData`` values. ``NoData`` will be represented as ``Int.MinValue`` or
``Double.Nan``.

Why you should care
-------------------

In most programming contexts, it isn't all that useful to think
carefully about the number of bits necessary to represent the data
passed around by a program. A program tasked with keeping track of all
the birthdays in an office or all the accidents on the New Jersey
turnpike simply doesn't benefit from carefully considering whether the
allocation of those extra few bits is *really* worth it. The costs for
any lack of efficiency are more than offset by the savings in
development time and effort. This insight - that computers have become
fast enough for us to be forgiven for many of our programming sins - is,
by now, truism.

An exception to this freedom from thinking too hard about implementation
details is any software that tries, in earnest, to provide the tools for
reading, writing, and working with large arrays of data. Rasters
certainly fit the bill. Even relatively modest rasters can be made up of
millions of underlying cells. Additionally, the semantics of a raster
imply that each of these cells shares an underlying data type. These
points - that rasters are made up of a great many cells and that they
all share a backing data type - jointly suggest that a decision
regarding the underlying data type could have profound consequences.
More on these consequences `below <#cell-type-performance>`__.

Compliance with the GeoTIFF standard is another reason that management
of cell types is important for GeoTrellis. The most common format for
persisting a raster is the
`GeoTIFF <https://trac.osgeo.org/geotiff/>`__. A GeoTIFF is simply an
array of data along with some useful tags (hence the 'tagged' of 'tagged
image file format'). One of these tags specifies the size of each cell
and how those bytes should be interpreted (i.e. whether the data for a
byte includes its sign - positive or negative - or whether it counts up
from 0 - and is therefore said to be 'unsigned').

In addition to keeping track of the memory used by each cell in a
``Tile``, the cell type is where decisions about which values count as
data (and which, if any, are treated as ``NoData``). A value recognized
as ``NoData`` will be ignored while mapping over tiles, carrying out
focal operations on them, interpolating for values in their region, and
just about all of the operations provided by GeoTrellis for working with
``Tile``\ s.

Cell Type Performance
---------------------

There are at least two major reasons for giving some thought to the
types of data you'll be working with in a raster: persistence and
performance.

Persistence is simple enough: smaller datatypes end up taking less space
on disk. If you're going to represent a region with only
``true``/``false`` values on a raster whose values are ``Double``\ s,
63/64 bits will be wasted. Naively, this means somewhere around 63 times
less data than if the most compact form possible had been chosen (the
use of ``BitCells`` would be maximally efficient for representing the
bivalent nature of boolean values). See the chart below for a sense of
the relative sizes of these cell types.

The performance impacts of cell type selection matter in both a local
and a distributed (spark) context. Locally, the memory footprint will
mean that as larger cell types are used, smaller amounts of data can be
held in memory and worked on at a given time and that more CPU cache
misses are to be expected. This latter point - that CPU cache misses
will increase - means that more time spent shuffling data from the
memory to the processor (which is often a performance bottleneck). When
running programs that leverage spark for compute distribution, larger
data types mean more data to serialize and more data send over the (very
slow, relatively speaking) network.

In the chart below, ``DataType``\ s are listed in the leftmost column
and important characteristics for deciding between them can be found to
the right. As you can see, the difference in size can be quite stark
depending on the cell type that a tile is backed by. That extra space is
the price paid for representing a larger range of values. Note that bit
cells lack the sufficient representational resources to have a
``NoData`` value.

+---------------+---------------+-----------------------+-----------------------------+----------------------------------+
|               | Bits / Cell   | 512x512 Raster (mb)   | Range (inclusive)           | GeoTrellis NoData Value          |
+===============+===============+=======================+=============================+==================================+
| BitCells      | 1             | 0.032768              | [0, 1]                      | N/A                              |
+---------------+---------------+-----------------------+-----------------------------+----------------------------------+
| ByteCells     | 8             | 0.262144              | [-128, 128]                 | -128 (``Byte.MinValue``)         |
+---------------+---------------+-----------------------+-----------------------------+----------------------------------+
| UbyteCells    | 8             | 0.262144              | [0, 255]                    | 0                                |
+---------------+---------------+-----------------------+-----------------------------+----------------------------------+
| ShortCells    | 16            | 0.524288              | [-32768, 32767]             | -32768 (``Short.MinValue``)      |
+---------------+---------------+-----------------------+-----------------------------+----------------------------------+
| UShortCells   | 16            | 0.524288              | [0, 65535]                  | 0                                |
+---------------+---------------+-----------------------+-----------------------------+----------------------------------+
| IntCells      | 32            | 1.048576              | [-2147483648, 2147483647]   | -2147483648 (``Int.MinValue``)   |
+---------------+---------------+-----------------------+-----------------------------+----------------------------------+
| FloatCells    | 32            | 1.048576              | [-3.40E38, 3.40E38]         | Float.NaN                        |
+---------------+---------------+-----------------------+-----------------------------+----------------------------------+
| DoubleCells   | 64            | 2.097152              | [-1.79E308, 1.79E308]       | Double.NaN                       |
+---------------+---------------+-----------------------+-----------------------------+----------------------------------+

One final point is worth making in the context of ``CellType``
performance: the ``Constant`` types are able to depend upon macros which
inline comparisons and conversions. This minor difference can certainly
be felt while iterating through millions and millions of cells. If
possible, Constant ``NoData`` values are to be preferred. For
convenience' sake, we've attempted to make the GeoTrellis-blessed
``NoData`` values as unobtrusive as possible a priori.

The limits of expected return types (discussed in the previous section)
is used by macros to squeeze as much speed out of the JVM as possible.
Check out `our macros
docs <../architecture/high-performance-scala/#macros>`__ for more on our
use of macros like ``isData`` and ``isNoData``.

.. raw:: html

   <hr>

Raster Data
===========

    “Yes raster is faster, but raster is vaster and vector just SEEMS
    more corrector.” — `C. Dana
    Tomlin <http://uregina.ca/piwowarj/NotableQuotables.html>`__

Rasters and Tiles
-----------------

The entire purpose of ``geotrellis.raster`` is to provide primitive
datatypes which implement, modify, and utilize rasters. In GeoTrellis, a
raster is just a ``Tile`` with an associated ``Extent``. A tile is just
a two-dimensional collection of evenly spaced data. Tiles are a lot like
certain sequences of sequences (this array of arrays is like a 3x3
tile):

.. code::

    // not real syntax
    val myFirstTile = [[1,1,1],[1,2,2],[1,2,3]]
    /** It probably looks more like your mental model if we stack them up:
      * [[1,1,1],
      *  [1,2,2],
      *  [1,2,3]]
      */

In the ``raster`` module of GeoTrellis, the base type of tile is just
``Tile``. All GeoTrellis compatible tiles will have inherited from that
base class, so if you find yourself wondering what a given type of
tile's powers are, that's a decent place to start your search. Here's an
incomplete list of the types of things on offer:

-  Mapping transformations of arbitrary complexity over the constituent
   cells
-  Carrying out operations (side-effects) for each cell
-  Querying a specific tile value
-  Rescaling, resampling, cropping

As we've already discussed, tiles are made up of squares which contain
values. We'll sometimes refer to these value-boxes as *cells*. And, just
like cells in the body, though they are discrete units, they're most
interesting when looked at from a more holistic perspective - rasters
encode relations between values in a uniform space and it is usually
these relations which most interest us. The code found in the
``mapalgebra`` submodule — discussed later in this document — is all
about exploiting these spatial relations.

Working with Cell Values
------------------------

One of the first questions you'll ask yourself when working with
GeoTrellis is what kinds of representation best models the domain you're
dealing with. What types of value do you need your raster to hold? This
question is the province of GeoTrellis ``CellType``\ s.

Building Your Own Tiles
-----------------------

With a grasp of tiles and ``CellType``\ s, we've got all the conceptual
tools necessary to construct our own tiles. Now, since a tile is a
combination of a ``CellType`` with which its cells are encoded and their
spatial arrangement, we will have to somehow combine ``Tile`` (which
encodes our expectations about how cells sit with respect to one
another) and the datatype of our choosing. Luckily, GeoTrellis has done
this for us. To keep its users sane, the wise maintainers of GeoTrellis
have organized ``geotrellis.raster`` such that fully reified tiles sit
at the bottom of an pretty simple inheritance chain. Let's explore that
inheritance so that you will know where to look when your intuitions
lead you astray:

From ``IntArrayTile.scala``:

.. code-block:: scala

    abstract class IntArrayTile(
      val array: Array[Int],
      cols: Int,
      rows: Int
    ) extends MutableArrayTile { ... }

From ``DoubleArrayTile.scala``:

.. code-block:: scala

    abstract class DoubleArrayTile(
      val array: Array[Double],
      cols: Int,
      rows: Int
    ) extends MutableArrayTile { ... }

Tile Inheritance Structure
--------------------------

Both ``IntArrayTile`` and ``DoubleArrayTile`` are themselves extended by
other child classes, but they are a good place to start. Critically,
they are both ``MutableArrayTile``\ s, which adds some nifty methods for
in-place manipulation of cells (GeoTrellis is about performance, so this
minor affront to the gods of immutability can be forgiven). From
MutableArrayTile.scala:

.. code-block:: scala

    trait MutableArrayTile extends ArrayTile { ... }

One level up is ``ArrayTile``. It's handy because it implements the
behavior which largely allows us to treat our tiles like big, long
arrays of (arrays of) data. They also have the trait ``Serializable``,
which is neat any time you can't completely conduct your business within
the neatly defined space-time of the JVM processes which are running on
a single machine (this is the point of GeoTrellis' Spark integration).
From ArrayTile.scala:

.. code-block:: scala

    trait ArrayTile extends Tile with Serializable { ... }

At the top rung in our abstraction ladder we have ``Tile``. You might be
surprised how much we can say about tile behavior from the base of its
inheritance tree, so the source is worth reading directly. From
Tile.scala:

.. code-block:: scala

    trait Tile extends CellGrid with ... { ... }

Where ``CellGrid`` and its parent ``Grid`` just declare something to be
- you guessed it - a grid of numbers with an explicit ``CellType``.

As it turns out, ``CellType`` is one of those things that we can
*mostly* ignore once we've settled on which one is proper for our
domain. After all, it appears as though there's very little difference
between tiles that prefer int-like things and tiles that prefer
double-like things.

    **CAUTION**: While it is true, in general, that operations are
    ``CellType`` agnostic, both ``get`` and ``getDouble`` are methods
    implemented on ``Tile``. In effect, this means that you'll want to
    be careful when querying values. If you're working with int-like
    ``CellType``\ s, probably use ``get``. If you're working with
    float-like ``CellType``\ s, usually you'll want ``getDouble``.

Raster Examples
---------------

In the repl, you can try this out to construct a simple ``Raster``:

.. code::

    import geotrellis.raster._
    import geotrellis.vector._

    scala> IntArrayTile(Array(1,2,3),1,3)
    res0: geotrellis.raster.IntArrayTile = IntArrayTile([S@338514ad,1,3)

    scala> IntArrayTile(Array(1,2,3),3,1)
    res1: geotrellis.raster.IntArrayTile = IntArrayTile([S@736a81de,3,1)

    scala> IntArrayTile(Array(1,2,3,4,5,6,7,8,9),3,3)
    res2: geotrellis.raster.IntArrayTile = IntArrayTile([I@5466441b,3,3)

    scala> Extent(0, 0, 1, 1)
    res4: geotrellis.vector.Extent = Extent(0.0,0.0,1.0,1.0)

    scala> Raster(res2, res4)
    res5: geotrellis.raster.Raster = Raster(IntArrayTile([I@7b47ab7,1,3),Extent(0.0,0.0,1.0,1.0))

Here's a fun method for exploring your tiles:

.. code-block:: scala

    scala> res0.asciiDraw()
    res3: String =
    "    1
         2
         3
    "

    scala> res2.asciiDraw()
    res4: String =
    "    1     2     3
         4     5     6
         7     8     9
    "

That's probably enough to get started. ``geotrellis.raster`` is a pretty
big place, so you'll benefit from spending a few hours playing with the
tools it provides.

.. raw:: html

   <hr>

Vector Data
===========

    “Raster is faster but vector is correcter.” — Somebody

Features and Geometries
-----------------------

In addition to working with raster data, Geotrellis provides a number of
tools for the creation, representation, and modification of vector data.
The data types central to this functionality
(``geotrellis.vector.Feature`` and ``geotrellis.vector.Geometry``)
correspond - and not by accident - to certain objects found in `the
GeoJson spec <http://geojson.org/geojson-spec.html>`__. ``Feature``\ s
correspond to the objects listed under ``features`` in a geojson
``FeatureCollection``. ``Geometry``\ s, to ``geometries`` in a geojson
``Feature``.

Geometries
----------

The base ``Geometry`` class can be found in ``Geometry.scala``. Concrete
geometries include:

-  ``geotrellis.vector.Point``
-  ``geotrellis.vector.MultiPoint``
-  ``geotrellis.vector.Line``
-  ``geotrellis.vector.MultiLine``
-  ``geotrellis.vector.Polygon``
-  ``geotrellis.vector.MultiPolygon``
-  ``geotrellis.vector.GeometryCollection``

Working with these geometries is a relatively straightforward affair.
Let's take a look:

.. code-block:: scala

    import geotrellis.vector._

    /** First, let's create a Point. Then, we'll use its intersection method.
      * Note: we are also using intersection's alias '&'.
      */
    val myPoint = Point(1.0, 1.1) // Create a point
    // Intersection method
    val selfIntersection = myPoint intersection Point(1.0, 1.1)
    // Intersection alias
    val nonIntersection = myPoint & Point(200, 300)

At this point, the values ``selfIntersection`` and ``nonIntersection``
are ``GeometryResult`` containers. These containers are what many JTS
operations on ``Geometry`` objects will wrap their results in. To
idiomatically destructure these wrappers, we can use the
``as[G <: Geometry]`` function which either returns ``Some(G)`` or
``None``.

.. code-block:: scala

    val pointIntersection = (Point(1.0, 2.0) & Point(1.0, 2.0)).as[Point]
    val pointNonIntersection = (Point(1.0, 2.0) & Point(12.0, 4.0)).as[Point]

    assert(pointIntersection == Some(Point(1.0, 2.0)))  // Either some point
    assert(pointNonIntersection == None)                // Or nothing at all

As convenient as ``as[G <: Geometry]`` is, it offers no guarantees about
the domain over which it ranges. So, while you can expect a neatly
packaged ``Option[G <: Geometry]``, it isn't necessarily the case that
the ``GeometryResult`` object produced by a given set of operations is
possibly convertable to the ``Geometry`` subtype you choose. For
example, a ``PointGeometryIntersectionResult.as[Polygon]`` will *always*
return ``None``.

An alternative approach uses pattern matching and ensures an exhaustive
check of the results. ``geotrellis.vector.Results`` contains a
large `ADT <https://en.wikipedia.org/wiki/Algebraic_data_type>`__ which
encodes the possible outcomes for different types of outcomes. The result
type of a JTS-dependent vector operation can be found somewhere on this tree
to the effect that an exhaustive match can be carried out to determine the
``Geometry`` (excepting cases of ``NoResult``, for which there is no
``Geometry``).

For example, we note that a ``Point``/``Point`` intersection has the
type ``PointOrNoResult``. From this we can deduce that it is either a
``Point`` underneath or else nothing:

.. code::

    scala> import geotrellis.vector._
    scala> p1 & p2 match {
         |   case PointResult(_) => println("A Point!)
         |   case NoResult => println("Sorry, no result.")
         | }
    A Point!

Beyond the methods which come with any ``Geometry`` object there are
implicits in many geotrellis modules which will extend Geometry
capabilities. For instance, after importing ``geotrellis.vector.io._``,
it becomes possible to call the ``toGeoJson`` method on any
``Geometry``:

.. code-block:: scala

    import geotrellis.vector.io._
    assert(Point(1,1).toGeoJson == """{"type":"Point","coordinates":[1.0,1.0]}""")

If you need to move from a geometry to a serialized representation or
vice-versa, take a look at the ``io`` directory's contents. This naming
convention for input and output is common throughout Geotrellis. So if
you're trying to get spatial representations in or out of your program,
spend some time seeing if the problem has already been solved.

Methods which are specific to certain subclasses of ``Geometry`` exist
too. For example, ``geotrellis.vector.MultiLine`` is implicitly extended
by ``geotrellis.vector.op`` such that this becomes possible:

.. code-block:: scala

    import geotrellis.vector.op._
    val myML = MultiLine.EMPTY
    myML.unionGeometries

The following packages extend ``Geometry`` capabilities:

-  `geotrellis.vector.io.json <io/json/>`__
-  `geotrellis.vector.io.WKT <io/WKT/>`__
-  `geotrellis.vector.io.WKB <io/WKB/>`__
-  `geotrellis.vector.op <op/>`__
-  `geotrellis.vector.op.affine <op/affine/>`__
-  `geotrellis.vector.reproject <reproject/>`__

Features
--------

The ``Feature`` class is odd at first glance; it thinly wraps one of the
afforementioned ``Geometry`` objects along with some type of data. Its
purpose will be clear if you can keep in mind the importance of the
geojson format of serialization which is now ubiquitous in the GIS
software space. It can be found in ``Feature.scala``.

Let's examine some source code so that this is all a bit clearer. From
``geotrellis.vector.Feature.scala``:

.. code-block:: scala

    abstract class Feature[D] {
      type G <: Geometry
      val geom: G ; val data: D
    }

    case class PointFeature[D](geom: Point, data: D) extends Feature[D] {type G = Point}

These type signatures tell us a good deal. Let's make this easy on
ourselves and put our findings into a list. - The type ``G`` is `some
instance or
other <http://docs.scala-lang.org/tutorials/tour/upper-type-bounds.html>`__
of ``Geometry`` (which we explored just above).

-  The value, ``geom``, which anything the compiler recognizes as a
   ``Feature`` must make available in its immediate closure must be of
   type ``G``.
-  As with ``geom`` the compiler will not be happy unless a ``Feature``
   provides ``data``.
-  Whereas, with ``geom``, we could say a good deal about the types of
   stuff (only things we call geometries) that would satisfy the
   compiler, we have nothing in particular to say about ``D``.

Our difficulty with ``D`` is shared by the ``Point``-focused feature,
``PointFeature``. ``PointFeature`` uses ``Point`` (which is one of the
concrete instances of ``Geometry`` introduced above) while telling us
nothing at all about ``data``'s type. This is just sugar for passing
around a ``Point`` and some associated metadata.

Let's look at some code which does something with D (code which calls
one of D's methods) so that we know what to expect. Remember: types are
just contracts which the compiler is kind enough to enforce. In
well-written code, types (and type variables) can tell us a great deal
about what was in the head of the author.

There's only one ``package`` which does anything with ``D``, so the
constraints (and our job) should be relatively easy. In
``geotrellis.vector.io.json.FeatureFormats`` there are
``ContextBound``\ s on ``D`` which ensure that they have JsonReader,
JsonWriter, and JsonFormat implicits available (this is a `typeclass
<http://danielwestheide.com/blog/2013/02/06/the-neophytes-guide-to-scala-part-12-type-classes.html>`__,
and it allows for something like type-safe duck-typing).

``D``'s purpose is clear enough: any ``D`` which comes with the tools
necessary for json serialization and deserialization will suffice. In
effect, ``data`` corresponds to the "properties" member of the geojson
spec's ``Feature`` object.

If you can provide the serialization tools (that is, implicit
conversions between some type (your ``D``) and `spray
json <https://github.com/spray/spray-json>`__), the ``Feature`` object
in ``geotrellis.vector`` does the heavy lifting of embedding your (thus
serializable) data into the larger structure which includes a
``Geometry``. There's even support for geojson IDs: the "ID" member of a
geojson Feature is represented by the keys of a ``Map`` from ``String``
to ``Feature[D]``. Data in both the ID and non-ID variants of geojson
Feature formats is easily transformed.

Submodules
----------

These submodules define useful methods for dealing with the entities
that call ``geotrellis.vector`` home:

-  ``geotrellis.vector.io`` defines input/output (serialization) of
   geometries
-  ``geotrellis.vector.op`` defines common operations on geometries
-  ``geotrellis.vector.reproject`` defines methods for translating
   between projections

Catalogs
========

We call the basic output of an ingest a **Layer**, and many GeoTrellis
operations `follow this idea <#tile-layers>`__. Layers may be written in
related groups we call **Pyramids**, which are made up of
interpolations/extrapolations of some base Layer (i.e. different zoom
levels). Finally, collections of Pyramids (or just single Layers) can be
grouped in a **Catalog** in an organized fashion that allows for logical
querying later.

While the term "Catalog" is not as pervasive as "Layer" in the GeoTrellis
API, it deserves mention nonetheless as Catalogs are the result of normal
GeoTrellis usage.

Catalog Organization
--------------------

Our `Landsat Tutorial
<https://github.com/geotrellis/geotrellis-landsat-tutorial>`__ produces a
simple single-pyramid catalog on the filesystem at ``data/catalog/`` which
we can use here as a reference. Running ``tree -L 2`` gives us a view of the
directory layout:

.. code::

   .
   ├── attributes
   │   ├── landsat__.__0__.__metadata.json
   │   ├── landsat__.__10__.__metadata.json
   │   ├── landsat__.__11__.__metadata.json
   │   ├── landsat__.__12__.__metadata.json
   │   ├── landsat__.__13__.__metadata.json
   │   ├── landsat__.__1__.__metadata.json
   │   ├── landsat__.__2__.__metadata.json
   │   ├── landsat__.__3__.__metadata.json
   │   ├── landsat__.__4__.__metadata.json
   │   ├── landsat__.__5__.__metadata.json
   │   ├── landsat__.__6__.__metadata.json
   │   ├── landsat__.__7__.__metadata.json
   │   ├── landsat__.__8__.__metadata.json
   │   └── landsat__.__9__.__metadata.json
   └── landsat
       ├── 0
       ├── 1
       ├── 10
       ├── 11
       ├── 12
       ├── 13
       ├── 2
       ├── 3
       ├── 4
       ├── 5
       ├── 6
       ├── 7
       ├── 8
       └── 9

   16 directories, 14 files

The children of ``landsat/`` are directories, but we used ``-L 2`` to hide
their contents. They actually contain thousands of ``Tile`` files, which are
explained below.

Metadata
--------

The metadata JSON files contain familiar information:

.. code-block:: console

   $ jshon < lansat__.__6__.__metadata.json
     [
       {
         "name": "landsat",
         "zoom": 6
       },
       {
         "header": {
           "format": "file",
           "keyClass": "geotrellis.spark.SpatialKey",
           "valueClass": "geotrellis.raster.MultibandTile",
           "path": "landsat/6"
         },
         "metadata": {
           "extent": {
             "xmin": 15454940.911194608,
             "ymin": 4146935.160646211,
             "xmax": 15762790.223459147,
             "ymax": 4454355.929947533
           },
           "layoutDefinition": { ... }
         },
         ... // more here
         "keyIndex": {
           "type": "zorder",
           "properties": {
             "keyBounds": {
               "minKey": { "col": 56, "row": 24 },
               "maxKey": { "col": 57, "row": 25 }
             }
           }
         },
         ... // more here
       }
     ]

Of note is the ``header`` block, which tells GeoTrellis where to look for
and how to interpret the stored ``Tile``\ s, and the ``keyIndex`` block
which is critical for reading/writing specific ranges of tiles. For more
information, see our `section on Key Indexes <#key-indexes>`__.

As we have multiple storage backends, ``header`` can look different. Here's
an example for a Layer ingested to S3:

.. code-block:: javascript

   ... // more here
   "header": {
      "format": "s3",
      "key": "catalog/nlcd-tms-epsg3857/6",
      "keyClass": "geotrellis.spark.SpatialKey",
      "valueClass": "geotrellis.raster.Tile",
      "bucket": "azavea-datahub"
    },
    ... // more here

Tiles
-----

From above, the numbered directories under ``landsat/`` contain serialized
``Tile`` files.

.. code-block:: console

   $ ls
   attributes/  landsat/
   $ cd landsat/6/
   $ ls
   1984  1985  1986  1987
   $ du -sh *
   12K     1984
   8.0K    1985
   44K     1986
   16K     1987

.. note:: These ``Tile`` files are not images, but can be rendered by
          GeoTrellis into PNGs.

Notice that the four ``Tile`` files here have different sizes. Why might
that be, if ``Tile``\ s are all Rasters of the same dimension? The answer is
that a ``Tile`` file can contain multiple tiles. Specifically, it is a
serialized ``Array[(K, V)]`` of which ``Array[(SpatialKey, Tile)]`` is a
common case. When or why multiple ``Tile``\ s might be grouped into a single
file like this is the result of the `Space Filling Curve <#key-indexes>`__
algorithm applied during ingest.

Separate Stores for Attributes and Tiles
----------------------------------------

The real story here is that layer attributes and the ``Tile``\ s themselves
don't need to be stored via the same `backend <tile-backends.html>`__.
Indeed, when instantiating a Layer IO class like ``S3LayerReader``, we notice
that its ``AttributeStore`` parameter is type-agnostic:

.. code-block:: scala

   class S3LayerReader(val attributeStore: AttributeStore)

So it's entirely possible to store your metadata with one service and your
tiles with another. Due to the ``header`` block in each Layer's metadata,
GeoTrellis will know how to fetch the ``Tile``\ s, no matter how they're
stored. This arrangement could be more performant/convenient for you,
depending on your architecture.

.. raw:: html

   <hr>

Layout Definitions and Layout Schemes
=====================================

**Data structures:** ``LayoutDefinition``, ``TileLayout``, ``CellSize``

A Layout Definition describes the location, dimensions of, and
organization of a tiled area of a map. Conceptually, the tiled area
forms a grid, and the Layout Definitions describes that grid's area and
cell width/height. These definitions can be used to chop a bundle of
imagery into tiles suitable for being served out on a web map.

Within the context of GeoTrellis, the ``LayoutDefinition`` class extends
``GridExtent``, and exposes methods for querying the sizes of the grid
and grid cells. Those values are stored in the ``TileLayout`` (the grid
description) and ``CellSize`` classes respectively.
``LayoutDefinition``\ s are used heavily during the raster reprojection
process. Within the context of Geotrellis, the ``LayoutDefinition``
class extends ``GridExtent``, and exposes methods for querying the sizes
of the grid and grid cells. Those values are stored in the
``TileLayout`` (the grid description) and ``CellSize`` classes
respectively. ``LayoutDefinition``\ s are used heavily during the raster
reprojection process.

**What is a Layout Scheme?**

The language here can be vexing, but a ``LayoutScheme`` can be thought
of as a factory which produces ``LayoutDefinition``\ s. It is the scheme
according to which some layout definition must be defined - a layout
definition definition, if you will. The most commonly used
``LayoutScheme`` is the ``ZoomedLayoutScheme``, which provides the
ability to generate ``LayoutDefinitions`` for the different zoom levels
of a web-based map (e.g. `Leaflet <http://leafletjs.com>`__).

| **How are Layout Definitions used throughout Geotrellis?**
| Suppose that we've got a distributed collection of
  ``ProjectedExtent``\ s and ``Tile``\ s which cover some contiguous
  area but which were derived from GeoTIFFs of varying sizes. We will
  sometimes describe operations like this as 'tiling'. The method which
  tiles a collection of imagery provided a ``LayoutDefinition``, the
  underlying ``CellType`` of the produced tiles, and the
  ``ResampleMethod`` to use for generating data at new resolutions is
  ``tileToLayout``. Let's take a look at its use:

.. code-block:: scala

    val sourceTiles: RDD[(ProjectedExtent, Tile)] = ??? // Tiles from GeoTIFF
    val cellType: CellType = IntCellType
    val layout: LayoutDefinition = ???
    val resamp: ResampleMethod = NearestNeighbor

    val tiled: RDD[(SpatialKey, Tile)] =
      tiles.tileToLayout[SpatialKey](cellType, layout, resamp)

In essence, a ``LayoutDefinition`` is the minimum information required
to describe the tiling of some map's area in Geotrellis. The
``LayoutDefinition`` class extends ``GridExtent``, and exposes methods
for querying the sizes of the grid and grid cells. Those values are
stored in the ``TileLayout`` (the grid description) and ``CellSize``
classes respectively. ``LayoutDefinition``\ s are most often encountered
in raster reprojection processes.

Map Algebra
===========

Map Algebra is a name given by Dr. Dana Tomlin in the 1980's to a way of
manipulating and transforming raster data. There is a lot of literature out
there, not least `the book by the guy who "wrote the book" on map algebra
<http://esripress.esri.com/display/index.cfm?fuseaction=display&websiteID=228&moduleID=0>`__,
so we will only give a brief introduction here. GeoTrellis follows Dana's
vision of map algebra operations, although there are many operations that
fall outside of the realm of Map Algebra that it also supports.

Map Algebra operations fall into 3 general categories:

Local Operations
----------------

.. figure:: images/local-animations-optimized.gif
   :alt: localops

Local operations are ones that only take into account the information of
on cell at a time. In the animation above, we can see that the blue and
the yellow cell are combined, as they are corresponding cells in the two
tiles. It wouldn't matter if the tiles were bigger or smaller - the only
information necessary for that step in the local operation is the cell
values that correspond to each other. A local operation happens for each
cell value, so if the whole bottom tile was blue and the upper tile were
yellow, then the resulting tile of the local operation would be green.

Focal Operations
----------------

.. figure:: images/focal-animations.gif
   :alt: focalops

Focal operations take into account a cell, and a neighborhood around that
cell. A neighborhood can be defined as a square of a specific size, or
include masks so that you can have things like circular or wedge-shaped
neighborhoods. In the above animation, the neighborhood is a 5x5 square
around the focal cell. The focal operation in the animation is a
``focalSum``. The focal value is 0, and all of the other cells in the focal
neighborhood; therefore the cell value of the result tile would be 8 at the
cell corresponding to the focal cell of the input tile. This focal operation
scans through each cell of the raster. You can imagine that along the
border, the focal neighborhood goes outside of the bounds of the tile; in
this case the neighborhood only considers the values that are covered by the
neighborhood. GeoTrellis also supports the idea of an analysis area, which
is the GridBounds that the focal operation carries over, in order to support
composing tiles with border tiles in order to support distributed focal
operation processing.

Zonal Operations
----------------

Zonal operations are ones that operate on two tiles: an input tile, and a
zone tile. The values of the zone tile determine what zone each of the
corresponding cells in the input tile belong to. For example, if you are
doing a ``zonalStatistics`` operation, and the zonal tile has a distribution
of zone 1, zone 2, and zone 3 values, we will get back the statistics such
as mean, median and mode for all cells in the input tile that correspond to
each of those zone values.

Using Map Algebra Operations
----------------------------

Map Algebra operations are defined as implicit methods on ``Tile`` or
``Traversable[Tile]``, which are imported with ``import
geotrellis.raster._``.

.. code-block:: scala

    import geotrellis.raster._

    val tile1: Tile = ???
    val tile2: Tile = ???

    // If tile1 and tile2 are the same dimensions, we can combine
    // them using local operations

    tile1.localAdd(tile2)

    // There are operators for some local operations.
    // This is equivalent to the localAdd call above

    tile1 + tile2

    // There is a local operation called "reclassify" in literature,
    // which transforms each value of the function.
    // We actually have a map method defined on Tile,
    // which serves this purpose.

    tile1.map { z => z + 1 } // Map over integer values.

    tile2.mapDouble { z => z + 1.1 } // Map over double values.

    tile1.dualMap({ z => z + 1 })({ z => z + 1.1 }) // Call either the integer value or double version, depending on cellType.

    // You can also combine values in a generic way with the combine funciton.
    // This is another local operation that is actually defined on Tile directly.

    tile1.combine(tile2) { (z1, z2) => z1 + z2 }

The following packages are where Map Algebra operations are defined in
GeoTrellis:

-  `geotrellis.raster.mapalgebra.local <https://geotrellis.github.io/scaladocs/latest/#geotrellis.raster.mapalgebra.local.package>`__
   defines operations which act on a cell without regard to its spatial
   relations. Need to double every cell on a tile? This is the module
   you'll want to explore.
-  `geotrellis.raster.mapalgebra.focal <https://geotrellis.github.io/scaladocs/latest/#geotrellis.raster.mapalgebra.focal.package>`__
   defines operations which focus on two-dimensional windows (internally
   referred to as neighborhoods) of a raster's values to determine their
   outputs.
-  `geotrellis.raster.mapalgebra.zonal <https://geotrellis.github.io/scaladocs/latest/#geotrellis.raster.mapalgebra.zonal.package>`__
   defines operations which apply over a zones as defined by
   corresponding cell values in the zones raster.

`Conway's Game of Life
<http://en.wikipedia.org/wiki/Conway%27s_Game_of_Life>`__ can be seen as a
focal operation in that each cell's value depends on neighboring cell
values. Though focal operations will tend to look at a local region of this
or that cell, they should not be confused with the operations which live in
``geotrellis.raster.local`` - those operations describe transformations over
tiles which, for any step of the calculation, need only know the input value
of the specific cell for which it is calculating an output (e.g.
incrementing each cell's value by 1).

.. raw:: html

   <hr>

Vector Tiles
============

Invented by `Mapbox <https://www.mapbox.com/>`__, VectorTiles are a
combination of the ideas of finite-sized tiles and vector geometries.
Mapbox maintains the official implementation spec for VectorTile codecs.
The specification is free and open source.

VectorTiles are advantageous over raster tiles in that:

-  They are typically smaller to store
-  They can be easily transformed (rotated, etc.) in real time
-  They allow for continuous (as opposed to step-wise) zoom in Slippy
   Maps.

Raw VectorTile data is stored in the protobuf format. Any codec
implementing `the
spec <https://github.com/mapbox/vector-tile-spec/tree/master/2.1>`__
must decode and encode data according to `this .proto
schema <https://github.com/mapbox/vector-tile-spec/blob/master/2.1/vector_tile.proto>`__.

GeoTrellis provides the ``geotrellis-vectortile`` module, a
high-performance implementation of **Version 2.1** of the VectorTile
spec. It features:

-  Decoding of **Version 2** VectorTiles from Protobuf byte data into
   useful Geotrellis types.
-  Lazy decoding of Geometries. Only parse what you need!
-  Read/write VectorTile layers to/from any of our backends.

As of 2016 November, ingests of raw vector data into VectorTile sets
aren't yet possible.

Small Example
-------------

.. code-block:: scala

    import geotrellis.spark.SpatialKey
    import geotrellis.spark.tiling.LayoutDefinition
    import geotrellis.vector.Extent
    import geotrellis.vectortile.VectorTile
    import geotrellis.vectortile.protobuf._

    val bytes: Array[Byte] = ...  // from some `.mvt` file
    val key: SpatialKey = ...  // preknown
    val layout: LayoutDefinition = ...  // preknown
    val tileExtent: Extent = layout.mapTransform(key)

    /* Decode Protobuf bytes. */
    val tile: VectorTile = ProtobufTile.fromBytes(bytes, tileExtent)

    /* Encode a VectorTile back into bytes. */
    val encodedBytes: Array[Byte] = tile match {
      case t: ProtobufTile => t.toBytes
      case _ => ???  // Handle other backends or throw errors.
    }

See `our VectorTile
Scaladocs <https://geotrellis.github.io/scaladocs/latest/#geotrellis.vectortile.package>`__
for detailed usage information.

Implementation Assumptions
--------------------------

This particular implementation of the VectorTile spec makes the
following assumptions:

-  Geometries are implicitly encoded in ''some'' Coordinate Reference
   system. That is, there is no such thing as a "projectionless"
   VectorTile. When decoding a VectorTile, we must provide a Geotrellis
   [[Extent]] that represents the Tile's area on a map. With this, the
   grid coordinates stored in the VectorTile's Geometry are shifted from
   their original [0,4096] range to actual world coordinates in the
   Extent's CRS.
-  The ``id`` field in VectorTile Features doesn't matter.
-  ``UNKNOWN`` geometries are safe to ignore.
-  If a VectorTile ``geometry`` list marked as ``POINT`` has only one
   pair of coordinates, it will be decoded as a Geotrellis ``Point``. If
   it has more than one pair, it will be decoded as a ``MultiPoint``.
   Likewise for the ``LINESTRING`` and ``POLYGON`` types. A complaint
   has been made about the spec regarding this, and future versions may
   include a difference between single and multi geometries.

.. raw:: html

   <hr>

GeoTiffs
========

GeoTiffs are a type of Tiff image file that contain image data
pertaining to satellite, aerial, and elevation data among other types of
geospatial information. The additional pieces of metadata that are
needed to store and display this information is what sets GeoTiffs apart
from normal Tiffs. For instance, the positions of geographic features on
the screen and how they are projected are two such pieces of data that
can be found within a GeoTiff, but is absent from a normal Tiff file.

GeoTiff File Format
-------------------

Because GeoTiffs are Tiffs with extended features, they both have the
same file structure. There exist three components that can be found in
all Tiff files: the header, the image file directory, and the actual
image data. Within these files, the directories and image data can be
found at any point within the file; regardless of how the images are
presented when the file is opened and viewed. The header is the only
section which has a constant location, and that is at the begining of
the file.

File Header
-----------

As stated earlier, the header is found at the beginning of every Tiff
file, including GeoTiffs. All Tiff files have the exact same header size
of 8 bytes. The first two bytes of the header are used to determine the
``ByteOrder`` of the file, also known as "Endianness". After these two,
comes the next two bytes which are used to determine the file's magic
number. ``.tif``, ``.txt``, ``.shp``, and all other file types have a
unique identifier number that tells the program kind of file it was
given. For Tiff files, the magic number is 42. Due to how the other
components can be situated anywhere within the file, the last 4 bytes of
the header provide the offset value that points to the first file
directory. Without this offset, it would be impossible to read a Tiff
file.

Image File Directory
--------------------

For every image found in a Tiff file there exists a corresponding image
file directory for that picture. Each property listed in the directory
is referred to as a ``Tag``. ``Tag``\ s contain information on, but not
limited to, the image size, compression types, and the type of color
plan. Since we're working with Geotiffs, geo-spatial information is also
documented within the ``Tag``\ s. These directories can vary in size, as
users can create their own tags and each image in the file does not need
to have exact same tags.

Other than image attributes, the file directory holds two offset values
that play a role in reading the file. One points to where the actual
image itself is located, and the other shows where the the next file
directory can be found.

Image Data
----------

A Tiff file can store any number of images within a single file,
including none at all. In the case of GeoTiffs, the images themselves
are almost always stored as bitmap data. It is important to understand
that there are two ways in which the actual image data is formatted
within the file. The two methods are: Striped and Tiled.

Striped
~~~~~~~

Striped storage breaks the image into segments of long, vertical bands
that stretch the entire width of the picture. Contained within them are
columns of bitmapped image data. If your GeoTiff file was created before
the realse of Tiff 6.0, then this is the data storage method in which it
most likely uses.

If an image has strip storage, then its corresponding file directory
contains the tags: ``RowsPerStrip``, ``StripOffsets``, and
``StripByteCount``. All three of these are needed to read that given
segment. The first one is the number of rows that are contained within
the strips. Every strip within an image must have the same number of
rows within it except for the last one in certain instances.
``StripOffsets`` is an array of offsets that shows where each strip
starts within the file. The last tag, ``ByteSegmentCount``, is also an
array of values that contains the size of each strip in terms of Bytes.

Tiled
~~~~~

Tiff 6.0 introduced a new way to arrange and store data within a Tiff,
tiled storage. These rectangular segments have both a height and a width
that must be divisible by 16. There are instances where the tiled grid
does not fit the image exactly. When this occurs, padding is added
around the image so as to meet the requirement of each tile having
dimensions of a factor of 16.

As with stips, tiles have specific tags that are needed in order to
process each segment. These new tags are: ``TileWidth``, ``TileLength``,
``TileOffsets``, and ``TileByteCounts``. ``TileWidth`` is the number of
columns and ``TileLength`` is the number of rows that are found within
the specified tile. As with striped, ``TileOffsets`` and
``TileByteCounts`` are arrays that contain the begining offset and the
byte count of each tile in the image, respectively.

Layout: Columns and Rows
--------------------------

At a high level, there exist two ways to refer to a location within GeoTiffs.
One is to use Map coordinates which are X and Y values. X's are oriented
along the horizontal axis and run from west to east while Y's are on the
vertical axis and run from south to north. Thus the further east you
are, the larger your X value; and the more north you are the larger
your Y value.

The other method is to use the grid coordinate system. This technique of
measurement uses Cols and Rows to describe the relative location of
things. Cols run east to west whereas Rows run north to south. This then
means that Cols increase as you go east to west, and rows increase as
you go north to south.

Each (X, Y) pair corresponds to some real location on the planet. Cols and
rows, on the other hand, are ways of specifying location *within the image*
rather than by reference to any actual location. For more on coordinate
systems supported by GeoTiffs, check out the relevant parts of the
`spec <http://geotiff.maptools.org/spec/geotiff2.5.html#2.5/>`_.

Big Tiffs
---------

In order to qualify as a BigTiff, your file needs to be **at least 4gb
in size or larger**. At this size, the methods used to store
and find data are different. The accommodation that is made is to
change the size of the various offsets and byte counts of each segment.
For a normal Tiff, this size is 32-bits, but BigTiffs have these sizes
at 64-bit. GeoTrellis transparently supports BigTiffs, so
so you shouldn't need to worry about size.

Cloud Optimized GeoTiffs
------------------------

Just as the GeoTiff is a subset of Tiff meant to convey information not
only about image values but also the spatial extent of that imagery,
Cloud Optimized GeoTiffs (COGs for short) are a nascent subset of GeoTiff
meant to increase their expressiveness, ease of use, and portability through
further standardization. We call these GeoTiffs "cloud optimized" because
the features they add allow for remote access to GeoTiff that, with the help
of HTTP GET range requests, access the parts of a tiff you're interested
without consuming large portions of the image which are irrelevant to your
computation.

COGs are thus capable of serving as a self-describing backing for raster
layers. The only cost associated with the use of COGs over GeoTrellis'
Avro-based layers is the extra effort related to metadata retrieval
and munging (metadata for each individual GeoTiff will need to be collected
as opposed to the monolithic metadata of Avro layers, which is read and
handled once for the entire layer).

The `COG specification <http://www.cogeo.org/>`_ (which is not a 100%
complete as of the writing of this documentation) defines required
tags and means of access (a server accepting GET range requests). These
required features are necessary to even support remotely reading
subsets of the overall image data from some remote Tiff.

COG requirements:
- Tiled storage of image data
- Overviews at different levels of resolution
- Infrastructure capable of handling GET Range requests

.. code-block:: scala

    // Constructing a COG from a non-COG tiff
    val nonCog = SinglebandGeoTiff(path = file:///path/to/my/tiff.tif)
    val almostCog = nonCog.withStorageMethod(Tiled)

    // most likely either NearestNeighbor or BilinearInterpolation; depends on context
    val resampleMethod: ResampleMethod = ???
    val fullCog = almostCog.withOverviews(resampleMethod)

> A note on sidecar files
> The spec seems to indicate that overviews be part of the GeoTiff itself to
> count as a COG. In practice, things are messier than that. Content providers
> aren't always going to want to rewrite their tiffs to stuff generated
> overviews into them. The practical upshot of this is that separate overview
> files should be supported (GDAL will actually inspect some canonical relative
> paths within the directory of the Tiff being read).

.. code-block:: scala

    // Constructing a COG with sidecar overviews
    val mainTile = SinglebandGeoTiff(path = file:///path/to/my/file.tif)
    val overview1 = SinglebandGeoTiff(path = file:///path/to/my/file.tif.ovr1)
    val overview2 = SinglebandGeoTiff(path = file:///path/to/my/file.tif.ovr2)
    val tileWithOverview = mainTile.withOverviews(List(overview1, overview2))

Structured vs Unstructured COGs
-------------------------------

Historically, Geotrellis layers have been backed by specially encoded Avro
layers which are were designed to maximize the performance of distributed
reading and querying. With the advent of the COG and the development of
tooling to support this subset of the GeoTiff spec, however, the advantages
of depending upon a bespoke raster format are less obvious than they once were.
Avro support is likely to continue, but support for applications backed by
COGs are a priority for continued GeoTrellis development.

To this end, GeoTrellis is introducing the notion of a 'structured' COG layer.
Structured COG layers are actually a collection of COGs tiled out in a
consistent manner and described through common (GeoTrellis-specific) metadata
which is designed to enhance query performance for larger layers by allowing
GeoTrellis programs to infer information about underlying, individual COG files
without having to read multiple of them.

Structured COG metadata:
- cellType: Underlying Tiff celltype (width of cell representation and NoData strategy)
- zoomRangeInfos: A map from some range of supported zoom levels to a collection of key extents
- layoutScheme: The scheme by which individual COG tiles are cut for this layer
- extent: The overall extent of all underlying COGs
- crs: The projection of all underlying COGs

.. code-block:: scala

  // We'll need to get a layer from somewhere
  val layer: RDD[(K, V)] with Metadata[TileLayerMetadata[K]] = ???
  // The native resolution for this layer (assumes standard TMS zoom levels)
  val baseZoom = 8

  // With that, we should be able to construct a 'structured' COG layer
  val structured: CogLayer[K, V] = CogLayer.fromLayerRDD(layer, baseZoom)

Further Readings
----------------

-  `For more information on the Tiff file
   format <http://www.fileformat.info/format/tiff/egff.htm>`__
-  `For more information on the GeoTiff file
   format <http://www.gdal.org/frmt_gtiff.html>`__
-  `For more information on the Cloud Optimized GeoTiff file
   format <http://www.cogeo.org/>`__

.. raw:: html

   <hr>

Typeclasses
===========

Typeclasses are a common feature of Functional Programming. As stated in
the `FAQ <./faq.html#how-do-i-import-geotrellis-methods>`__, typeclasses
group data types by what they can *do*, as opposed to by what they
*are*. If traditional OO inheritance arranges classes in a tree
hierarchy, typeclasses arrange them in a graph.

Typeclasses are realized in Scala through a combination of ``trait``\ s
and ``implicit`` class wrappings. A typeclass constraint is visible in a
class/method signature like this:

.. code-block:: scala

    class Foo[A: Order](a: A) { ... }

Meaning that ``Foo`` can accept any ``A``, so long as it is "orderable".
In reality, this in syntactic sugar for the following:

.. code-block:: scala

    class Foo[A](a: A)(implicit ev: Order[A]) { ... }

Here's a real-world example from GeoTrellis code:

.. code-block:: scala

    protected def _write[
      K: AvroRecordCodec: JsonFormat: ClassTag,
      V: AvroRecordCodec: ClassTag,
      M: JsonFormat: GetComponent[?, Bounds[K]]
    ](layerId: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = { ... }

A few things to notice:

-  Multiple constraints can be given to a single type variable:
   ``K: Foo: Bar: Baz``
-  ``?`` refers to ``M``, helping the compiler with type inference.
   Unfortunately ``M: GetComponent[M, Bounds[K]]`` is not syntactically
   possible

Below is a description of the most-used typeclasses used in GeoTrellis.
All are written by us, unless otherwise stated.

ClassTag
--------

Built-in from ``scala.reflect``. This allows classes to maintain some
type information at runtime, which in GeoTrellis is important for
serialization. You will never need to use this directly, but may have to
annotate your methods with it (the compiler will let you know).

JsonFormat
----------

From the ``spray`` library. This constraint says that its type can be
converted to and from JSON, like this:

.. code-block:: scala

    def toJsonAndBack[A: JsonFormat](a: A): A = {
      val json: Value = a.toJson

      json.convertTo[A]
    }

AvroRecordCodec
---------------

Any type that can be serialized by `Apache
Avro <https://avro.apache.org/>`__. While references to
``AvroRecordCodec`` appear frequently through GeoTrellis code, you will
never need to use its methods. They are used internally by our Tile
Layer Backends and Spark.

Boundable
---------

Always used on ``K``, ``Boundable`` means your key type has a finite
bound.

.. code-block:: scala

    trait Boundable[K] extends Serializable {
      def minBound(p1: K, p2: K): K

      def maxBound(p1: K, p2: K): K
    ...  // etc
    }

Component
---------

``Component`` is a bare-bones ``Lens``. A ``Lens`` is a pair of
functions that allow one to generically get and set values in a data
structure. They are particularly useful for nested data structures.
``Component`` looks like this:

.. code-block:: scala

    trait Component[T, C] extends GetComponent[T, C] with SetComponent[T, C]

Which reads as "if I have a ``T``, I can read a ``C`` out of it" and "if
I have a ``T``, I can write some ``C`` back into it". The lenses we
provide are as follows:

-  ``SpatialComponent[T]`` - read a ``SpatialKey`` out of a some ``T``
   (usually ``SpatialKey`` or ``SpaceTimeKey``)
-  ``TemporalComponent[T]`` - read a ``TemporalKey`` of some ``T``
   (usually ``SpaceTimeKey``)

Cats
----

There is a wide variety of standard typeclasses employed by the functional
programming community.  Rather than implement them ourselves, we have elected
to depend on the `Cats project <http://typelevel.org/cats/>`__ to provide this
extra functionality.  We intend to provide as much compatibility with Cats as
is reasonable without sacrificing readability for users who are not functional
programming mavens.  Initially, we rely on `Functor`s, `Semigroup`s, and
`Monoid`s, but there is some use of the `IO` monad in limited parts of the
code base.  Please see the documentation for Cats for more information.

More Core Concepts
==================

CRS
---

**Data Structures:** ``CRS``, ``LatLng``, ``WebMercator``,
``ConusAlbers``

In GIS, a *projection* is a mathematical transformation of
Latitude/Longitude coordinates on a sphere onto some other flat plane.
Such a plane is naturally useful for representing a map of the earth in
2D. A projection is defined by a *Coordinate Reference System* (CRS),
which holds some extra information useful for reprojection. CRSs
themselves have static definitions, have agreed-upon string
representations, and are usually made public by standards bodies or
companies. They can be looked up at
`SpatialReference.org <http://spatialreference.org/>`__.

A *reprojection* is the transformation of coorindates in one CRS to
another. To do so, coordinates are first converted to those of a sphere.
Every CRS knows how to convert between its coordinates and a sphere's,
so a transformation ``CRS.A -> CRS.B -> CRS.A`` is actually
``CRS.A -> Sphere -> CRS.B -> Sphere -> CRS.A``. Naturally some floating
point error does accumulate during this process.

Within the context of GeoTrellis, the main projection-related object is
the ``CRS`` trait. It stores related ``CRS`` objects from underlying
libraries, and also provides the means for defining custom reprojection
methods, should the need arise.

Here is an example of using a ``CRS`` to reproject a ``Line``:

.. code-block:: scala

    val wm = Line(...)  // A `LineString` vector object in WebMercator.
    val ll: Line = wm.reproject(WebMercator, LatLng)  // The Line reprojected into LatLng.

Extents
-------

**Data structures:** ``Extent``, ``ProjectedExtent``,
``TemporalProjectedExtent``, ``GridExtent``, ``RasterExtent``

An ``Extent`` is a rectangular section of a 2D projection of the Earth.
It is represented by two coordinate pairs that are its "min" and "max"
corners in some Coorindate Reference System. "min" and "max" here are
CRS specific, as the location of the point ``(0,0)`` varies between
different CRS. An Extent can also be referred to as a *Bounding Box*.

Within the context of GeoTrellis, the points within an ``Extent`` always
implicitely belong to some ``CRS``, while a ``ProjectedExtent`` holds
both the original ``Extent`` and its current ``CRS``.

Here are some useful ``Extent`` operations, among many more:

-  ``Extent.translate: (Double, Double) => Extent``
-  ``Extent.distance: Extent => Double``
-  ``Extent.contains: Extent => Boolean``
-  ``Extent.intersection: Extent => Option[Extent]``
-  ``ProjectedExtent.reproject: CRS => Extent``

``Extent``\ s are most often used to represent the area of an entire
Tile layer, and also the individual ``Tile``\ s themselves (especially
in the case of ``Raster``\ s).
