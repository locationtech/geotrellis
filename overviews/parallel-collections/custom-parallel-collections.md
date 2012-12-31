---
layout: overview-large
title: Creating Custom Parallel Collections

disqus: true

partof: parallel-collections
num: 6
---

## Parallel collections without combiners

Just as it is possible to define custom sequential collections without
defining their builders, it is possible to define parallel collections without
defining their combiners. The consequence of not having a combiner is that
transformer methods (e.g. `map`, `flatMap`, `collect`, `filter`, ...) will by
default return a standard collection type which is nearest in the hierarchy.
For example, ranges do not have builders, so mapping elements of a range
creates a vector.

In the following example we define a parallel string collection. Since strings
are logically immutable sequences, we have parallel strings inherit
`immutable.ParSeq[Char]`:

    class ParString(val str: String)
    extends immutable.ParSeq[Char] {

Next, we define methods found in every immutable sequence:

      def apply(i: Int) = str.charAt(i)
      
      def length = str.length

We have to also define the sequential counterpart of this parallel collection.
In this case, we return the `WrappedString` class:

      def seq = new collection.immutable.WrappedString(str)

Finally, we have to define a splitter for our parallel string collection. We
name the splitter `ParStringSplitter` and have it inherit a sequence splitter,
that is, `SeqSplitter[Char]`:

      def splitter = new ParStringSplitter(str, 0, str.length)
      
      class ParStringSplitter(private var s: String, private var i: Int, private val ntl: Int)
      extends SeqSplitter[Char] {

        final def hasNext = i < ntl
		
        final def next = {
          val r = s.charAt(i)
          i += 1
          r
        }

Above, `ntl` represents the total length of the string, `i` is the current
position and `s` is the string itself.

Parallel collection iterators or splitters require a few more methods in
addition to `next` and `hasNext` found in sequential collection iterators.
First of all, they have a method called `remaining` which returns the number
of elements this splitter has yet to traverse. Next, they have a method called
`dup` which duplicates the current splitter.

        def remaining = ntl - i
		
        def dup = new ParStringSplitter(s, i, ntl)
		
Finally, methods `split` and `psplit` are used to create splitters which
traverse subsets of the elements of the current splitter. Method `split` has
the contract that it returns a sequence of splitters which traverse disjoint,
non-overlapping subsets of elements that the current splitter traverses, none
of which is empty. If the current splitter has 1 or less elements, then
`split` just returns a sequence of this splitter. Method `psplit` has to
return a sequence of splitters which traverse exactly as many elements as
specified by the `sizes` parameter. If the `sizes` parameter specifies less
elements than the current splitter, then an additional splitter with the rest
of the elements is appended at the end. If the `sizes` parameter requires more
elements than there are remaining in the current splitter, it will append an
empty splitter for each size. Finally, calling either `split` or `psplit`
invalidates the current splitter.

       def split = {
          val rem = remaining
          if (rem >= 2) psplit(rem / 2, rem - rem / 2)
          else Seq(this)
        }
		
        def psplit(sizes: Int*): Seq[ParStringSplitter] = {
          val splitted = new ArrayBuffer[ParStringSplitter]
          for (sz <- sizes) {
            val next = (i + sz) min ntl
            splitted += new ParStringSplitter(s, i, next)
            i = next
          }
          if (remaining > 0) splitted += new ParStringSplitter(s, i, ntl)
          splitted
        }
      }
    }

Above, `split` is implemented in terms of `psplit`, which is often the case
with parallel sequences. Implementing a splitter for parallel maps, sets or
iterables is often easier, since it does not require `psplit`.

Thus, we obtain a parallel string class. The only downside is that calling transformer methods
such as `filter` will not produce a parallel string, but a parallel vector instead, which
may be suboptimal - producing a string again from the vector after filtering may be costly.


## Parallel collections with combiners

Lets say we want to `filter` the characters of the parallel string, to get rid
of commas for example. As noted above, calling `filter` produces a parallel
vector and we want to obtain a parallel string (since some interface in the
API might require a sequential string).

