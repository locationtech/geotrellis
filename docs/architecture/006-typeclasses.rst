==========================
Typeclasses via Simulacrum
==========================

We've decided to use `Simulacrum <https://github.com/mpilquist/simulacrum>`__
to provide new GeoTrellis *typeclasses* around which we expect significant
code reorganization and clean up.

Typeclasses Defined
-------------------

Typeclasses are powerful programming abstractions that relate data types
which have common behaviour. They describe how a type should behave, as opposed
to what a data type is (re: Object Oriented programming).

Typeclasses expose one or more functions and *must* have associated mathematical "Laws".

.. code-block:: scala

   import simulacrum._

   /**
    * LAW: Associativity
    * (a <> b) <> c == a <> (b <> c)
    */
   @typeclass trait Semigroup[A] {
     @op("<>") def combine(x: A, y: A): A
   }

For any given "instance" implementation of ``Semigroup`` (say ``Int``),
the law is unchecked by the compiler. Making sure an
instance upholds the laws is left up to the instance author, say by
writing appropriate unit tests.

In the case of Scala with ``simulacrum``, macro mechanics are employed to
provide the compile-time boilerplate necessary for method injection to occur.
Say given a type ``Foo`` which has a ``Semigroup`` instance, the following
method would be automatically injected:

.. code-block:: scala

   def combine(y: Foo): Foo

where the original ``x`` is supplied by ``this`` / ``self``.

What a Typeclass is not
^^^^^^^^^^^^^^^^^^^^^^^

.. pull-quote::

   (1) *A typeclass without laws is an obfuscation, not an abstraction.*

If a law cannot be found for the proposed fundamental functions,
then the functions must be changed or the attempt at writing the typeclass
must be abandoned.

.. pull-quote::

   (2) *A typeclass with one instance and used in one place is not an abstraction.*

Writing "one-off" typeclasses needlessly increases API surface area. We should consider if the
same functionality can't be written directly on the type itself.

.. pull-quote::

   Corollary from (1) and (2): *A typeclass is not an excuse to inject methods.*

If it comes to this, we must reconsider our designs.

.. pull-quote::

   (3) *A typeclass is not a description of class fields.*

We should avoid definining ``val`` fields on our typeclasses, as this is closer
to the "what" of OOP as opposed to the "how" of typeclasses. Further, a typeclass
having a ``.combine`` method, say, does not mean we expect the implementing class
to directly contain a ``.combine`` method. ``Foo`` does not extend ``Semigroup``,
it provides an *instance* of ``Semigroup`` in its companion object.

What a Typeclass is
^^^^^^^^^^^^^^^^^^^

.. pull-quote::

   (1) *A typeclass is an abstraction over types who share lawful behaviour.*

So somewhere in our code, we could write the function:

.. code-block:: scala

   def foo[A: Semigroup](a: A, foo: Foo): Bar = { ... }

and use ``.combine`` on ``a`` safely, knowing that it will behave sanely no matter
which ``A`` we actually choose at the call-site. This is different from just knowing
that our ``A`` contains a ``.foobar`` method through the usual OOP inheritance
mechanisms (``A <: FooBar``). Typeclasses bring together types that otherwise share
no supertype/subtype relationship, so laws are what guarantee sane behaviour now
and in the future.

Usage
-----

Custom Types and Typeclasses
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

First we define our typeclass:

.. code-block:: scala

   package geotrellis

   import simulacrum._

   /**
    * LAW: Associativity
    * (a <> b) <> c == a <> (b <> c)
    */
   @typeclass trait Semigroup[A] {
     @op("<>") def combine(x: A, y: A): A
   }

Now we define an instance. This is always done in the companion object of the associated type:

.. code-block:: scala

   package geotrellis.foobar

   case class Pair(a: Int, b: Int)

   object Pair {
     implicit val pairSemi: Semigroup[Pair] = new Semigroup[Pair] {
       def combine(x: Pair, y: Pair): Pair = Pair(x.a + y.a, x.b + y.b)
     }
   }

Then "forward" the method injection mechanisms through a top-level import:

.. code-block:: scala

   package object geotrellis extends Semigroup.ToSemigroupOps

And then the following will work:

.. code-block:: scala

   scala> import geotrellis._
   scala> import geotrellis.foobar.Pair

   scala> Pair(1, 2) <> Pair(3, 4)
   res0: Pair = Pair(4, 6)

Instances for Stdlib Types
^^^^^^^^^^^^^^^^^^^^^^^^^^

Sometimes you want to provide an instance for an existing type whose
companion object you don't have access to, say ``scala.collection.immutable.List``.
Consider how to write an instance of ``Layer`` for ``List``:

.. code-block:: scala

   package geotrellis

   /**
    * LAW: Blah blah something about Layer keys
    */
   @typeclass trait Layer[F[_]] {
     def stitch[K, V: Semigroup](layer: F[(K, V)]): V
     // ... other functions
   }

Where to put the instance? One option is in a neatly labelled object:

