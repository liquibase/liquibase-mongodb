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
        String specificChangelogPath = baseChangelogPath + changelogPathSpecification
        String commandLineInputFormats = System.getProperty("inputFormat")
        String commandLineChangeObjects = System.getProperty("changeObjects")

        List inputFormatList = Collections.emptyList()
        List commandLineChangeObjectList = Collections.emptyList()

        if (commandLineInputFormats) {
            TestConfig.instance.inputFormat = commandLineInputFormats
        }

        inputFormatList = Arrays.asList(TestConfig.instance.inputFormat.contains(",")
                ? TestConfig.instance.inputFormat.split(",")
                : TestConfig.instance.inputFormat)

        for(String inputFormat: inputFormatList){
            if (!supportedChangeLogFormats.contains(inputFormat)) {
                throw new IllegalArgumentException(inputFormat + " inputFormat is not supported")
            }
        }

        if (commandLineChangeObjects) {
            commandLineChangeObjectList = Arrays.asList(commandLineChangeObjects.contains(",")
                    ? commandLineChangeObjects.split(",")
                    : commandLineChangeObjects)
        }

        Scope.getCurrentScope().getUI().sendMessage("Only " + TestConfig.instance.inputFormat
                + " input files are taken into account for this test run")

        List<TestInput> inputList = new ArrayList<>()
        DatabaseConnectionUtil databaseConnectionUtil = new DatabaseConnectionUtil()
        for (DatabaseUnderTest databaseUnderTest : databaseConnectionUtil
                .initializeDatabasesConnection(TestConfig.instance.getFilteredDatabasesUnderTest())) {
            for (String inputFormat: inputFormatList) {
                for (def changeLogEntry : FileUtils.resolveInputFilePaths(databaseUnderTest, specificChangelogPath, inputFormat).entrySet()) {
                    if (!commandLineChangeObjectList || commandLineChangeObjectList.contains(changeLogEntry.key)) {
                        inputList.add(TestInput.builder()
                                .databaseName(databaseUnderTest.name)
                                .url(databaseUnderTest.url)
                                .dbSchema(databaseUnderTest.dbSchema)
                                .username(databaseUnderTest.username)
                                .password(databaseUnderTest.password)
                                .version(databaseUnderTest.version)
                                .change(changeLogEntry.key)
                                .inputFormat(inputFormat)
                                .pathToChangeLogFile(changeLogEntry.value)
                                .database(databaseUnderTest.database)
                                .build())
                    }
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
        String inputFormat
        Database database
    }
}
