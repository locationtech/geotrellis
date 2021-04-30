## IngestMessage Type

`object` ([IngestMessage](ingest-message.md))

# IngestMessage Properties

| Property                | Type     | Required | Nullable       | Defined by                                                                                                                   |
| :---------------------- | -------- | -------- | -------------- | :--------------------------------------------------------------------------------------------------------------------------- |
| [typeName](#typeName)   | `string` | Required | cannot be null | [IngestMessage](ingest-message-properties-the-typename-schema.md "\#/properties/typeName#/properties/typeName")              |
| [dataType](#dataType)   | `string` | Required | cannot be null | [IngestMessage](ingest-message-properties-the-datatype-schema.md "\#/properties/dataType#/properties/dataType")              |
| [uri](#uri)             | `string` | Required | cannot be null | [IngestMessage](ingest-message-properties-the-uri-schema.md "\#/properties/uri#/properties/uri")                             |
| [options](#options)     | `object` | Optional | can be null    | [IngestMessage](ingest-message-properties-the-ingest-options.md "\#/properties/options#/properties/options")                 |
| [namespace](#namespace) | `string` | Optional | can be null    | [IngestMessage](ingest-message-properties-storage-ie-cassandra-namespace.md "\#/properties/namespace#/properties/namespace") |

## typeName

A name that is used to identify DataType & IndexType it should be the same as it was configured in the IndexMessage


`typeName`

-   is required
-   Type: `string` ([The typeName Schema](ingest-message-properties-the-typename-schema.md))
-   cannot be null
-   defined in: [IngestMessage](ingest-message-properties-the-typename-schema.md "\#/properties/typeName#/properties/typeName")

### typeName Type

`string` ([The typeName Schema](ingest-message-properties-the-typename-schema.md))

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

A dataType the ingest would happen for


`dataType`

-   is required
-   Type: `string` ([The dataType Schema](ingest-message-properties-the-datatype-schema.md))
-   cannot be null
-   defined in: [IngestMessage](ingest-message-properties-the-datatype-schema.md "\#/properties/dataType#/properties/dataType")

### dataType Type

`string` ([The dataType Schema](ingest-message-properties-the-datatype-schema.md))

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

## uri

Path to a file


`uri`

-   is required
-   Type: `string` ([The uri Schema](ingest-message-properties-the-uri-schema.md))
-   cannot be null
-   defined in: [IngestMessage](ingest-message-properties-the-uri-schema.md "\#/properties/uri#/properties/uri")

### uri Type

`string` ([The uri Schema](ingest-message-properties-the-uri-schema.md))

### uri Constraints

**pattern**: the string must match the following regular expression:

```regexp
^(.*)$
```

[try pattern](https://regexr.com/?expression=%5E(.*)%24 "try regular expression with regexr.com")

### uri Examples

```json
"file://path/to/file"
```

## options

Data type specific options


`options`

-   is optional
-   Type: `object` ([The Ingest Options](ingest-message-properties-the-ingest-options.md))
-   can be null
-   defined in: [IngestMessage](ingest-message-properties-the-ingest-options.md "\#/properties/options#/properties/options")

### options Type

`object` ([The Ingest Options](ingest-message-properties-the-ingest-options.md))

## namespace

A namespace to refer during the connections establishment


`namespace`

-   is optional
-   Type: `string` ([Storage (i.e. Cassandra) namespace](ingest-message-properties-storage-ie-cassandra-namespace.md))
-   can be null
-   defined in: [IngestMessage](ingest-message-properties-storage-ie-cassandra-namespace.md "\#/properties/namespace#/properties/namespace")

### namespace Type

`string` ([Storage (i.e. Cassandra) namespace](ingest-message-properties-storage-ie-cassandra-namespace.md))

### namespace Constraints

**pattern**: the string must match the following regular expression:

```regexp
^(.*)$
```
