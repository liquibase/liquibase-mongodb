package liquibase.ext.mongodb.statement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.client.FindIterable;

import liquibase.ext.AbstractMongoIntegrationTest;
import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;

class UpdateManyStatementIT extends AbstractMongoIntegrationTest {

    private final Document first = new Document("name", "first");
    private final Document second = new Document("name", "second");
    private final Document modified = new Document("name", "modified");
    private final Document update = new Document("$set", modified);
    private final List<Document> aggregation = Arrays.asList(
        new Document("$set",
            new Document("name",
                new Document("$replaceAll",
                    new Document(Map.of(
                        "input", "$name",
                        "find", "first",
                        "replacement", "second"))))));
    private final Document emptyDocument = new Document();

    private String collectionName;

    @BeforeEach
    public void createCollectionName() {
        collectionName = COLLECTION_NAME_1 + System.nanoTime();
    }

     @Test
     public void testUpdateWhenNoDocumentFound() {
         int updated = new UpdateManyStatement(collectionName, emptyDocument, update)
                 .update(database);
         assertThat(updated).isEqualTo(0);
     }

    @Test
    public void testUpdateWhenDocumentFound() {

        new InsertOneStatement(collectionName, first).execute(database);

        int updated = new UpdateManyStatement(collectionName, emptyDocument, update)
                .update(database);
        assertThat(updated).isEqualTo(1);

        final FindIterable<Document> docs = mongoDatabase.getCollection(collectionName).find();
        assertThat(docs).hasSize(1);
        assertThat(docs.iterator().next())
                .containsEntry("name", "modified");
    }

    @Test
    public void testUpdateWithMatchingFilter() {

        new InsertOneStatement(collectionName, first).execute(database);
        new InsertOneStatement(collectionName, second).execute(database);

        int updated = new UpdateManyStatement(collectionName, second, update)
                .update(database);
        assertThat(updated).isEqualTo(1);

        final FindIterable<Document> docs = mongoDatabase.getCollection(collectionName).find(modified);
        assertThat(docs).hasSize(1);
        assertThat(docs.iterator().next())
                .containsEntry("name", "modified");
    }

    @Test
    public void testUpdateWhenMultipleDocumentsFound() {

        new InsertOneStatement(collectionName, second).execute(database);
        new InsertOneStatement(collectionName, second).execute(database);

        int updated = new UpdateManyStatement(collectionName, second, update)
                .update(database);
        assertThat(updated).isEqualTo(2);

        final FindIterable<Document> docs = mongoDatabase.getCollection(collectionName).find(modified);
        assertThat(docs).hasSize(2);
        assertThat(docs).allMatch(doc -> doc.getString("name").equals("modified"));
    }

    @Test
    public void testUpdateWithAggregation() {

        new InsertOneStatement(collectionName, first).execute(database);

        int updated = new UpdateManyStatement(collectionName, first, aggregation)
            .update(database);
        assertThat(updated).isEqualTo(1);

        final FindIterable<Document> docs = mongoDatabase.getCollection(collectionName).find(second);
        assertThat(docs).hasSize(1);
        assertThat(docs.iterator().next())
                .containsEntry("name", "second");
    }

    @Test
    void toStringJs() {
        final UpdateManyStatement statement = new UpdateManyStatement(COLLECTION_NAME_1, first, update);
        assertThat(statement.toJs())
            .isEqualTo(statement.toString())
            .isEqualTo("db.collectionName.updateMany(" +
                "{\"name\": \"first\"}, " +
                "{\"$set\": {\"name\": \"modified\"}});");
    }
}
