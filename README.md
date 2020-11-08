# Liquibase MongoDB Extension

[![Build Status](https://travis-ci.com/liquibase/liquibase-mongodb.svg?branch=master)](https://travis-ci.com/liquibase/liquibase-mongodb)

## Table of contents

1. [Introduction](#introduction)
1. [Release Notes](#release-notes)
1. [Implemented Changes](#implemented-changes)
1. [Getting Started](#getting-started)
1. [Running tests](#running-tests)
1. [Integration](#integration)
1. [Contributing](#contributing)
1. [License](#license)

<a name="introduction"></a>
## Introduction

This is a Liquibase extension for MongoDB support. 

It resulted as an alternative to existing MongoDB evolution tools.  
Majority of them are basically wrappers over [`db.eval`](https://docs.mongodb.com/manual/reference/method/db.eval/#db.eval) shell method that is deprecated staring from MongoDB 4.2.

In order to call specific `mongo-java-driver` specific methods, 
Liquibase turned to be the most feasible tool to extend as it allows to define change sets to fit driver methods arguments.

<a name="release-notes"></a>
## Release Notes

#### 4.1.2
* Fixed [Rollback doesn't work with liquibase-mongodb-4.0.0.2 extension](https://github.com/liquibase/liquibase-mongodb/issues/38)
* Added dropCollection and dropIndex Changes
* Added NoSql JSON Parser which can pass raw JSON for a property like this:
```json 
{
    "options" : 
        {
            "$rawJson" : { ... }
        }
}
```
For the command line is required to copy to `[liquibase]/lib` 
libraries : `jackson-annotations-2.11.3.jar, jackson-core-2.11.3.jar, jackson-databind-2.11.3.jar`

* New properties added
```properties
# If disabled can be used on API which do not support validators (Azure Cosmos DB with Mongo API, Amazon DocumentDB)
liquibase.mongodb.supportsValidator=true
# If enabled will adjust indexes and validators for Liquibase tracking tables LOCK and CHANGELOG. Can be disabled if sure Liquibase not updated.
liquibase.mongodb.adjustTrackingTablesOnStartup=true
```
* Overridden Liquibase table names removed. Now will be used the default ones in Liquibase. If previous releases used then table names should be explicitly passed as parameters.
Currently, by default as Liquibase default :`DATABASECHANGELOGLOCK, DATABASECHANGELOG`
Previous releases used by default : `databaseChangeLogLock, databaseChangeLog`

#### 4.1.1
* Support for Liquibase 4.1.1

#### 4.1.0
* Support for Liquibase 4.1.0

#### 4.0.0
* Works with Liquibase v4.0.0

#### 3.10.0
* Support for Liquibase 3.10

#### 3.9.0
* First release

<a name="implemented-changes"></a>
## Implemented Changes:

A couple of Changes were implemented until identified that majority of the operations can be achieved using `db.runCommand()` and `db.adminCommand()`

* [createCollection](https://docs.mongodb.com/manual/reference/method/db.createCollection/#db.createCollection) - 
Creates a collection with validator
* [dropCollection](https://docs.mongodb.com/manual/reference/method/db.collection.drop/#db-collection-drop) - 
Removes a collection or view from the database
* [createIndex](https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/#db.collection.createIndex) - 
Creates an index for a collection
* [dropIndex](https://docs.mongodb.com/manual/reference/method/db.collection.dropIndex/#db.collection.dropIndex) - 
Drops index for a collection by keys
* [insertMany](https://docs.mongodb.com/manual/reference/method/db.collection.insertMany/#db.collection.insertMany) - 
Inserts multiple documents into a collection
* [insertOne](https://docs.mongodb.com/manual/tutorial/insert-documents/#insert-a-single-document) - 
Inserts a Single Document into a collection
* [__runCommand__](https://docs.mongodb.com/manual/reference/method/db.runCommand/#db-runcommand) - 
Provides a helper to run specified database commands. This is the preferred method to issue database commands, as it provides a consistent interface between the shell and drivers
* [__adminCommand__](https://docs.mongodb.com/manual/reference/method/db.adminCommand/#db.adminCommand) - 
Provides a helper to run specified database commands against the admin database

<a name="getting-started"></a>
## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. 

### Prerequisites

```
mongo-java-driver:3.12.7
```

### Installing

* Clone the project
* [Run tests](#running-tests)

<a name="running-tests"></a>
## Running tests

### Adjust connection string
 
Connection url can be adjusted here: [`url`](./src/test/resources/liquibase.properties)
[Connection String Format](https://docs.mongodb.com/manual/reference/connection-string/)
Run Integration tests by enabling `run-its` profile 

### Run integration tests

```shell script
mvn clean install -Prun-its
```

<a name="integration"></a>
## Integration

### Add dependency: 

```xml
<dependency>
    <groupId>org.liquibase.ext</groupId>
    <artifactId>liquibase-mongodb</artifactId>
    <version>${liquibase-mongodb.version}</version>
</dependency>
```
### Java call:
```java
public class Application {
    public static void main(String[] args) {
        MongoLiquibaseDatabase database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance().openDatabase(url, null, null, null, null);
        Liquibase liquibase = new Liquibase("liquibase/ext/changelog.generic.test.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.update("");
    }
}
```

<a name="contributing"></a>
## Contributing

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

<a name="license"></a>
## License

This project is licensed under the Apache License Version 2.0 - see the [LICENSE.md](LICENSE.md) file for details



