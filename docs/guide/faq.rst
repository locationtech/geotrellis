Frequently Asked Questions
**************************

How do I install GeoTrellis?
============================

Sadly, you can't. GeoTrellis is a developer toolkit/library/framework
used to develop applications in Scala against geospatial data large and
small. To use it, you need it listed as a dependency in your project
config, like any other library. See our `Setup
Tutorial <../tutorials/setup.html>`__ on how to do this.

How do I convert a ``Tile``'s ``CellType``?
===========================================

**Question**: Let's say I have a tile with incorrect ``CellType``
information or that, for whatever reason, I need to change it. How can I
convert a ``Tile``'s ``CellType``? Which methods can I use?

**Answer**: There are two distinct flavors of 'conversion' which
GeoTrellis supports for moving between ``CellType``\ s: ``convert`` and
``interpretAs``. In what follows we will try to limit any confusion
about just what differentiates these two methods and describe which
should be used under what circumstances.

`Elsewhere <core-concepts.html#cell-types>`__, we've said that the
``CellType`` is just a piece of metadata carried around alongside a
``Tile`` which helps GeoTrellis to keep track of how that ``Tile``'s
array should be interacted with. The distinction between ``interpretAs``
and ``convert`` relates to how smart GeoTrellis should be while swapping
out one ``CellType`` for another.

Broadly, ``convert`` assumes that your ``Tile``'s ``CellType`` is
accurate and that you'd like the semantics of your ``Tile`` to remain
invariant under the conversion in question. For example, imagine that
we've got categorical data whose cardinality is equal to the cardinality
of ``Byte`` (254 assuming we reserved a spot for ``NoData``). Let's
fiat, too, that the ``CellType`` we're using is ``ByteConstantNoData``.
What happens if we want to add a 255th category? Unless we abandon
``NoData`` (usually not the right move), it would seem we're out of
options so long as we use ``ByteCells``. Instead, we should call
``convert`` on that tile and tell it that we'd like to transpose all
``Byte`` values to ``Short`` values. All of the numbers will remain the
same with the exception of any ``Byte.MinValue`` cells, which will be
turned into ``Short.MinValue`` in accordance with the new ``CellType``'s
chosen ``NoData`` value. This frees up quite a bit of extra room for
categories and allows us to continue working with our data in nearly the
same manner as before conversion.

``interpretAs`` is a method that was written to resolve a different
problem. If your ``Tile`` is associated with an incorrect ``CellType``
(as can often happen when reading GeoTIFFs that lack proper, accurate
headers), ``interpretAs`` provides a means for attaching the correct
metadata to your ``Tile`` *without trusting the pre-interpretation
metadata*. The "conversion" carried out through ``interpretAs`` does
*not* try to do anything intelligent. There can be no guarantee that
meaning is preserved through reinterpretation - in fact, the primary use
case for ``interpretAs`` is to attach the correct metadata to a ``Tile``
which is improperly labelled for whatever reason.

An interesting consequence is that you can certainly move between data
types (not just policies for handling ``NoData``) by way of
``interpretAs`` but that, because the original metadata is not accurate,
the default, naive conversion (``_.toInt``, ``_.toFloat``, etc.) must be
depended upon.

.. code:: scala

    /** getRaw is a method that allows us to see the values regardless of
    if, semantically, they are properly treated as non-data. We use it here
    simply to expose the mechanics of the transformation 'under the hood' */

    val myData = Array(42, 2, 3, 4)
    val tileBefore = IntArrayTile(myData, 2, 2, IntUserDefinedNoDataValue(42))

    /** While the value in (0, 0) is NoData, it is now 1 instead of 42
      *  (which matches our new CellType's expectations)
      */
    val converted = tileBefore.convert(IntUserDefinedNoData(1))
    assert(converted.getRaw.get(0, 0) != converted.get(0, 0))

    /** Here, the first value is still 42. But because the NoData value is
      *  now 1, the first value is no longer treated as NoData
      *  (which matches our new CellType's expectations) */
    val interpreted = tileBefore.interpretAs(IntUserDefinedNoData(1))
    assert(interpreted.getRaw.get(0, 0) == interpreted.get(0, 0))

TL;DR: If your ``CellType`` is just wrong, reinterpret the meaning of
your underlying cells with a call to ``interpretAs``. If you trust your
``CellType`` and wish for its semantics to be preserved through
transformation, use ``convert``.

