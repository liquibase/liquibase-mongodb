package liquibase.ext.mongodb.changelog;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import liquibase.ext.AbstractMongoIntegrationTest;
import liquibase.ext.mongodb.statement.FindAllStatement;
import liquibase.ext.mongodb.statement.InsertOneStatement;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AdjustChangeLogCollectionStatementIT extends AbstractMongoIntegrationTest {

    public static final String LOG_COLLECTION_NAME = "historyLogCollection";

    protected FindAllStatement findAllStatement = new FindAllStatement(LOG_COLLECTION_NAME);

    @Test
    void executeToJSTest() {
        AdjustChangeLogCollectionStatement adjustChangeLogCollectionStatement =
                new AdjustChangeLogCollectionStatement(LOG_COLLECTION_NAME);

        assertThat(adjustChangeLogCollectionStatement.getCommandName()).isEqualTo("adjustChangeLogCollection");
        assertThat(adjustChangeLogCollectionStatement.getCollectionName()).isEqualTo(LOG_COLLECTION_NAME);
        assertThat(adjustChangeLogCollectionStatement.getSupportsValidator()).isTrue();
        assertThat(adjustChangeLogCollectionStatement.toJs()).isEqualTo("db.adjustChangeLogCollection({" +
                "\"collMod\": \"historyLogCollection\", \"validator\": {" +
                "\"$jsonSchema\": {\"bsonType\": \"object\", \"description\": \"Database Change Log Table.\", " +
                "\"required\": [\"id\", \"author\", \"fileName\", \"execType\"], " +
                "\"properties\": {\"id\": {\"bsonType\": \"string\", \"description\": \"Value from the changeSet id attribute.\"}, " +
                "\"author\": {\"bsonType\": \"string\", \"description\": \"Value from the changeSet author attribute.\"}, " +
                "\"fileName\": {\"bsonType\": \"string\", \"description\": \"Path to the changelog. " +
                "This may be an absolute path or a relative path depending on how the changelog was passed to Liquibase. For best results, " +
                "it should be a relative path.\"}, " +
                "\"dateExecuted\": {\"bsonType\": [\"date\", \"null\"], \"description\": \"Date/time of when the changeSet was executed. " +
                "Used with orderExecuted to determine rollback order.\"}, " +
                "\"orderExecuted\": {\"bsonType\": [\"int\", \"null\"], " +
                "\"description\": \"Order that the changeSets were executed. Used in addition to dateExecuted " +
                "to ensure order is correct even when the databases datetime supports poor resolution.\"}, " +
                "\"execType\": {\"bsonType\": \"string\", \"enum\": [\"EXECUTED\", \"FAILED\", \"SKIPPED\", \"RERAN\", \"MARK_RAN\"], " +
                "\"description\": \"Description of how the changeSet was executed.\"}, " +
                "\"md5sum\": {\"bsonType\": [\"string\", \"null\"], \"description\": \"Checksum of the changeSet when it was executed. " +
                "Used on each run to ensure there have been no unexpected changes to changSet in the changelog file.\"}, " +
                "\"description\": {\"bsonType\": [\"string\", \"null\"], \"description\": \"Short auto-generated human readable description of changeSet.\"}, " +
                "\"comments\": {\"bsonType\": [\"string\", \"null\"], \"description\": \"Value from the changeSet comment attribute.\"}, " +
                "\"tag\": {\"bsonType\": [\"string\", \"null\"], \"description\": \"Tracks which changeSets correspond to tag operations.\"}, " +
                "\"contexts\": {\"bsonType\": [\"string\", \"null\"], \"description\": \"Context expression of the run.\"}, " +
                "\"labels\": {\"bsonType\": [\"string\", \"null\"], \"description\": \"Labels assigned.\"}, " +
                "\"deploymentId\": {\"bsonType\": [\"string\", \"null\"], \"description\": \"Unique identifier generate for a run.\"}, " +
                "\"liquibase\": {\"bsonType\": [\"string\", \"null\"], \"description\": \"Version of Liquibase used to execute the changeSet.\"}}}}, " +
                "\"validationLevel\": \"strict\", \"validationAction\": \"error\"});");
        new CreateChangeLogCollectionStatement(LOG_COLLECTION_NAME).execute(connection);
        adjustChangeLogCollectionStatement.execute(connection);

        Optional<Document> options = ofNullable(connection.getDatabase().listCollections().first()).map(c -> (Document) c.get("options"));
        assertThat(options.map(Document::toJson).orElse(""))
                .isEqualToIgnoringWhitespace(CreateChangeLogCollectionStatement.OPTIONS);
    }

    @Test
    void executeTest() {

        new CreateChangeLogCollectionStatement(LOG_COLLECTION_NAME).execute(connection);

        MongoCollection<Document> collection = connection.getDatabase().getCollection(LOG_COLLECTION_NAME);

        // options not present
        final Document collectionInfo =
                connection.getDatabase().listCollections().filter(Filters.eq("name", LOG_COLLECTION_NAME)).first();

        assertThat(collectionInfo)
                .isNotNull()
                .returns(TRUE, c -> ((Document) c.get("options")).isEmpty());

        // has just default index
        List<Document> indexes = new ArrayList<>();
        collection.listIndexes().into(indexes);
        assertThat(indexes).hasSize(1);
        assertThat(indexes.get(0).get("name")).isEqualTo("_id_");

        // with explicit supportsValidator=false should not change validators should add indexes only
        new AdjustChangeLogCollectionStatement(LOG_COLLECTION_NAME, FALSE).execute(connection);
        final Document collectionInfoExplicitNoAdjustment =
                connection.getDatabase().listCollections().filter(Filters.eq("name", LOG_COLLECTION_NAME)).first();

        assertThat(collectionInfoExplicitNoAdjustment)
                .isNotNull()
                .returns(TRUE, c -> ((Document) c.get("options")).isEmpty());

        indexes.clear();
        connection.getDatabase().getCollection(LOG_COLLECTION_NAME).listIndexes().into(indexes);
        assertThat(indexes).hasSize(2);
        assertThat(indexes.stream().filter(i -> i.get("name").equals("ui_" + LOG_COLLECTION_NAME)).findFirst().orElse(null))
                .isNotNull()
                .returns(TRUE, i -> i.get("unique"))
                .returns(1, i -> ((Document) i.get("key")).get("fileName"))
                .returns(1, i -> ((Document) i.get("key")).get("author"))
                .returns(1, i -> ((Document) i.get("key")).get("id"));

        // with explicit supportsValidator=true validator should be changed indexes remain same
        new AdjustChangeLogCollectionStatement(LOG_COLLECTION_NAME, TRUE).execute(connection);

        final Document collectionInfoAdjusted =
                connection.getDatabase().listCollections().filter(Filters.eq("name", LOG_COLLECTION_NAME)).first();

        assertThat(collectionInfoAdjusted)
                .isNotNull()
                .returns(FALSE, c -> ((Document) c.get("options")).isEmpty())
                .returns(FALSE, c -> ((Document) ((Document) c.get("options")).get("validator")).isEmpty())
                .returns("error", c -> ((Document) c.get("options")).get("validationAction"))
                .returns("strict", c -> ((Document) c.get("options")).get("validationLevel"));

        indexes.clear();
        connection.getDatabase().getCollection(LOG_COLLECTION_NAME).listIndexes().into(indexes);
        assertThat(indexes).hasSize(2);
        assertThat(indexes.stream().filter(i -> i.get("name").equals("ui_" + LOG_COLLECTION_NAME)).findFirst().orElse(null))
                .isNotNull();
    }

    @Test
    void insertDataTest() {
        // Returns empty even when collection does not exists
        assertThat(findAllStatement.queryForList(connection)).isEmpty();

        // Create collection
        new CreateChangeLogCollectionStatement(LOG_COLLECTION_NAME).execute(connection);
        new AdjustChangeLogCollectionStatement(LOG_COLLECTION_NAME, TRUE).execute(connection);
        assertThat(findAllStatement.queryForList(connection)).isEmpty();

        // Minimal not all required fields
        final Document options = new Document();
        final Document minimal = new Document()
                .append("id", "cs1");
        assertThatExceptionOfType(MongoWriteException.class)
                .isThrownBy(() -> new InsertOneStatement(LOG_COLLECTION_NAME, minimal, options).execute(connection))
                .withMessageStartingWith("Document failed validation");

        // Minimal not all required fields
        minimal.append("author", "Alex");
        assertThatExceptionOfType(MongoWriteException.class)
                .isThrownBy(() -> new InsertOneStatement(LOG_COLLECTION_NAME, minimal, options).execute(connection))
                .withMessageStartingWith("Document failed validation");

        // Minimal not all required fields
        minimal.append("fileName", "liquibase/file.xml");
        assertThatExceptionOfType(MongoWriteException.class)
                .isThrownBy(() -> new InsertOneStatement(LOG_COLLECTION_NAME, minimal, options).execute(connection))
                .withMessageStartingWith("Document failed validation");

        // Minimal accepted
        minimal.append("execType", "EXECUTED");
        new InsertOneStatement(LOG_COLLECTION_NAME, minimal, options).execute(connection);

        assertThat(findAllStatement.queryForList(connection))
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.changeSetId, "cs1")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.author, "Alex")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.fileName, "liquibase/file.xml")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.execType, "EXECUTED");

        // Unique constraint failure
        minimal.remove("_id");
        assertThatExceptionOfType(MongoWriteException.class)
                .isThrownBy(() -> new InsertOneStatement(LOG_COLLECTION_NAME, minimal, options).execute(connection))
                .withMessageStartingWith("E11000 duplicate key error collection");

        // Extra fields are allowed
        minimal.remove("_id");
        minimal.append("id", "cs2").append("extraField", "extraValue");
        new InsertOneStatement(LOG_COLLECTION_NAME, minimal, options).execute(connection);
        assertThat(findAllStatement.queryForList(connection)).hasSize(2)
                .filteredOn(d -> d.get("id").equals("cs2")).hasSize(1).first()
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.changeSetId, "cs2")
                .hasFieldOrPropertyWithValue("extraField", "extraValue");

        // Nulls fail validation
        minimal.remove("_id");
        minimal.append("id", "cs3").append("fileName", null);
        assertThatExceptionOfType(MongoWriteException.class)
                .isThrownBy(() -> new InsertOneStatement(LOG_COLLECTION_NAME, minimal, options).execute(connection))
                .withMessageStartingWith("Document failed validation");

        // Maximum
        final Date dateExecuted = new Date();
        final Document maximal = new Document()
                .append("id", "cs4")
                .append("author", "Alex")
                .append("fileName", "liquibase/file.xml")
                .append("dateExecuted", dateExecuted)
                .append("orderExecuted", 100)
                .append("execType", "FAILED")
                .append("md5sum", "QWERTY")
                .append("description", "The Description")
                .append("comments", "The Comments")
                .append("tag", "Tags")
                .append("contexts", "Contexts")
                .append("labels", "Labels")
                .append("deploymentId", "The Deployment Id")
                .append("liquibase", "Liquibase Version");

        new InsertOneStatement(LOG_COLLECTION_NAME, maximal, options).execute(connection);
        assertThat(findAllStatement.queryForList(connection)).hasSize(3)
                .filteredOn(d -> d.get("id").equals("cs4")).hasSize(1).first()
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.changeSetId, "cs4")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.author, "Alex")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.fileName, "liquibase/file.xml")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.dateExecuted, dateExecuted)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.orderExecuted, 100)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.execType, "FAILED")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.md5sum, "QWERTY")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.description, "The Description")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.comments, "The Comments")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.tag, "Tags")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.contexts, "Contexts")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.labels, "Labels")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.deploymentId, "The Deployment Id")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.liquibase, "Liquibase Version");

        // Maximum with allowed null
        maximal.remove("_id");
        maximal.append("id", "cs5")
                .append("dateExecuted", null)
                .append("orderExecuted", null)
                .append("md5sum", null)
                .append("description", null)
                .append("comments", null)
                .append("tag", null)
                .append("contexts", null)
                .append("labels", null)
                .append("deploymentId", null)
                .append("liquibase", null);

        new InsertOneStatement(LOG_COLLECTION_NAME, maximal, options).execute(connection);
        assertThat(findAllStatement.queryForList(connection)).hasSize(4)
                .filteredOn(d -> d.get("id").equals("cs5")).hasSize(1).first()
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.changeSetId, "cs5")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.author, "Alex")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.fileName, "liquibase/file.xml")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.dateExecuted, null)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.orderExecuted, null)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.execType, "FAILED")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.md5sum, null)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.description, null)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.comments, null)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.tag, null)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.contexts, null)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.labels, null)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.deploymentId, null)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.liquibase, null);
    }

    @Test
    void insertDataNoValidatorTest() {
        // Returns empty even when collection does not exists
        assertThat(findAllStatement.queryForList(connection)).isEmpty();

        // Create collection
        new CreateChangeLogCollectionStatement(LOG_COLLECTION_NAME).execute(connection);
        new AdjustChangeLogCollectionStatement(LOG_COLLECTION_NAME, FALSE).execute(connection);
        assertThat(findAllStatement.queryForList(connection)).isEmpty();

        final Document options = new Document();
        final Document minimal = new Document()
                .append("_id", 1);

        // Insert a not valid one, check it was persisted
        new InsertOneStatement(LOG_COLLECTION_NAME, minimal, options).execute(connection);

        assertThat(findAllStatement.queryForList(connection)).hasSize(1);
    }

}