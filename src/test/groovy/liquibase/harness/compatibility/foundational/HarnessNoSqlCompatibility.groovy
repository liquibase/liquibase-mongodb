package liquibase.harness.compatibility.foundational


import liquibase.ext.mongodb.change.CreateCollectionChange
import liquibase.ext.mongodb.database.MongoConnection
import liquibase.harness.config.DatabaseUnderTest
import liquibase.harness.config.TestConfig
import liquibase.harness.util.MongoTestUtils
import liquibase.harness.util.rollback.RollbackStrategy
import org.json.JSONArray
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONCompareMode
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static liquibase.harness.util.FileUtils.getJSONFileContent
import static liquibase.harness.util.JSONUtils.compareJSONArrays

@Unroll
class HarnessNoSqlCompatibility extends Specification {
    @Shared
    RollbackStrategy strategy
    @Shared
    List<DatabaseUnderTest> databases

    def setupSpec() {
        databases = TestConfig.instance.getFilteredDatabasesUnderTest()
        strategy = MongoTestUtils.chooseRollbackStrategy()
        strategy.prepareForRollback(databases)
    }

    def "apply #testInput.change against #testInput.databaseName #testInput.version"() {
        given: "read input data"
        String expectedResultSet = getJSONFileContent(testInput.change, testInput.databaseName, testInput.version,
                "liquibase/harness/compatibility/foundational/expectedResultSet")
        Map<String, Object> argsMap = new HashMap()
        argsMap.put("url", testInput.url)
        argsMap.put("username", testInput.username)
        argsMap.put("password", testInput.password)

        String basePath = "liquibase/harness/compatibility/foundational/changelogs/nosql"
        ArrayList<String> changelogList = new ArrayList<>()
        changelogList.add("${basePath}/${testInput.change}.xml")
        changelogList.add("${basePath}/${testInput.change}.json")
        changelogList.add("${basePath}/${testInput.change}.yaml")

        boolean shouldRunChangeSet

        and: "fail test if expectedResultSet is not provided"
        shouldRunChangeSet = expectedResultSet != null
        assert shouldRunChangeSet: "No expectedResultSet for ${testInput.change} against " +
                "${testInput.database.shortName} ${testInput.database.databaseMajorVersion}." +
                "${testInput.database.databaseMinorVersion}"

        and: "check database under test is online"
        def connection = testInput.database.getConnection()
        shouldRunChangeSet = connection instanceof MongoConnection
        assert shouldRunChangeSet: "Database ${testInput.databaseName} ${testInput.version} is offline!"

        and: "execute Liquibase validate command to ensure that changelog is valid"
        for (int i = 0; i < changelogList.size(); i++) {
            argsMap.put("changeLogFile", changelogList.get(i))
            MongoTestUtils.executeCommandScope("validate", argsMap)
        }

        List<String> collectionNames = new ArrayList<>()

        when: "execute changelogs using liquibase update command"
        for (int i = 0; i < changelogList.size(); i++) {
            argsMap.put("changeLogFile", changelogList.get(i))
            MongoTestUtils.executeCommandScope("update", argsMap)
            if (!testInput.change.contains("Command")) {
                final String collectionName = ((CreateCollectionChange) MongoTestUtils.getChangesets(changelogList.get(i), testInput.database)
                        .get(0).getChanges().get(0)).getCollectionName()
                collectionNames.add(collectionName)
            }
        }

        and: "execute Liquibase tag command. Tagging last row of DATABASECHANGELOG table"
        argsMap.remove("changeLogFile")
        argsMap.put("tag", "test_tag")
        MongoTestUtils.executeCommandScope("tag", argsMap)

        and: "execute Liquibase history command"
        for (int i = 0; i < changelogList.size(); i++) {
            assert MongoTestUtils.executeCommandScope("history", argsMap).toString().contains(changelogList.get(i))
        }

        and: "execute Liquibase status command"
        for (int i = 0; i < changelogList.size(); i++) {
            argsMap.put("changeLogFile", changelogList.get(i))
            assert MongoTestUtils.executeCommandScope("status", argsMap).toString().contains("is up to date")
        }

        then: "obtain result set, compare it to expected result set"
        def generatedResultSet = ((MongoConnection) connection).getMongoDatabase().getCollection("DATABASECHANGELOG").find().iterator().collect()
        def generatedResultSetArray = new JSONArray(generatedResultSet)
        def expectedResultSetArray = new JSONObject(expectedResultSet).getJSONArray(testInput.change)
        assert compareJSONArrays(generatedResultSetArray, expectedResultSetArray, JSONCompareMode.LENIENT)

        and: "check for actual presence of created object"
        if (!testInput.change.startsWith("drop")) {
            assert MongoTestUtils.getCollections(connection as MongoConnection).containsAll(collectionNames)
        }

        cleanup: "rollback changes if we ran changeSet"
        if (shouldRunChangeSet) {
            for (int i = 0; i < changelogList.size(); i++) {
                argsMap.put("changeLogFile", changelogList.get(i))
                strategy.performRollback(argsMap)
            }
        }

        where: "test input in next data table"
        testInput << CompatibilityTestHelper.buildTestInput("/nosql")
    }

    def cleanupSpec() {
        strategy.cleanupDatabase(databases)
    }
}