How do I import GeoTrellis methods?
===================================

**Question**: In some of the GeoTrellis sample code and certainly in
example projects, it looks like some GeoTrellis types have more methods
than they really do. If I create an ``IntArrayTile``, it doesn't have
most of the methods that it should - I can't reproject, resample, or
carry out map algebra operations - why is that and how can I fix it?

**Answer**: Scala is a weird language. It is both object oriented
(there's an inheritance tree which binds together the various types of
``Tile``) and functional (harder to define, exactly, but there's plenty
of sugar for dealing with functions). The phenomenon of apparently
missing methods is an upshot of the fact that many of the behaviors
bestowed upon GeoTrellis types come from the more functional structure
of typeclasses rather than the stricter, more brittle, and more familiar
standard inheritance structure.

Roughly, if OO structures of inheritance define what can be done in
virtue of *what a thing is*, typeclasses allow us to define an object's
behavior in virtue of *what it can do*. Within Scala's type system, this
differing expectation can be found between a function which takes a
``T`` where ``T <: Duck`` (the ``T`` that is expected must be a duck or
one of its subtypes) and a function which takes ``T`` where
``T: Quacks`` (the ``T`` that is expected must be able to quack,
regardless of what it is).

If this sounds a lot like duck-typing, that's because it is. But,
whereas method extension through duck-typing in other languages is a
somewhat risky affair (runtime errors abound), Scala's type system
allows us to be every bit as certain of the behavior in our typeclasses
as we would be were the methods defined within the body of some class,
itself.

Unlike the rather straightforward means for defining typeclasses which
exist in some languages (e.g. Haskell), Scala's typeclasses depend upon
implicitly applying pieces of code which happen to be in scope. The
details can get confusing and are unnecessary for most work with
GeoTrellis. If you're interested in understanding the problem at a
deeper level, check out `this excellent
article <http://danielwestheide.com/blog/2013/02/06/the-neophytes-guide-to-scala-part-12-type-classes.html>`__.

Because the entire typeclass infrastructure depends upon implicits, all
you need to worry about is importing the proper set of classes which
define the behavior in question. Let's look to a concrete example. Note
the difference in import statements:

This does not compile.

.. code:: scala

    import geotrellis.vector._

    val feature = Feature[Point, Int](Point(1, 2), 42)
    feature.toGeoJson  // not allowed, method extension not in scope

This does.

.. code:: scala

    import geotrellis.vector._
    import geotrellis.vector.io._

    val feature = Feature[Point, Int](Point(1, 2), 42)
    feature.toGeoJson  // returns geojson, as expected

TL;DR: Make sure you're importing the appropriate implicits. They define
methods that extend GeoTrellis types.

How do I resolve dependency compatibility issues (Guava, etc.)?
===============================================================

Full possible exception message:

.. code::

    Caused by: java.lang.IllegalStateException: Detected Guava issue #1635
    which indicates that a version of Guava less than 16.01 is in use.  This
    introduces codec resolution issues and potentially other incompatibility
    issues in the driver. Please upgrade to Guava 16.01 or later.

GeoTrellis depends on a huge number of complex dependencies that may cause
dependency hell. One of such dependency is the Guava library. ``GeoTrellis
ETL`` and ``GeoTrellis Cassandra`` depend on ``Guava 16.01``, but Hadoop
depends on ``Guava 11.0.2`` which causes runtime issues due to library
incompatibility. When two different versions of the same library are both
available in the Spark classpath and in a fat assembly jar, Spark will use
library version from its classpath.

There are two possible solutions:

1. To ``shade`` the conflicting library (example below shades Guava in all
GeoTrellis related deps, this idea can be extrapolated on all conflicting
libraries):

.. code:: scala

    assemblyShadeRules in assembly := {
      val shadePackage = "com.azavea.shaded.demo"
      Seq(
        ShadeRule.rename("com.google.common.**" -> s"$shadePackage.google.common.@1")
          .inLibrary(
            "com.azavea.geotrellis" %% "geotrellis-cassandra" % gtVersion,
            "com.github.fge" % "json-schema-validator" % "2.2.6"
          ).inAll
      )
    }

2. To use `spark.driver.userClassPathFirst
<http://spark.apache.org/docs/latest/configuration.html#runtime-environment>`__.
It's an experimental Spark property to force Spark using all deps from the
fat assembly jar.
