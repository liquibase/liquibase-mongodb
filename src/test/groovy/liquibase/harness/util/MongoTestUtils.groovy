package liquibase.harness.util


import liquibase.ext.mongodb.database.MongoConnection

import java.util.stream.Collectors
import java.util.stream.StreamSupport

class MongoTestUtils extends TestUtils {

    static List<String> getCollections(final MongoConnection connection) {
        return StreamSupport.stream(connection.getMongoDatabase().listCollectionNames().spliterator(), false)
                .collect(Collectors.toList());
    }
}
