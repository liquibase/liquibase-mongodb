# Using the Liquibase Test Harness in the MongoDB Extension
The liquibase-mongodb extension now comes with integration test support via the liquibase-test-harness.
This Liquibase test framework is designed to *also* make it easier for you to test your extensions.

### Configuring your project

#### Configuring your connections

- Use the provided `harness-config.yml` file in `src/test/resources` directory.
- Update this file to add the connection information for all the databases you want the Liquibase MongoDB extension to be tested against.
    - *If this config file does not exist, create a new one using this as an example : https://github.com/liquibase/liquibase-test-harness/blob/main/src/test/resources/harness-config.yml*
- Your database under test needs to be completely empty prior to the Harness tests running.

#### Executing the Harness NoSQL Foundational test
- From your IDE, right-click on the `HarnessNoSqlCompatibility` test class present in `src/test/groovy/liquibase/harness/compatibility/foundational` directory.
    - Doing so, will allow you to execute NoSQL Foundational harness suite. Test data for this test should be located in the next directories:
    - `src/test/resources/liquibase/harness/compatibility/foundational/changelogs/nosql` for the changelogs you want to test. XML, JSON & YAML formats are supported.
    - `src/test/resources/liquibase/harness/compatibility/foundational/expectedResultSet/mongodb` for the JSON format files with the values you expect to be present in the DATABASECHANGELOG table after applying your changelog files.
    In the key:value format like: `"id":"1"`, `"author":"as"`,`"description":"createCollection collectionName=towns"`, etc. Use existing files as an example. 
   
##### Alternative ways to run the Harness test suites
- Using maven by executing next command:
  `mvn -Dtest="HarnessNoSqlCompatibility" -DdbName=mongodb(optional) -DdbUsername=USERNAME(optional) -DdbPassword=PASSWORD(optional) -DdbUrl=URL(optional) test`
    - where USERNAME, PASSWORD and URL are connection credentials.

#### Troubleshooting notes
- If your IDE doesn't allow you to run HarnessNoSqlCompatibility as a test class, mark test/groovy folder as test classes folder in your IDE
