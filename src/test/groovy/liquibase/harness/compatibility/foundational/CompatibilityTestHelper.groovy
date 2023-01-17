package liquibase.harness.compatibility.foundational

import groovy.transform.ToString
import groovy.transform.builder.Builder
import liquibase.Scope
import liquibase.database.Database
import liquibase.database.DatabaseConnection
import liquibase.database.jvm.JdbcConnection
import liquibase.harness.config.DatabaseUnderTest
import liquibase.harness.config.TestConfig
import liquibase.harness.util.DatabaseConnectionUtil
import liquibase.harness.util.FileUtils

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

class CompatibilityTestHelper {

    final static String baseChangelogPath = "liquibase/harness/compatibility/foundational/changelogs"
    final static List supportedChangeLogFormats = ['xml', 'sql', 'json', 'yml', 'yaml'].asImmutable()

    static boolean shouldOpenNewConnection(DatabaseConnection connection, String... dbNames) {
        return connection.isClosed()||Arrays.stream(dbNames).anyMatch({ dbName -> connection.getDatabaseProductName().toLowerCase().contains(dbName) })
    }

    static List<TestInput> buildTestInput(String changelogPathSpecification) {
        String commandLineInputFormat = System.getProperty("inputFormat")
        String specificChangelogPath = baseChangelogPath + changelogPathSpecification
        if (commandLineInputFormat) {
            if (!supportedChangeLogFormats.contains(commandLineInputFormat)) {
                throw new IllegalArgumentException(commandLineInputFormat + " inputFormat is not supported")
            }
            TestConfig.instance.inputFormat = commandLineInputFormat
        }
        Scope.getCurrentScope().getUI().sendMessage("Only " + TestConfig.instance.inputFormat
                + " input files are taken into account for this test run")

        List<TestInput> inputList = new ArrayList<>()
        DatabaseConnectionUtil databaseConnectionUtil = new DatabaseConnectionUtil()
        for (DatabaseUnderTest databaseUnderTest : databaseConnectionUtil
                .initializeDatabasesConnection(TestConfig.instance.getFilteredDatabasesUnderTest())) {
            for (def changeLogEntry : FileUtils.resolveInputFilePaths(databaseUnderTest, specificChangelogPath, "xml").entrySet()) {
                inputList.add(TestInput.builder()
                        .databaseName(databaseUnderTest.name)
                        .url(databaseUnderTest.url)
                        .dbSchema(databaseUnderTest.dbSchema)
                        .username(databaseUnderTest.username)
                        .password(databaseUnderTest.password)
                        .version(databaseUnderTest.version)
                        .change(changeLogEntry.key)
                        .database(databaseUnderTest.database)
                        .build())
            }
        }
        return inputList
    }

    static ResultSet executeQuery(String pathToSql, TestInput testInput) throws SQLException {
        Connection newConnection
        ResultSet resultSet
        if (shouldOpenNewConnection(testInput.database.getConnection(), "firebird")) {
            newConnection = DriverManager.getConnection(testInput.url, testInput.username, testInput.password)
            resultSet = newConnection.createStatement().executeQuery(pathToSql)
            newConnection.close()
        } else {
            JdbcConnection connection = (JdbcConnection) testInput.database.connection
            resultSet = connection.createStatement().executeQuery(pathToSql)
            testInput.database.connection.autoCommit ?: testInput.database.connection.commit()
        }
        return resultSet
    }

    @Builder
    @ToString(includeNames = true, includeFields = true, includePackage = false, excludes = 'database,password')
    static class TestInput {
        String databaseName
        String version
        String username
        String password
        String url
        String dbSchema
        String change
        Database database
    }
}
