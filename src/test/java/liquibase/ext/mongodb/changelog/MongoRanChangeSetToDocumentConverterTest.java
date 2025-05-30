package liquibase.ext.mongodb.changelog;

import liquibase.ContextExpression;
import liquibase.Labels;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeSet;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class MongoRanChangeSetToDocumentConverterTest {

    protected MongoRanChangeSetToDocumentConverter converter = new MongoRanChangeSetToDocumentConverter();

    @Test
    void toDocument() {
        // Create a MongoRanChangeSet with all fields populated
        final Date dateExecuted = new Date();
        final CheckSum checkSum = CheckSum.parse("8:c3981fa8d26e95d911fe8eaeb6570f2f");
        final MongoRanChangeSet changeSet = new MongoRanChangeSet(
                "liquibase/file.xml",
                "cs4",
                "Alex",
                checkSum,
                dateExecuted,
                "Tags",
                ChangeSet.ExecType.EXECUTED,
                "The Description",
                "The Comments",
                new ContextExpression("context1,context2"),
                Arrays.asList(new ContextExpression("inheritedContext")),
                new Labels("label1,label2"),
                "The Deployment Id",
                100,
                "Liquibase Version"
        );

        // Convert to document
        final Document document = converter.toDocument(changeSet);

        // Verify all fields are correctly mapped
        assertThat(document)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.changeSetId, "cs4")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.author, "Alex")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.fileName, "liquibase/file.xml")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.dateExecuted, dateExecuted)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.orderExecuted, 100)
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.execType, "EXECUTED")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.md5sum, "8:c3981fa8d26e95d911fe8eaeb6570f2f")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.description, "The Description")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.comments, "The Comments")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.tag, "Tags")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.deploymentId, "The Deployment Id")
                .hasFieldOrPropertyWithValue(MongoRanChangeSet.Fields.liquibase, "Liquibase Version");

        // Verify contexts include inherited contexts
        assertThat(document.getString(MongoRanChangeSet.Fields.contexts))
                .contains("inheritedContext")
                .contains("context1")
                .contains("context2");

        // Verify labels
        assertThat(document.getString(MongoRanChangeSet.Fields.labels))
                .isEqualTo("label1,label2");
    }

    @Test
    void fromDocument() {

        // Maximum
        final Date dateExecuted = new Date();
        final Document maximal = new Document()
                .append(MongoRanChangeSet.Fields.changeSetId, "cs4")
                .append(MongoRanChangeSet.Fields.author, "Alex")
                .append(MongoRanChangeSet.Fields.fileName, "liquibase/file.xml")
                .append(MongoRanChangeSet.Fields.dateExecuted, dateExecuted)
                .append(MongoRanChangeSet.Fields.orderExecuted, 100)
                .append(MongoRanChangeSet.Fields.execType, "EXECUTED")
                .append(MongoRanChangeSet.Fields.md5sum, "8:c3981fa8d26e95d911fe8eaeb6570f2f")
                .append(MongoRanChangeSet.Fields.description, "The Description")
                .append(MongoRanChangeSet.Fields.comments, "The Comments")
                .append(MongoRanChangeSet.Fields.tag, "Tags")
                .append(MongoRanChangeSet.Fields.contexts, "context1,context2")
                .append(MongoRanChangeSet.Fields.labels, "label1,label2")
                .append(MongoRanChangeSet.Fields.deploymentId, "The Deployment Id")
                .append(MongoRanChangeSet.Fields.liquibase, "Liquibase Version");

        assertThat(converter.fromDocument(maximal))
                .isInstanceOf(MongoRanChangeSet.class)
                .returns("cs4", MongoRanChangeSet::getId)
                .returns("Alex", MongoRanChangeSet::getAuthor)
                .returns("liquibase/file.xml", MongoRanChangeSet::getChangeLog)
                .returns(dateExecuted, MongoRanChangeSet::getDateExecuted)
                .returns(100, MongoRanChangeSet::getOrderExecuted)
                .returns(ChangeSet.ExecType.EXECUTED, MongoRanChangeSet::getExecType)
                .returns(CheckSum.parse("8:c3981fa8d26e95d911fe8eaeb6570f2f"), MongoRanChangeSet::getLastCheckSum)
                .returns( "The Description", MongoRanChangeSet::getDescription)
                .returns( "The Comments", MongoRanChangeSet::getComments)
                .returns("Tags", MongoRanChangeSet::getTag)
                .returns(true, c-> c.getContextExpression().getContexts().containsAll(Arrays.asList("context1","context2")))
                .returns(true, c-> c.getLabels().getLabels().containsAll(Arrays.asList("label1", "label2")))
                .returns("The Deployment Id", MongoRanChangeSet::getDeploymentId)
                .returns("Liquibase Version", MongoRanChangeSet::getLiquibaseVersion);

        // Empty Document
        assertThat(converter.fromDocument(new Document()))
                .isInstanceOf(MongoRanChangeSet.class)
                .hasAllNullFieldsOrPropertiesExcept("contextExpression", "labels");
    }
}