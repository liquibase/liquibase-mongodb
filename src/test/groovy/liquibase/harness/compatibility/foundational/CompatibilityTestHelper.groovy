package liquibase.harness.compatibility.foundational

import groovy.transform.ToString
import groovy.transform.builder.Builder
import liquibase.Scope
import liquibase.database.Database
import liquibase.harness.config.DatabaseUnderTest
import liquibase.harness.config.TestConfig
import liquibase.harness.util.DatabaseConnectionUtil
import liquibase.harness.util.FileUtils

class CompatibilityTestHelper {

    final static String baseChangelogPath = "liquibase/harness/compatibility/foundational/changelogs"
    final static List supportedChangeLogFormats = ['xml', 'json', 'yml', 'yaml'].asImmutable()

    static List<TestInput> buildTestInput(String changelogPathSpecification) {
        String commandLineInputFormat = System.getProperty("inputFormat")
        String commandLineChangeObjects = System.getProperty("changeObjects")
        List commandLineChangeObjectList = Collections.emptyList()
        if (commandLineChangeObjects) {
            commandLineChangeObjectList = Arrays.asList(commandLineChangeObjects.contains(",")
                    ? commandLineChangeObjects.split(",")
                    : commandLineChangeObjects)
        }
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
            for (def changeLogEntry : FileUtils.resolveInputFilePaths(databaseUnderTest, specificChangelogPath, TestConfig.instance.inputFormat).entrySet()) {
                if (!commandLineChangeObjectList || commandLineChangeObjectList.contains(changeLogEntry.key)) {
                    inputList.add(TestInput.builder()
                            .databaseName(databaseUnderTest.name)
                            .url(databaseUnderTest.url)
                            .dbSchema(databaseUnderTest.dbSchema)
                            .username(databaseUnderTest.username)
                            .password(databaseUnderTest.password)
                            .version(databaseUnderTest.version)
                            .change(changeLogEntry.key)
                            .pathToChangeLogFile(changeLogEntry.value)
                            .database(databaseUnderTest.database)
                            .build())
                }
            }
        }
        return inputList
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
        String pathToChangeLogFile
        Database database
    }
}
