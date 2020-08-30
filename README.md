# Liquibase MongoDB Extension

[![Build Status](https://travis-ci.com/liquibase/liquibase-mongodb.svg?branch=master)](https://travis-ci.com/liquibase/liquibase-mongodb)

## Table of contents

1. [Introduction](#introduction)
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

<a name="implemented-changes"></a>
## Implemented Changes:

A couple of Changes were implemented until identified that majority of the operations can be achieved using `db.runCommand()` and `db.adminCommand()`

* [createCollection](https://docs.mongodb.com/manual/reference/method/db.createCollection/#db.createCollection) - 
Creates a collection with validator
* [createIndex](https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/#db.collection.createIndex) - 
Creates an index for a collection
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
mongo-java-driver:3.10.2
```

### Installing

* Clone the project
* [Run tests](#running-tests)

<a name="running-tests"></a>
## Running tests

### Adjust connection string
 
Connection url can be adjusted here: [`db.connection.uri`](./src/test/resources/application-test.properties)
Integration tests are run by enabling `run-its` profile 

The tests require Mongo 4.0 to execute successfully.

### Run integration tests

```
mvn clean install -Prun-its
```

<a name="integration"></a>
## Integration

### Add dependency: 

```
<dependency>
    <groupId>org.liquibase.ext</groupId>
    <artifactId>liquibase-mongodb</artifactId>
    <version>${liquibase-mongodb.version}</version>
</dependency>
```
### Java call:
```
MongoConnection mongoConnection;
MongoExecutor mongoExecutor;
MongoLiquibaseDatabase database;

mongoConnection = new MongoConnection("mongodb://localhost:27017/test_db?socketTimeoutMS=100&connectTimeoutMS=100&serverSelectionTimeoutMS=100");

//Can be achieved by excluding the package to scan or pass package list via system.parameter
//ServiceLocator.getInstance().getPackages().remove("liquibase.executor");
//Another way is to register the executor against a Db

database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance().findCorrectDatabaseImplementation(mongoConnection);
database.setConnection(mongoConnection);

mongoExecutor = new MongoExecutor();
mongoExecutor.setDatabase(database);

ExecutorService.getInstance().setExecutor(database, mongoExecutor);

 final Liquibase liquibase = new Liquibase("liquibase/ext/changelog.create-users.test.xml", new ClassLoaderResourceAccessor(), database);
 liquibase.update("{}");
``` 

<a name="contributing"></a>
## Liquibase CLI

To use with the liquibase CLI, all changesets must use the runWith attribute of `mongodb`, this is only compatible with Liquibase 4.0.0

## Docker

Included is a docker-compose file that will start Mongo up as well as run the test liquibase migrations against a mongo server. Instructions:

1. `docker-compose up -d mongo`
2. Edit `src/test/resources/application-test.properties` and change the port for mongo to 37017 (e.g., `sed -i -E "s/27017/37017/" src/test/resources/application-test.properties`)
  * Note sporadically tests may fail because short timeouts.
3. `mvn clean install -Prun-its`
4. `docker-compose up --build liquibase-cli-1`

## Contributing

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

<a name="license"></a>
## License

This project is licensed under the Apache License Version 2.0 - see the [LICENSE.md](LICENSE.md) file for details



