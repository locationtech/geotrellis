---
layout: overview-large
title: Performance Characteristics

disqus: true

partof: collections
num: 12
languages: [ja]
---

The previous explanations have made it clear that different collection types have different performance characteristics. That's often the primary reason for picking one collection type over another. You can see the performance characteristics of some common operations on collections summarized in the following two tables.

Performance characteristics of sequence types:

|               | head | tail | apply | update| prepend | append | insert |
| --------      | ---- | ---- | ----  | ----  | ----    | ----   | ----   |
| **immutable** |      |      |       |       |         |        |        |
| `List`        | C    | C    | L     | L     |  C      | L      |  -     |
| `Stream`      | C    | C    | L     | L     |  C      | L      |  -     |
| `Vector`      | eC   | eC   | eC    | eC    |  eC     | eC     |  -     |
| `Stack`       | C    | C    | L     | L     |  C      | C      |  L     |
| `Queue`       | aC   | aC   | L     | L     |  L      | C      |  -     |
| `Range`       | C    | C    | C     | -     |  -      | -      |  -     |
| `String`      | C    | L    | C     | L     |  L      | L      |  -     |
| **mutable**   |      |      |       |       |         |        |        |
| `ArrayBuffer` | C    | L    | C     | C     |  L      | aC     |  L     |
| `ListBuffer`  | C    | L    | L     | L     |  C      | C      |  L     |
|`StringBuilder`| C    | L    | C     | C     |  L      | aC     |  L     |
| `MutableList` | C    | L    | L     | L     |  C      | C      |  L     |
| `Queue`       | C    | L    | L     | L     |  C      | C      |  L     |
| `ArraySeq`    | C    | L    | C     | C     |  -      | -      |  -     |
| `Stack`       | C    | L    | L     | L     |  C      | L      |  L     |
| `ArrayStack`  | C    | L    | C     | C     |  aC     | L      |  L     |
| `Array`       | C    | L    | C     | C     |  -      | -      |  -     |

Performance characteristics of set and map types:

|                    | lookup | add | remove | min           |
| --------           | ----   | ---- | ----  | ----          |
| **immutable**      |        |      |       |               |
| `HashSet`/`HashMap`| eC     | eC   | eC    | L             |
| `TreeSet`/`TreeMap`| Log    | Log  | Log   | Log           |
| `BitSet`           | C      | L    | L     | eC<sup>1</sup>|
| `ListMap`          | L      | L    | L     | L             |
| **mutable**        |        |      |       |               |
| `HashSet`/`HashMap`| eC     | eC   | eC    | L             |
| `WeakHashMap`      | eC     | eC   | eC    | L             |
| `BitSet`           | C      | aC   | C     | eC<sup>1</sup>|
| `TreeSet`          | Log    | Log  | Log   | Log           |

Footnote: <sup>1</sup> Assuming bits are densely packed.

The entries in these two tables are explained as follows:

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