To avoid this, we have to write a combiner for the parallel string collection.
We will also inherit the `ParSeqLike` trait this time to ensure that return
type of `filter` is more specific - a `ParString` instead of a `ParSeq[Char]`.
The `ParSeqLike` has a third type parameter which specifies the type of the
sequential counterpart of the parallel collection (unlike sequential `*Like`
traits which have only two type parameters).

    class ParString(val str: String)
    extends immutable.ParSeq[Char]
       with ParSeqLike[Char, ParString, collection.immutable.WrappedString]

All the methods remain the same as before, but we add an additional protected method `newCombiner` which
is internally used by `filter`.

      protected[this] override def newCombiner: Combiner[Char, ParString] = new ParStringCombiner

Next we define the `ParStringCombiner` class. Combiners are subtypes of
builders and they introduce an additional method called `combine`, which takes
another combiner as an argument and returns a new combiner which contains the
elements of both the current and the argument combiner. The current and the
argument combiner are invalidated after calling `combine`. If the argument is
the same object as the current combiner, then `combine` just returns the
current combiner. This method is expected to be efficient, having logarithmic
running time with respect to the number of elements in the worst case, since
it is called multiple times during a parallel computation.

Our `ParStringCombiner` will internally maintain a sequence of string
builders. It will implement `+=` by adding an element to the last string
builder in the sequence, and `combine` by concatenating the lists of string
builders of the current and the argument combiner. The `result` method, which
is called at the end of the parallel computation, will produce a parallel
string by appending all the string builders together. This way, elements are
copied only once at the end instead of being copied every time `combine` is
called. Ideally, we would like to parallelize this process and copy them in
parallel (this is being done for parallel arrays), but without tapping into
the internal represenation of strings this is the best we can do-- we have to
live with this sequential bottleneck.

    private class ParStringCombiner extends Combiner[Char, ParString] {
      var sz = 0
      val chunks = new ArrayBuffer[StringBuilder] += new StringBuilder
      var lastc = chunks.last
      
      def size: Int = sz
      
      def +=(elem: Char): this.type = {
        lastc += elem
        sz += 1
        this
      }
      
      def clear = {
        chunks.clear
        chunks += new StringBuilder
        lastc = chunks.last
        sz = 0
      }
      
      def result: ParString = {
        val rsb = new StringBuilder
        for (sb <- chunks) rsb.append(sb)
        new ParString(rsb.toString)
      }
      
      def combine[U <: Char, NewTo >: ParString](other: Combiner[U, NewTo]) = if (other eq this) this else {
        val that = other.asInstanceOf[ParStringCombiner]
        sz += that.sz
        chunks ++= that.chunks
        lastc = chunks.last
        this
      }
    }


## How do I implement my combiner in general?

There are no predefined recipes-- it depends on the data-structure at
hand, and usually requires a bit of ingenuity on the implementer's
part. However there are a few approaches usually taken:

1. Concatenation and merge. Some data-structures have efficient
implementations (usually logarithmic) of these operations.
If the collection at hand is backed by such a data-structure,
its combiner can be the collection itself. Finger trees,
ropes and various heaps are particularly suitable for such an approach.

2. Two-phase evaluation. An approach taken in parallel arrays and
parallel hash tables, it assumes the elements can be efficiently
partially sorted into concatenable buckets from which the final
data-structure can be constructed in parallel. In the first phase
different processors populate these buckets independently and
concatenate the buckets together. In the second phase, the data
structure is allocated and different processors populate different
parts of the data structure in parallel using elements from disjoint
buckets.
Care must be taken that different processors never modify the same
part of the data structure, otherwise subtle concurrency errors may occur.
This approach is easily applicable to random access sequences, as we
have shown in the previous section.

