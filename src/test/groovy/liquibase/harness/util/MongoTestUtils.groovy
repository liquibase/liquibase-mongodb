package liquibase.harness.util

import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.exception.LiquibaseException
import liquibase.ext.mongodb.database.MongoConnection
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.parser.ChangeLogParser
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor

import java.util.stream.Collectors
import java.util.stream.StreamSupport

class MongoTestUtils extends TestUtils {


    static List<ChangeSet> getChangesets(final String changeSetPath, final MongoLiquibaseDatabase database) throws LiquibaseException {
        final ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
        final ChangeLogParser parser =
                ChangeLogParserFactory.getInstance().getParser(
                        changeSetPath, resourceAccessor
                );

        final DatabaseChangeLog changeLog =
                parser.parse(changeSetPath, new ChangeLogParameters(database), resourceAccessor);
        return changeLog.getChangeSets();
    }

    static List<String> getCollections(final MongoConnection connection) {
        return StreamSupport.stream(connection.getMongoDatabase().listCollectionNames().spliterator(), false)
                .collect(Collectors.toList());
    }
}
