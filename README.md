# Liquibase MongoDB Extension

[![Build Status](https://travis-ci.com/liquibase/liquibase-mongodb.svg?branch=master)](https://travis-ci.com/liquibase/liquibase-mongodb)

## Table of contents

1. [Introduction](#introduction)
1. [Release Notes](#release-notes)
1. [Implemented Changes](#implemented-changes)
1. [Connection String Formats](#connection-string)
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
## [Release Notes](./changelog.txt)

<a name="implemented-changes"></a>
## Implemented Changes:

A couple of Changes were implemented until identified that majority of the operations can be achieved using `db.runCommand()` and `db.adminCommand()`

* [createCollection](https://docs.mongodb.com/manual/reference/method/db.createCollection/#db.createCollection) - 
Creates a collection with validator [create](https://docs.mongodb.com/manual/reference/command/create/)
* [dropCollection](https://docs.mongodb.com/manual/reference/method/db.collection.drop/#db-collection-drop) - 
Removes a collection or view from the database [drop](https://docs.mongodb.com/manual/reference/command/drop/)
* [createIndex](https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/#db.collection.createIndex) - 
Creates an index for a collection [createIndexes](https://docs.mongodb.com/manual/reference/command/createIndexes/)
* [dropIndex](https://docs.mongodb.com/manual/reference/method/db.collection.dropIndex/#db.collection.dropIndex) - 
Drops index for a collection by keys [dropIndexes](https://docs.mongodb.com/manual/reference/command/dropIndexes/)
* [insertMany](https://docs.mongodb.com/manual/reference/method/db.collection.insertMany/#db.collection.insertMany) - 
Inserts multiple documents into a collection [insert](https://docs.mongodb.com/manual/reference/command/insert/)
* [insertOne](https://docs.mongodb.com/manual/tutorial/insert-documents/#insert-a-single-document) - 
Inserts a Single Document into a collection [insert](https://docs.mongodb.com/manual/reference/command/insert/)
* [__runCommand__](https://docs.mongodb.com/manual/reference/method/db.runCommand/#db-runcommand) - 
Provides a helper to run specified database commands. This is the preferred method to issue database commands, as it provides a consistent interface between the shell and drivers
* [__adminCommand__](https://docs.mongodb.com/manual/reference/method/db.adminCommand/#db.adminCommand) - 
Provides a helper to run specified database commands against the admin database

<a name="connection-string"></a>
## Connection String Formats

### [Standard Connection String Format](https://docs.mongodb.com/manual/reference/connection-string/index.html#standard-connection-string-format)

`
mongodb://[username:password@]host1[:port1][,...hostN[:portN]][/[defaultauthdb][?options]]
mongodb://mongodb1.example.com:27317,mongodb2.example.com:27017/?replicaSet=mySet&authSource=authDB
`

### [DNS Seed List Connection Format](https://docs.mongodb.com/manual/reference/connection-string/index.html#dns-seed-list-connection-format)

`
mongodb+srv://[username:password@]host[/[database][?options]]
mongodb+srv://server.example.com/
mongodb+srv://:@cluster0.example.com/testdb?authSource=$external&authMechanism=MONGODB-AWS
`

<a name="getting-started"></a>
## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. 

### Prerequisites
 
* Dependencies that have to be available in classpath if run via Liquibase CLI

```
mongodb-driver-sync:4.2.0
snakeyaml:1.27
jackson-annotations:2.11.3
jackson-core:2.11.3
jackson-databind:2.11.3
```

### Installing

* Clone the project

```shell
git clone https://github.com/liquibase/liquibase-mongodb
```  
* [Run tests](#running-tests)

<a name="running-tests"></a>
## Running tests

### Adjust connection string
 
Connection url can be adjusted here: [`url`](./src/test/resources/liquibase.properties)
[Connection String Format](https://docs.mongodb.com/manual/reference/connection-string/)
Run Integration tests by enabling `run-its` profile 

### Run integration tests

```shell
mvn clean install -Prun-its
```

#### Run integration test driver backward compatibility
1. Produce test containing jar
```shell
mvn clean install -Ptest-jar
```
2. Go to test-project
```shell
cd test-project
```
3. Run backward compatibility test with provided 3x driver
```shell
mvn clean install -Prun-its,mongo-3x
```

### Quick Start Examples

[Quick start Application for NoSql liquibase extensions](https://github.com/alexandru-slobodcicov/liquibase-nosql-quickstart)

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