3. A concurrent data-structure. While the last two approaches actually
do not require any synchronization primitives in the data-structure
itself, they assume that it can be constructed concurrently in a way
such that two different processors never modify the same memory
location. There exists a large number of concurrent data-structures
that can be modified safely by multiple processors-- concurrent skip lists,
concurrent hash tables, split-ordered lists, concurrent avl trees, to
name a few.
An important consideration in this case is that the concurrent
data-structure has a horizontally scalable insertion method.
For concurrent parallel collections the combiner can be the collection
itself, and a single combiner instance is shared between all the
processors performing a parallel operation.


## Integration with the collections framework

Our `ParString` class is not complete yet. Although we have implemented a
custom combiner which will be used by methods such as `filter`, `partition`,
`takeWhile` or `span`, most transformer methods require an implicit
`CanBuildFrom` evidence (see Scala collections guide for a full explanation).
To make it available and completely integrate `ParString` with the collections
framework, we have to mix an additional trait called `GenericParTemplate` and
define the companion object of `ParString`.

    class ParString(val str: String)
    extends immutable.ParSeq[Char]
       with GenericParTemplate[Char, ParString]
       with ParSeqLike[Char, ParString, collection.immutable.WrappedString] {
	  
	  def companion = ParString

Inside the companion object we provide an implicit evidence for the `CanBuildFrom` parameter.

    object ParString {
      implicit def canBuildFrom: CanCombineFrom[ParString, Char, ParString] =
        new CanCombinerFrom[ParString, Char, ParString] {
          def apply(from: ParString) = newCombiner
          def apply() = newCombiner
        }
	  
      def newBuilder: Combiner[Char, ParString] = newCombiner
      
      def newCombiner: Combiner[Char, ParString] = new ParStringCombiner
      
      def apply(elems: Char*): ParString = {
		val cb = newCombiner
		cb ++= elems
		cb.result
	  }
    }



## Further customizations-- concurrent and other collections

Implementing a concurrent collection (unlike parallel collections, concurrent
collections are ones that can be concurrently modified, like
`collection.concurrent.TrieMap`) is not always straightforward. Combiners in
particular often require a lot of thought. In most _parallel_ collections
described so far, combiners use a two-step evaluation. In the first step the
elements are added to the combiners by different processors and the combiners
are merged together. In the second step, after all the elements are available,
the resulting collection is constructed.

Another approach to combiners is to construct the resulting collection as the
elements. This requires the collection to be thread-safe-- a combiner must
allow _concurrent_ element insertion. In this case one combiner is shared by
all the processors.

To parallelize a concurrent collection, its combiners must override the method
`canBeShared` to return `true`. This will ensure that only one combiner is
created when a parallel operation is invoked. Next, the `+=` method must be
thread-safe. Finally, method `combine` still returns the current combiner if
the current combiner and the argument combiner are the same, and is free to
throw an exception otherwise.

Splitters are divided into smaller splitters to achieve better load balancing.
By default, information returned by the `remaining` method is used to decide
when to stop dividing the splitter. For some collections, calling the
`remaining` method may be costly and some other means should be used to decide
when to divide the splitter. In this case, one should override the
`shouldSplitFurther` method in the splitter.

The default implementation divides the splitter if the number of remaining
elements is greater than the collection size divided by eight times the
parallelism level.

    def shouldSplitFurther[S](coll: ParIterable[S], parallelismLevel: Int) =
	    remaining > thresholdFromSize(coll.size, parallelismLevel)

Equivalently, a splitter can hold a counter on how many times it was split and
implement `shouldSplitFurther` by returning `true` if the split count is
greater than `3 + log(parallelismLevel)`. This avoids having to call
`remaining`.

Furthermore, if calling `remaining` is not a cheap operation for a particular
collection (i.e. it requires evaluating the number of elements in the
collection), then the method `isRemainingCheap` in splitters should be
overridden to return `false`.

Finally, if the `remaining` method in splitters is extremely cumbersome to
implement, you can override the method `isStrictSplitterCollection` in its
collection to return `false`. Such collections will fail to execute some
methods which rely on splitters being strict, i.e. returning a correct value
in the `remaining` method. Importantly, this does not effect methods used in
for-comprehensions.