.. code-block:: scala

   package geotrellis

   object StdInstances {
     implicit val listLayer: Layer[List] = new Layer[List] {
       def stitch[K, V: Semigroup](layer: List[(K, V)]): V = ???  // super smart implementation
       // ... other functions
     }
   }

and then "forward" as usual:

.. code-block:: scala

   package object geotrellis extends Semigroup.ToSemigroupOps with StdInstances

The other option being to write them directly in the ``package object``:

.. code-block:: scala

   package object geotrellis extends Semigroup.ToSemigroupOps {
     implicit val listLayer: Layer[List] = new Layer[List] {
       def stitch[K, V: Semigroup](layer: List[(K, V)]): V = ???  // super smart implementation
       // ... other functions
     }
   }

Best Practices
--------------

- **Minimalism**: Try to find the minimal set of operations and laws that describe the fundamentals.
  We need not overcomplicate each typeclass - in fact, we can break more complex behaviour
  into child typeclasses, forming a hierarchy (like ``Semigroup`` and ``Monoid``).
- **No Orphans**: Always write typeclass instances in the companion object of
  the associated type. *Not*
  doing so is called writing "Orphan Instances", which is an abyss of import confusion
  and developer pain.
- **Law Clarity**: State the typeclass's laws in its docstrings, and verify each instance with unit tests.
- **Discovery**: In rendered Haskell docs, a type's typeclass instances are very visible:

.. figure:: images/instances1.png

And "reverse lookup" is also possible. For any given typeclass, we can see what
types implement instances for it:

.. figure:: images/instances2.png

Unfortunately in Scala we can only achieve the former. So, when trying to discover
what a type can "do", check its companion object for the typeclass instances it
implements. Dev/user chin-scratchers like "Can I reproject this thing?" should
become easily answerable.

Proposed Typeclasses
--------------------

``Local``
^^^^^^^^^

Types which can have *Local* map algebra operations performed on them.
Mostly a glorified, monomorphic ``Functor``.

.. code-block:: scala

   /**
    * LAW: Identity
    * {{{
    * a.map(identity) == identity(a)
    * }}}
    *
    * LAW: Composibility
    * {{{
    * a.map(f compose g) == a.map(g).map(f)
    * }}}
    *
    * LAW: Right-laziness
    * {{{
    * val a: Tile = ArrayTile.empty(IntCellType, 0, 0)
    * a.zipWith(throw new Exception) == a
    * }}}
    *
    * @groupname minimal Minimal Complete Definition
    * @groupprio minimal 0
    *
    * @groupname local Local Operations
    * @groupprio local 1
    * @groupdesc local Per-"location" operations between one or more `A`.
    */
   @typeclass trait Local[A] {

     /** @group minimal */
     def map(self: A, f: Int => Int): A

     /** @group minimal */
     def zipWith(self: A, other: => A, f: (Int, Int) => Int): A  // Could also be called `parmap`.

     /** @group local */
     def classify(self: A, f: Int => Int): A = map(self, f)

     /** @group local */
     def localSum(self: A, other: => A): A = zipWith(self, other, (_ + _))

     /* All other local ops would be provided for free, like `localSum` */

   }

Here, ``Tile`` remains a monomorphic Sum Type for performance reasons. Both ``Tile``
and ``MultibandTile`` would be given instances of ``Local``, and ``Local`` itself
remains with the kind ``Local :: *``. Were ``Tile`` higher-kinded, ``Local`` too
could be ``Local :: * -> *`` and we could piggy-back off ``Functor``.
Maybe one day.

``Projected``
^^^^^^^^^^^^^

Types which exist in a projection, with the ability to be reprojected.

.. code-block:: scala

   /**
    * LAW: Identity
    *   if Projected[A].crs.get(a) == Foo then a.reproject(Foo) == a
    *   with /no/ floating point error.
    *
    * LAW: Isomorphism
    *   if Projected[A].crs.get(a) == Foo then a.reproject(Bar).reproject(Foo) ~= a
    *   with /negligible/ floating point error.
    *
    * LAW: Transitivity
    *   if Projected[A].crs.get(a) == Foo then a.reproject(Bar).reproject(Baz) ~= a.reproject(Baz)
    *   with /negligible/ floating point error.
    *
    * Minimal Complete Definition: [[crs]], [[reproject]]
    */
   @typeclass trait Projected[A] {

     def crs: Lens[A, CRS]

     def reproject(a: A, target: CRS)

   }

``Layer``
^^^^^^^^^

Any higher-kinded type which could be considered a GeoTrellis "Layer".

.. code-block:: scala

   /**
    * LAW: Keys or something?
    *    Keys are sanely positioned? I don't know.
    *
    * Minimal Complete Definition: ???
    */
   @typeclass trait Layer[F[_]] extends Functor[F[_]] {

     ???  // Some fundamental operation that gives the laws meaning.

     def saveToS3[K: ???, V: Binary](layer: F[(K, V)]: IO[Unit] = { ... }  // Provided.

   }

where ``Binary`` is alluding to some binary codec, say Avro.

*Note:* The ``Layer`` symbol is currently used in ``geotrellis-vectortile``.

Further Work
------------

Removal of custom ``MethodExtension`` mechanics.
