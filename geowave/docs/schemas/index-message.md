## IndexMessage Type

`object` ([IndexMessage](index-message.md))

# IndexMessage Properties

| Property                | Type     | Required | Nullable       | Defined by                                                                                                                 |
| :---------------------- | -------- | -------- | -------------- | :------------------------------------------------------------------------------------------------------------------------- |
| [indices](#indices)     | `array`  | Required | cannot be null | [IndexMessage](index-message-properties-the-indices-schema.md "\#/properties/indices#/properties/indices")                 |
| [typeName](#typeName)   | `string` | Required | cannot be null | [IndexMessage](index-message-properties-the-typename-schema.md "\#/properties/typeName#/properties/typeName")              |
| [dataType](#dataType)   | `string` | Required | cannot be null | [IndexMessage](index-message-properties-the-datatype-schema.md "\#/properties/dataType#/properties/dataType")              |
| [namespace](#namespace) | `string` | Optional | can be null    | [IndexMessage](index-message-properties-storage-ie-cassandra-namespace.md "\#/properties/namespace#/properties/namespace") |

## indices




`indices`

-   is required
-   Type: `object[]` ([The Items Schema](index-message-properties-the-indices-schema-the-items-schema.md))
-   cannot be null
-   defined in: [IndexMessage](index-message-properties-the-indices-schema.md "\#/properties/indices#/properties/indices")

### indices Type

`object[]` ([The Items Schema](index-message-properties-the-indices-schema-the-items-schema.md))

## typeName

An arbitrary name that would be used to identify DataType & IndexType and will be used in the ingest message


`typeName`

-   is required
-   Type: `string` ([The typeName Schema](index-message-properties-the-typename-schema.md))
-   cannot be null
-   defined in: [IndexMessage](index-message-properties-the-typename-schema.md "\#/properties/typeName#/properties/typeName")

### typeName Type

`string` ([The typeName Schema](index-message-properties-the-typename-schema.md))

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

## dataType

A dataType this index would be used for


`dataType`

-   is required
-   Type: `string` ([The dataType Schema](index-message-properties-the-datatype-schema.md))
-   cannot be null
-   defined in: [IndexMessage](index-message-properties-the-datatype-schema.md "\#/properties/dataType#/properties/dataType")

### dataType Type

`string` ([The dataType Schema](index-message-properties-the-datatype-schema.md))

### dataType Constraints

**pattern**: the string must match the following regular expression:

```regexp
^(.*)$
```

[try pattern](https://regexr.com/?expression=%5E(.*)%24 "try regular expression with regexr.com")

### dataType Examples

```json
"GEOTIFF"
```

## namespace

A namespace to refer during the connections establishment


`namespace`

-   is optional
-   Type: `string` ([Storage (i.e. Cassandra) namespace](index-message-properties-storage-ie-cassandra-namespace.md))
-   can be null
-   defined in: [IndexMessage](index-message-properties-storage-ie-cassandra-namespace.md "\#/properties/namespace#/properties/namespace")

### namespace Type

`string` ([Storage (i.e. Cassandra) namespace](index-message-properties-storage-ie-cassandra-namespace.md))

### namespace Constraints

**pattern**: the string must match the following regular expression:

```regexp
^(.*)$
```
