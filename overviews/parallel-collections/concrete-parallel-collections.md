---
layout: overview-large
title: Concrete Parallel Collection Classes

disqus: true

partof: parallel-collections
num: 2
---

## Parallel Array

A [ParArray](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/parallel/mutable/ParArray.html) 
sequence holds a linear,
contiguous array of elements. This means that the elements can be accessed and
updated efficiently by modifying the underlying array. Traversing the
elements is also very efficient for this reason. Parallel arrays are like
arrays in the sense that their size is constant.

    scala> val pa = scala.collection.parallel.mutable.ParArray.tabulate(1000)(x => 2 * x + 1)
    pa: scala.collection.parallel.mutable.ParArray[Int] = ParArray(1, 3, 5, 7, 9, 11, 13,...
    
    scala> pa reduce (_ + _)
    res0: Int = 1000000
    
    scala> pa map (x => (x - 1) / 2)
    res1: scala.collection.parallel.mutable.ParArray[Int] = ParArray(0, 1, 2, 3, 4, 5, 6, 7,...

Internally, splitting a parallel array 
[splitter]({{ site.baseurl }}/overviews/parallel-collections/architecture.html#core_abstractions) 
amounts to creating two new splitters with their iteration indices updated. 
[Combiners]({{ site.baseurl }}/overviews/parallel-collections/architecture.html#core_abstractions) 
are slightly more involved.Since for most transformer methods (e.g. `flatMap`, `filter`, `takeWhile`,
etc.) we don't know the number of elements (and hence, the array size) in
advance, each combiner is essentially a variant of an array buffer with an
amortized constant time `+=` operation. Different processors add elements to
separate parallel array combiners, which are then combined by chaining their
internal arrays. The underlying array is only allocated and filled in parallel
after the total number of elements becomes known. For this reason, transformer
methods are slightly more expensive than accessor methods. Also, note that the
final array allocation proceeds sequentially on the JVM, so this can prove to
be a sequential bottleneck if the mapping operation itself is very cheap.

By calling the `seq` method, parallel arrays are converted to `ArraySeq`
collections, which are their sequential counterparts. This conversion is
efficient, and the `ArraySeq` is backed by the same underlying array as the
parallel array it was obtained from.


## Parallel Vector

A [ParVector](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/parallel/immutable/ParVector.html) 
is an immutable sequence with a low-constant factor logarithmic access and 
update time.

    scala> val pv = scala.collection.parallel.immutable.ParVector.tabulate(1000)(x => x)
    pv: scala.collection.parallel.immutable.ParVector[Int] = ParVector(0, 1, 2, 3, 4, 5, 6, 7, 8, 9,...
    
    scala> pv filter (_ % 2 == 0)
    res0: scala.collection.parallel.immutable.ParVector[Int] = ParVector(0, 2, 4, 6, 8, 10, 12, 14, 16, 18,...

Immutable vectors are represented by 32-way trees, so 
[splitter]({{ site.baseurl }}/overviews/parallel-collections/architecture.html#core_abstractions)s 
are split by assigning subtrees to each splitter. 
[Combiners]({{ site.baseurl }}/overviews/parallel-collections/architecture.html#core_abstractions) 
currently keep a vector of
elements and are combined by lazily copying the elements. For this reason,
transformer methods are less scalable than those of a parallel array. Once the
vector concatenation operation becomes available in a future Scala release,
combiners will be combined using concatenation and transformer methods will
become much more efficient.

Parallel vector is a parallel counterpart of the sequential 
[Vector](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/Vector.html), 
so conversion between the two takes constant time.


## Parallel Range

A [ParRange](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/parallel/immutable/ParRange.html) 
is an ordered sequence of elements equally spaced apart. A parallel range is 
created in a similar way as the sequential 
[Range](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/Range.html):

    scala> 1 to 3 par
    res0: scala.collection.parallel.immutable.ParRange = ParRange(1, 2, 3)
    
    scala> 15 to 5 by -2 par
    res1: scala.collection.parallel.immutable.ParRange = ParRange(15, 13, 11, 9, 7, 5)

Just as sequential ranges have no builders, parallel ranges have no 
[combiner]({{ site.baseurl }}/overviews/parallel-collections/architecture.html#core_abstractions)s.
Mapping the elements of a parallel range produces a parallel vector.
Sequential ranges and parallel ranges can be converted efficiently one from
another using the `seq` and `par` methods.


## Parallel Hash Tables

Parallel hash tables store their elements in an underlying array and place
them in the position determined by the hash code of the respective element.
Parallel mutable hash sets 
([mutable.ParHashSet](http://www.scala-lang.org/api/{{ site.scala-version}}/scala/collection/parallel/mutable/ParHashSet.html)) 
and parallel mutable hash maps 
([mutable.ParHashMap](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/parallel/mutable/ParHashMap.html)) 
are based on hash tables.

    scala> val phs = scala.collection.parallel.mutable.ParHashSet(1 until 2000: _*)
    phs: scala.collection.parallel.mutable.ParHashSet[Int] = ParHashSet(18, 327, 736, 1045, 773, 1082,...
	
    scala> phs map (x => x * x)
    res0: scala.collection.parallel.mutable.ParHashSet[Int] = ParHashSet(2181529, 2446096, 99225, 2585664,...

Parallel hash table combiners sort elements into buckets according to their
hashcode prefix. They are combined by simply concatenating these buckets
together. Once the final hash table is to be constructed (i.e. combiner
`result` method is called), the underlying array is allocated and the elements
from different buckets are copied in parallel to different contiguous segments
of the hash table array.

Sequential hash maps and hash sets can be converted to their parallel variants
using the `par` method. Parallel hash tables internally require a size map
which tracks the number of elements in different chunks of the hash table.
What this means is that the first time that a sequential hash table is
converted into a parallel hash table, the table is traversed and the size map
is created - for this reason, the first call to `par` takes time linear in the
size of the hash table. Further modifications to the hash table maintain the
state of the size map, so subsequent conversions using `par` and `seq` have
constant complexity. Maintenance of the size map can be turned on and off
using the `useSizeMap` method of the hash table. Importantly, modifications in
the sequential hash table are visible in the parallel hash table, and vice
versa.


## Parallel Hash Tries

Parallel hash tries are a parallel counterpart of the immutable hash tries,
which are used to represent immutable sets and maps efficiently. They are
supported by classes 
[immutable.ParHashSet](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/parallel/immutable/ParHashSet.html) 
and
[immutable.ParHashMap](http://www.scala-lang.org/api/{{ site.scala-version}}/scala/collection/parallel/immutable/ParHashMap.html).

    scala> val phs = scala.collection.parallel.immutable.ParHashSet(1 until 1000: _*)
    phs: scala.collection.parallel.immutable.ParHashSet[Int] = ParSet(645, 892, 69, 809, 629, 365, 138, 760, 101, 479,...
	
    scala> phs map { x => x * x } sum
    res0: Int = 332833500

Similar to parallel hash tables, parallel hash trie 
[combiners]({{ site.baseurl }}/overviews/parallel-collections/architecture.html#core_abstractions) 
pre-sort the
elements into buckets and construct the resulting hash trie in parallel by
assigning different buckets to different processors, which construct the
subtries independently.

Parallel hash tries can be converted back and forth to sequential hash tries
by using the `seq` and `par` method in constant time.


## Parallel Concurrent Tries

A [concurrent.TrieMap](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/concurrent/TrieMap.html) 
is a concurrent thread-safe map, whereas a 
[mutable.ParTrieMap](http://www.scala-lang.org/api/{{ site.scala-version}}/scala/collection/parallel/mutable/ParTrieMap.html) 
is its parallel counterpart. While most concurrent data structures do not guarantee
consistent traversal if the the data structure is modified during traversal,
Ctries guarantee that updates are only visible in the next iteration. This
means that you can mutate the concurrent trie while traversing it, like in the
following example which outputs square roots of number from 1 to 99:

    scala> val numbers = scala.collection.parallel.mutable.ParTrieMap((1 until 100) zip (1 until 100): _*) map { case (k, v) => (k.toDouble, v.toDouble) }
    numbers: scala.collection.parallel.mutable.ParTrieMap[Double,Double] = ParTrieMap(0.0 -> 0.0, 42.0 -> 42.0, 70.0 -> 70.0, 2.0 -> 2.0,...
    
    scala> while (numbers.nonEmpty) {
         |   numbers foreach { case (num, sqrt) =>
		 |     val nsqrt = 0.5 * (sqrt + num / sqrt)
		 |     numbers(num) = nsqrt
		 |     if (math.abs(nsqrt - sqrt) < 0.01) { 
		 |       println(num, nsqrt)
		 |		 numbers.remove(num)
		 |	   }
		 |   }
		 | }
	(1.0,1.0)
    (2.0,1.4142156862745097)
    (7.0,2.64576704419029)
    (4.0,2.0000000929222947)
	...


[Combiners]({{ site.baseurl }}/overviews/parallel-collections/architecture.html#core_abstractions) 
are implemented as `TrieMap`s under the hood-- since this is a
concurrent data structure, only one combiner is constructed for the entire
transformer method invocation and shared by all the processors.

As with all parallel mutable collections, `TrieMap`s and parallel `ParTrieMap`s obtained
by calling `seq` or `par` methods are backed by the same store, so
modifications in one are visible in the other. Conversions happen in constant
time.


## Performance characteristics

Performance characteristics of sequence types:

|               | head | tail | apply | update| prepend | append | insert |
| --------      | ---- | ---- | ----  | ----  | ----    | ----   | ----   |
| `ParArray`    | C    | L    | C     | C     |  L      | L      |  L     |
| `ParVector`   | eC   | eC   | eC    | eC    |  eC     | eC     |  -     |
| `ParRange`    | C    | C    | C     | -     |  -      | -      |  -     |

Performance characteristics of set and map types:

|                          | lookup | add  | remove |
| --------                 | ----   | ---- | ----   |
| **immutable**            |        |      |        |
| `ParHashSet`/`ParHashMap`| eC     | eC   | eC     |
| **mutable**              |        |      |        |
| `ParHashSet`/`ParHashMap`| C      | C    | C      |
| `ParTrieMap`             | eC     | eC   | eC     |


### Key

The entries in the above two tables are explained as follows:

|     |                                           |
| --- | ----                                      |
| **C**   | The operation takes (fast) constant time. |
| **eC**  | The operation takes effectively constant time, but this might depend on some assumptions such as maximum length of a vector or distribution of hash keys.|
| **aC**  | The operation takes amortized constant time. Some invocations of the operation might take longer, but if many operations are performed on average only constant time per operation is taken. |
| **Log** | The operation takes time proportional to the logarithm of the collection size. |
| **L**   | The operation is linear, that is it takes time proportional to the collection size. |
| **-**   | The operation is not supported. |

The first table treats sequence types--both immutable and mutable--with the following operations:

|     |                                                     |
| --- | ----                                                |
| **head**   | Selecting the first element of the sequence. |
| **tail**   | Producing a new sequence that consists of all elements except the first one. |
| **apply**  | Indexing. |
| **update** | Functional update (with `updated`) for immutable sequences, side-effecting update (with `update` for mutable sequences. |
| **prepend**| Adding an element to the front of the sequence. For immutable sequences, this produces a new sequence. For mutable sequences it modified the existing sequence. |
| **append** | Adding an element and the end of the sequence. For immutable sequences, this produces a new sequence. For mutable sequences it modified the existing sequence. |
| **insert** | Inserting an element at an arbitrary position in the sequence. This is only supported directly for mutable sequences. |

The second table treats mutable and immutable sets and maps with the following operations:

|     |                                                     |
| --- | ----                                                |
| **lookup** | Testing whether an element is contained in set, or selecting a value associated with a key. |
| **add**    | Adding a new element to a set or key/value pair to a map. |
| **remove** | Removing an element from a set or a key from a map. |
| **min**    | The smallest element of the set, or the smallest key of a map. |













