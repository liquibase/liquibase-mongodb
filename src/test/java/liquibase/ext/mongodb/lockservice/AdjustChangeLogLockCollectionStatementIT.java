package liquibase.ext.mongodb.lockservice;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import liquibase.ext.AbstractMongoIntegrationTest;
import liquibase.ext.mongodb.statement.FindAllStatement;
import liquibase.ext.mongodb.statement.InsertOneStatement;
import liquibase.lockservice.DatabaseChangeLogLock;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AdjustChangeLogLockCollectionStatementIT extends AbstractMongoIntegrationTest {

    public static final String LOCK_COLLECTION_NAME = "lockCollection";

    protected FindAllStatement findAllStatement = new FindAllStatement(LOCK_COLLECTION_NAME);
    protected SelectChangeLogLockStatement selectChangeLogLockStatement = new SelectChangeLogLockStatement(LOCK_COLLECTION_NAME);

    protected MongoChangeLogLockToDocumentConverter converter = new MongoChangeLogLockToDocumentConverter();

    @Test
    @SneakyThrows
    void executeToJSTest() {
        AdjustChangeLogLockCollectionStatement adjustChangeLogLockCollectionStatement =
                new AdjustChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME);

        assertThat(adjustChangeLogLockCollectionStatement.getCommandName()).isEqualTo("adjustChangeLogLockCollection");
        assertThat(adjustChangeLogLockCollectionStatement.getCollectionName()).isEqualTo(LOCK_COLLECTION_NAME);
        assertThat(adjustChangeLogLockCollectionStatement.toJs()).isEqualTo(
                "db.adjustChangeLogLockCollection({\"collMod\": \"lockCollection\", \"validator\": " +
                        "{\"$jsonSchema\": {\"bsonType\": \"object\", \"description\": \"Database Lock Collection\", " +
                        "\"required\": [\"_id\", \"locked\"], \"properties\": {" +
                        "\"_id\": {\"bsonType\": \"int\", \"description\": \"Unique lock identifier\"}, " +
                        "\"locked\": {\"bsonType\": \"bool\", \"description\": \"Lock flag\"}, " +
                        "\"lockGranted\": {\"bsonType\": \"date\", \"description\": \"Timestamp when lock acquired\"}, " +
                        "\"lockedBy\": {\"bsonType\": [\"string\", \"null\"], \"description\": \"Owner of the lock\"}}}}, " +
                        "\"validationLevel\": \"strict\", \"validationAction\": \"error\"});");

        new CreateChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME).execute(database);
        adjustChangeLogLockCollectionStatement.execute(database);

        Optional<Document> options = ofNullable(connection.getDatabase().listCollections().first()).map(c -> (Document)c.get("options"));
        assertThat(options.map(Document::toJson).orElse(""))
                .isEqualToIgnoringWhitespace(CreateChangeLogLockCollectionStatement.OPTIONS);

    }

    @Test
    @SneakyThrows
    void executeTest() {

        new CreateChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME).execute(database);

        MongoCollection<Document> collection = connection.getDatabase().getCollection(LOCK_COLLECTION_NAME);

        List<Document> indexes = new ArrayList<>();

        collection.listIndexes().into(indexes);

        // Has just default index
        assertThat(indexes).hasSize(1);
        assertThat(indexes.get(0).get("name")).isEqualTo("_id_");

        // Options not present
        final Document collectionInfo =
                connection.getDatabase().listCollections().filter(Filters.eq("name", LOCK_COLLECTION_NAME)).first();

        assertThat(collectionInfo)
                .isNotNull()
                .returns(TRUE, c -> ((Document) c.get("options")).isEmpty());

        // With explicit supportsValidator=false should not be changed
        database.setSupportsValidator(FALSE);
        new AdjustChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME).execute(database);
        final Document collectionInfoExplicitNoAdjustment =
                connection.getDatabase().listCollections().filter(Filters.eq("name", LOCK_COLLECTION_NAME)).first();

        assertThat(collectionInfoExplicitNoAdjustment)
                .isNotNull()
                .returns(TRUE, c -> ((Document) c.get("options")).isEmpty());

        // With explicit supportsValidator=true should be changed
        database.setSupportsValidator(TRUE);
        new AdjustChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME).execute(database);

        indexes.clear();
        connection.getDatabase().getCollection(LOCK_COLLECTION_NAME).listIndexes().into(indexes);
        assertThat(indexes).hasSize(1);
        assertThat(indexes.get(0).get("name")).isEqualTo("_id_");

        final Document collectionInfoAdjusted =
                connection.getDatabase().listCollections().filter(Filters.eq("name", LOCK_COLLECTION_NAME)).first();

        assertThat(collectionInfoAdjusted)
                .isNotNull()
                .returns(FALSE, c -> ((Document) c.get("options")).isEmpty())
                .returns(FALSE, c -> ((Document) ((Document) c.get("options")).get("validator")).isEmpty())
                .returns("error", c -> ((Document) c.get("options")).get("validationAction"))
                .returns("strict", c -> ((Document) c.get("options")).get("validationLevel"));
    }

    @Test
    @SneakyThrows
    void insertDataTest() {
        // Returns empty even when collection does not exists
        assertThat(findAllStatement.queryForList(database)).isEmpty();

        // Create collection
        new CreateChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME).execute(database);
        database.setSupportsValidator(TRUE);
        new AdjustChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME).execute(database);
        assertThat(findAllStatement.queryForList(database)).isEmpty();

        final Document options = new Document();
        final Document minimal = new Document()
                .append("_id", 1);

        // Minimal not all required fields
        assertThatExceptionOfType(MongoException.class)
                .isThrownBy(() -> new InsertOneStatement(LOCK_COLLECTION_NAME, minimal, options).execute(database))
                .withMessageStartingWith("Command failed. The full response is")
                .withMessageContaining("Document failed validation");

        // Minimal accepted
        minimal.append("locked", true);
        new InsertOneStatement(LOCK_COLLECTION_NAME, minimal, options).execute(database);

        assertThat(findAllStatement.queryForList(database))
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("_id", 1)
                .hasFieldOrPropertyWithValue("locked", true)
                .returns(null, d -> d.get("lockedBy"))
                .returns(null, d -> d.get("lockGranted"));

        // Unique constraint failure
        assertThatExceptionOfType(MongoException.class)
                .isThrownBy(() -> new InsertOneStatement(LOCK_COLLECTION_NAME, minimal, options).execute(database))
                .withMessageStartingWith("Command failed. The full response is")
                .withMessageContaining("E11000 duplicate key error collection");

        // Extra fields are allowed
        minimal.append("_id", 2).append("extraField", "extraValue");
        new InsertOneStatement(LOCK_COLLECTION_NAME, minimal, options).execute(database);
        assertThat(findAllStatement.queryForList(database)).hasSize(2)
                .filteredOn(d -> d.get("_id").equals(2)).hasSize(1).first()
                .hasFieldOrPropertyWithValue("_id", 2)
                .hasFieldOrPropertyWithValue("extraField", "extraValue");

        // Nulls fail validation
        minimal.append("_id", 3).append("lockGranted", null);
        assertThatExceptionOfType(MongoException.class)
                .isThrownBy(() -> new InsertOneStatement(LOCK_COLLECTION_NAME, minimal, options).execute(database))
                .withMessageStartingWith("Command failed. The full response is")
                .withMessageContaining("Document failed validation");

        // Maximum
        final Date lockGranted = new Date();
        final Document maximal = new Document()
                .append("_id", 3)
                .append("lockGranted", lockGranted)
                .append("locked", false)
                .append("lockedBy", "Alex");

        new InsertOneStatement(LOCK_COLLECTION_NAME, maximal, options).execute(database);
        assertThat(findAllStatement.queryForList(database)).hasSize(3)
                .filteredOn(d -> d.get("_id").equals(3)).hasSize(1).first()
                .hasFieldOrPropertyWithValue("_id", 3)
                .hasFieldOrPropertyWithValue("locked", false)
                .hasFieldOrPropertyWithValue("lockGranted", lockGranted)
                .hasFieldOrPropertyWithValue("lockedBy", "Alex")
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.id, 3)
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.locked, false)
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.lockGranted, lockGranted)
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.lockedBy, "Alex");

        // Maximum with allowed null
        maximal.append(MongoChangeLogLock.Fields.id, 4).append(MongoChangeLogLock.Fields.lockedBy, null);
        new InsertOneStatement(LOCK_COLLECTION_NAME, maximal, options).execute(database);
        assertThat(findAllStatement.queryForList(database)).hasSize(4)
                .filteredOn(d -> d.get("_id").equals(4)).hasSize(1).first()
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.id, 4)
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.locked, false)
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.lockGranted, lockGranted)
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.lockedBy, null)
                .returns(null, d -> d.get(MongoChangeLogLock.Fields.lockedBy));

    }

    @Test
    @SneakyThrows
    void insertDataNoValidatorTest() {
        // Returns empty even when collection does not exists
        assertThat(findAllStatement.queryForList(database)).isEmpty();

        // Create collection
        new CreateChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME).execute(database);
        database.setSupportsValidator(FALSE);
        new AdjustChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME).execute(database);
        assertThat(findAllStatement.queryForList(database)).isEmpty();

        final Document options = new Document();
        final Document minimal = new Document()
                .append("_id", 1);

        // Insert a not valid one, check it was persisted
        new InsertOneStatement(LOCK_COLLECTION_NAME, minimal, options).execute(database);

        assertThat(findAllStatement.queryForList(database)).hasSize(1);
    }

    @Test
    @SneakyThrows
    void insertEntityTest() {
        final Date lockGranted = new Date();

        // Create collection
        new CreateChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME).execute(database);
        database.setSupportsValidator(TRUE);
        new AdjustChangeLogLockCollectionStatement(LOCK_COLLECTION_NAME).execute(database);
        assertThat(findAllStatement.queryForList(database)).isEmpty();

        final Document options = new Document();
        final MongoChangeLogLock minimal = new MongoChangeLogLock(1, lockGranted, null, null);

        // Fail on nulls
        assertThatExceptionOfType(MongoException.class)
                .isThrownBy(() -> new InsertOneStatement(LOCK_COLLECTION_NAME, converter.toDocument(minimal), options).execute(database))
                .withMessageStartingWith("Command failed. The full response is")
                .withMessageContaining("Document failed validation");

        // Minimal accepted
        final MongoChangeLogLock defaultConstructor = new MongoChangeLogLock();
        new InsertOneStatement(LOCK_COLLECTION_NAME, converter.toDocument(defaultConstructor), options).execute(database);

        assertThat(findAllStatement.queryForList(database))
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("_id", 1)
                .hasFieldOrPropertyWithValue("locked", true)
                .hasFieldOrPropertyWithValue("lockedBy", "NoArgConstructor")
                .returns(false, d -> isNull(d.get("lockGranted")));

        // Unique constraint failure
        assertThatExceptionOfType(MongoException.class)
                .isThrownBy(() -> new InsertOneStatement(LOCK_COLLECTION_NAME, converter.toDocument(defaultConstructor), options).execute(database))
                .withMessageStartingWith("Command failed. The full response is")
                .withMessageContaining("E11000 duplicate key error collection");

        // Maximum
        final MongoChangeLogLock maximal = new MongoChangeLogLock(2, lockGranted, "Alex", false);

        new InsertOneStatement(LOCK_COLLECTION_NAME, converter.toDocument(maximal), options).execute(database);
        assertThat(findAllStatement.queryForList(database)).hasSize(2)
                .filteredOn(d -> d.get("_id").equals(2)).hasSize(1).first()
                .hasFieldOrPropertyWithValue("_id", 2)
                .hasFieldOrPropertyWithValue("locked", false)
                .hasFieldOrPropertyWithValue("lockGranted", lockGranted)
                .hasFieldOrPropertyWithValue("lockedBy", "Alex")
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.id, 2)
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.locked, false)
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.lockGranted, lockGranted)
                .hasFieldOrPropertyWithValue(MongoChangeLogLock.Fields.lockedBy, "Alex");

        // selectChangeLogLockStatement
        assertThat(converter.fromDocument(selectChangeLogLockStatement.queryForObject(database, Document.class)))
                .isInstanceOf(MongoChangeLogLock.class)
                .returns(1, DatabaseChangeLogLock::getId)
                .returns("NoArgConstructor", DatabaseChangeLogLock::getLockedBy)
                .returns(false, d -> isNull(d.getLockGranted()));
    }


}