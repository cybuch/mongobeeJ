[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/cybuch/mongobeeJ/blob/master/LICENSE)
---

**mongobeeJ** is a Java tool which helps you to *manage changes* in your MongoDB and *synchronize* them with your application.
The concept is very similar to other db migration tools such as [Liquibase](http://www.liquibase.org) or [Flyway](http://flywaydb.org) but *without using XML/JSON/YML files*.
mongobeeJ is built on top of [mongobee](https://github.com/mongobee/mongobee)

The goal is to keep this tool simple and comfortable to use.

**mongobeeJ** provides new approach for adding changes (change sets) based on Java classes and methods with appropriate annotations.

## Deprecation Notice 
**mongobeeJ** is no longer actively developed. It will be supported (bug fixes and dependency upgrades) for the sake of legacy projects that use **mongobeeJ** or **mongobee**. 
However, it's recommended to migrate active projects from **mongobeeJ** to other tools like [Mongock](https://github.com/cloudyrock/mongock) or [Sherlock](https://coditory.github.io/sherlock-distributed-lock/migrator/).

### Getting started

### Add a dependency

With Maven
```xml
<dependency>
  <groupId>com.github.cybuch</groupId>
  <artifactId>mongobeej</artifactId>
  <version>1.0.1</version>
</dependency>
```
With Gradle
```groovy
compile 'com.github.cybuch:mongobeej:1.0.1'
```

### Migrating from Mongobee
Replace mongobee dependency with mongobeeJ and then simply change `com.github.mongobee` in import statements to: `com.github.mongobeej`

### Usage with Spring
You need to instantiate Mongobee object and provide some configuration.
If you use Spring can be instantiated as a singleton bean in the Spring context. 
In this case the migration process will be executed automatically on startup.

```java
@Bean
public Mongobee mongobee() {
  Mongobee runner = new Mongobee("mongodb://YOUR_DB_HOST:27017/DB_NAME");
  runner.setDbName("yourDbName");         // db name must be set if not set in URI
  runner.setChangeLogsScanPackage(
       "com.example.yourapp.changelogs"); // the package to be scanned for changesets
  return runner;
}
```

or

```java
@Bean
public Mongobee mongobee(MongoClient mongoClient) {
  Mongobee runner = new MongobeemongoClient);
  runner.setDbName("yourDbName");         // db name must be set
  runner.setChangeLogsScanPackage(
       "com.example.yourapp.changelogs"); // the package to be scanned for changesets
  return runner;
}
```


### Usage without Spring
Using mongobee without a spring context has similar configuration but you have to remember to run `execute()` method to start a migration process.

```java
Mongobee runner = new Mongobee("mongodb://YOUR_DB_HOST:27017/DB_NAME");
runner.setDbName("yourDbName");         // db name must be set if not set in URI
runner.setChangeLogsScanPackage(
     "com.example.yourapp.changelogs"); // package to scan for changesets
runner.execute();         //  ------> starts migration changesets
```

Above examples provide minimal configuration. `Mongobee` object provides some other possibilities (setters) to make the tool more flexible:

```java
runner.setChangelogCollectionName(logColName);   // default is dbchangelog, collection with applied change sets
runner.setLockCollectionName(lockColName);       // default is mongobeelock, collection used during migration process
runner.setEnabled(shouldBeEnabled);              // default is true, migration won't start if set to false
```

MongoDB URI format:
```
mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database[.collection]][?options]]
```
[More about URI](https://docs.mongodb.com/manual/reference/connection-string/)


### Creating change logs

`ChangeLog` contains bunch of `ChangeSet`s. `ChangeSet` is a single task (set of instructions made on a database). In other words `ChangeLog` is a class annotated with `@ChangeLog` and containing methods annotated with `@ChangeSet`.

```java 
package com.example.yourapp.changelogs;

@ChangeLog
public class DatabaseChangelog {
  
  @ChangeSet(order = "001", id = "someChangeId", author = "testAuthor")
  public void importantWorkToDo(MongoTemplate mongoTemplate) {
     // task implementation
  }
}
```
#### @ChangeLog

Class with change sets must be annotated by `@ChangeLog`. There can be more than one change log class but in that case `order` argument should be provided:

```java
@ChangeLog(order = "001")
public class DatabaseChangelog {
  //...
}
```
ChangeLogs are sorted alphabetically by `order` argument and changesets are applied due to this order.

#### @ChangeSet

Method annotated by @ChangeSet is taken and applied to the database. History of applied change sets is stored in a collection called `dbchangelog` (by default) in your MongoDB

##### Annotation parameters:

`order` - string for sorting change sets in one changelog. Sorting in alphabetical order, ascending. It can be a number, a date etc.

`id` - name of a change set, **must be unique** for all change logs in a database

`author` - author of a change set

`runAlways` - _[optional, default: false]_ changeset will always be executed but only first execution event will be stored in dbchangelog collection

##### Defining ChangeSet methods
Method annotated by `@ChangeSet` can have one of the following definition:

```java
@ChangeSet(order = "001", id = "someChangeWithoutArgs", author = "testAuthor")
public void someChange1() {
   // method without arguments can do some non-db changes
}

@ChangeSet(order = "002", id = "someChangeWithMongoDatabase", author = "testAuthor")
public void someChange2(MongoDatabase db) {
  // type: com.mongodb.client.MongoDatabase : original MongoDB driver v. 3.x, operations allowed by driver are possible
  // example: 
  MongoCollection<Document> mycollection = db.getCollection("mycollection");
  Document doc = new Document("testName", "example").append("test", "1");
  mycollection.insertOne(doc);
}

@ChangeSet(order = "003", id = "someChangeWithSpringDataTemplate", author = "testAuthor")
public void someChange3(MongoTemplate mongoTemplate) {
  // type: org.springframework.data.mongodb.core.MongoTemplate
  // Spring Data integration allows using MongoTemplate in the ChangeSet
  // example:
  mongoTemplate.save(myEntity);
}

@ChangeSet(order = "004", id = "someChangeWithSpringDataTemplate", author = "testAuthor")
public void someChange4(MongoTemplate mongoTemplate, Environment environment) {
  // type: org.springframework.data.mongodb.core.MongoTemplate
  // type: org.springframework.core.env.Environment
  // Spring Data integration allows using MongoTemplate and Environment in the ChangeSet
}
```

### Using Spring profiles
     
**mongobeeJ** accepts Spring's `org.springframework.context.annotation.Profile` annotation. If a change log or change set class is annotated  with `@Profile`, 
then it is activated for current application profiles.

_Example 1_: annotated change set will be invoked for a `dev` profile
```java
@Profile("dev")
@ChangeSet(author = "testuser", id = "myDevChangest", order = "01")
public void devEnvOnly(MongoTemplate mongoTemplate) {
  // ...
}
```
_Example 2_: all change sets in a changelog will be invoked for a `test` profile
```java
@ChangeLog(order = "1")
@Profile("test")
public class ChangelogForTestEnv{
  @ChangeSet(author = "testuser", id = "myTestChangest", order = "01")
  public void testingEnvOnly(MongoTemplate mongoTemplate) {
    // ...
  } 
}
```

#### Enabling @Profile annotation (option)
      
To enable the `@Profile` integration, please inject `org.springframework.core.env.Environment` to you runner.

```java      
@Bean @Autowired
public Mongobee mongobee(Environment environment) {
  Mongobee runner = new Mongobee(uri);
  runner.setSpringEnvironment(environment)
  //... etc
}
```
