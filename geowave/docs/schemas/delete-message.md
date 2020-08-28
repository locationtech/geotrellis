## The Root Schema Type

`object` ([The Root Schema](delete-message.md))

# The Root Schema Properties

| Property                | Type     | Required | Nullable       | Defined by                                                                                                                     |
| :---------------------- | -------- | -------- | -------------- | :----------------------------------------------------------------------------------------------------------------------------- |
| [typeName](#typeName)   | `string` | Required | cannot be null | [The Root Schema](delete-message-properties-the-typename-schema.md "\#/properties/typeName#/properties/typeName")              |
| [indexName](#indexName) | `string` | Required | cannot be null | [The Root Schema](delete-message-properties-the-indexname-schema.md "\#/properties/indexName#/properties/indexName")           |
| [geometry](#geometry)   | `object` | Optional | can be null    | [The Root Schema](delete-message-properties-the-geometry-schema.md "\#/properties/geometry#/properties/geometry")              |
| [time](#time)           | `object` | Optional | can be null    | [The Root Schema](delete-message-properties-the-time-schema.md "\#/properties/time#/properties/time")                          |
| [elevation](#elevation) | `object` | Optional | can be null    | [The Root Schema](delete-message-properties-the-elevation-schema.md "\#/properties/elevation#/properties/elevation")                       |
| [compareOp](#compareOp) | `string` | Optional | can be null    | [The Root Schema](delete-message-properties-the-compareop-schema.md "\#/properties/compareOp#/properties/compareOp")           |
| [namespace](#namespace) | `string` | Optional | can be null    | [The Root Schema](delete-message-properties-storage-ie-cassandra-namespace.md "\#/properties/namespace#/properties/namespace") |

## typeName

A name that is used to identify DataType & IndexType it should be the same as it was configured in the IndexMessage


`typeName`

-   is required
-   Type: `string` ([The typeName Schema](delete-message-properties-the-typename-schema.md))
-   cannot be null
-   defined in: [The Root Schema](delete-message-properties-the-typename-schema.md "\#/properties/typeName#/properties/typeName")

### typeName Type

`string` ([The typeName Schema](delete-message-properties-the-typename-schema.md))

### typeName Constraints

**pattern**: the string must match the following regular expression:

```regexp
^(.*)$
```

[try pattern](https://regexr.com/?expression=%5E(.*)%24 "try regular expression with regexr.com")

### typeName Examples

```json
"GeoTiffType"
```

## indexName

An index name. Index name would be inferred from indexOptions and indexType by default during the IndexMessage process.


`indexName`

-   is required
-   Type: `string` ([The indexName Schema](delete-message-properties-the-indexname-schema.md))
-   cannot be null
-   defined in: [The Root Schema](delete-message-properties-the-indexname-schema.md "\#/properties/indexName#/properties/indexName")

### indexName Type

`string` ([The indexName Schema](delete-message-properties-the-indexname-schema.md))

## geometry

GeoJSON of a Geometry (Point, Polygon, MultiPolygon, etc) type.


`geometry`

-   is optional
-   Type: `object` ([The Geometry Schema](delete-message-properties-the-geometry-schema.md))
-   can be null
-   defined in: [The Root Schema](delete-message-properties-the-geometry-schema.md "\#/properties/geometry#/properties/geometry")

### geometry Type

`object` ([The Geometry Schema](delete-message-properties-the-geometry-schema.md))

## time

The time range definition in milliseconds with ISO strings.


`time`

-   is optional
-   Type: `object` ([The Time Schema](delete-message-properties-the-time-schema.md))
-   can be null
-   defined in: [The Root Schema](delete-message-properties-the-time-schema.md "\#/properties/time#/properties/time")

### time Type

`object` ([The Time Schema](delete-message-properties-the-time-schema.md))

## elevation

The elevation range definition in ingested units.


`elevation`

-   is optional
-   Type: `object` ([The elevation Schema](delete-message-properties-the-elevation-schema.md))
-   can be null
-   defined in: [The Root Schema](delete-message-properties-the-elevation-schema.md "\#/properties/elevation#/properties/elevation")

### elevation Type

`object` ([The elevation Schema](delete-message-properties-the-elevation-schema.md))

## compareOp

Type of query: intersection, inclusion, etc.


`compareOp`

-   is optional
-   Type: `string` ([The Compareop Schema](delete-message-properties-the-compareop-schema.md))
-   can be null
-   defined in: [The Root Schema](delete-message-properties-the-compareop-schema.md "\#/properties/compareOp#/properties/compareOp")

### compareOp Type

`string` ([The Compareop Schema](delete-message-properties-the-compareop-schema.md))

### compareOp Constraints

**pattern**: the string must match the following regular expression:

```regexp
^(.*)$
```

[try pattern](https://regexr.com/?expression=%5E(.*)%24 "try regular expression with regexr.com")

### compareOp Default Value

The default value is:

```json
"INTERSECTS"
```

### compareOp Examples

```json
"CONTAINS"
```

```json
"OVERLAPS"
```

```json
"INTERSECTS"
```

```json
"TOUCHES"
```

```json
"WITHIN"
```

```json
"DISJOINT"
```

```json
"CROSSES"
```

```json
"EQUALS"
```

## namespace

A namespace to refer during the connections establishment


`namespace`

-   is optional
-   Type: `string` ([Storage (i.e. Cassandra) namespace](delete-message-properties-storage-ie-cassandra-namespace.md))
-   can be null
-   defined in: [The Root Schema](delete-message-properties-storage-ie-cassandra-namespace.md "\#/properties/namespace#/properties/namespace")

### namespace Type

`string` ([Storage (i.e. Cassandra) namespace](delete-message-properties-storage-ie-cassandra-namespace.md))

### namespace Constraints

**pattern**: the string must match the following regular expression:

```regexp
^(.*)$
```
