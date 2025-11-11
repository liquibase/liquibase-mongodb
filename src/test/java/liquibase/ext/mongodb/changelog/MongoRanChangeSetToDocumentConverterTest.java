package liquibase.ext.mongodb.changelog;

import liquibase.ContextExpression;
import liquibase.Labels;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeSet;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class MongoRanChangeSetToDocumentConverterTest {

    protected MongoRanChangeSetToDocumentConverter converter = new MongoRanChangeSetToDocumentConverter();

    @Test
    void toDocument() {
        final ChangeSet changeSet = new ChangeSet(
                "1"
                , "author"
                , false
                , false
                , "fileName"
                , null
                , null
                , null
        );
        final ContextExpression contextExpression = new ContextExpression("context1", "context2");
        final ContextExpression inheritableContextExpression = new ContextExpression("inhContext1", "inhContext2");
        final Labels labels = new Labels("label1", "label2");

        final MongoRanChangeSet mongoRanChangeSet = new MongoRanChangeSet(changeSet, ChangeSet.ExecType.EXECUTED, contextExpression, labels);
        mongoRanChangeSet.setInheritableContexts(Collections.singletonList(inheritableContextExpression));

        final Document result = converter.toDocument(mongoRanChangeSet);

        assertThat(result)
                .isNotEmpty()
                .satisfies(document -> {
                    assertThat(document).containsEntry("id", mongoRanChangeSet.getId());
                    assertThat(document).containsEntry("author", mongoRanChangeSet.getAuthor());
                    assertThat(document).containsEntry("fileName", mongoRanChangeSet.getChangeLog());
                    assertThat(document).containsEntry("md5sum", mongoRanChangeSet.getLastCheckSum().toString());
                    assertThat(document).containsEntry("dateExecuted", mongoRanChangeSet.getDateExecuted());
                    assertThat(document).containsEntry("execType", mongoRanChangeSet.getExecType().value);
                    assertThat(document).containsEntry("labels", mongoRanChangeSet.getLabels().toString());
                    assertThat(document).containsEntry("contexts", "(inhContext1,inhContext2) AND (context1,context2)");
                });
    }

    @Test
    void fromDocument() {

        // Maximum
        final Date dateExecuted = new Date();
        final Document maximal = new Document()
                .append("id", "cs4")
                .append("author", "Alex")
                .append("fileName", "liquibase/file.xml")
                .append("dateExecuted", dateExecuted)
                .append("orderExecuted", 100)
                .append("execType", "EXECUTED")
                .append("md5sum", "9:c3981fa8d26e95d911fe8eaeb6570f2f")
                .append("description", "The Description")
                .append("comments", "The Comments")
                .append("tag", "Tags")
                .append("contexts", "context1,context2")
                .append("labels", "label1,label2")
                .append("deploymentId", "The Deployment Id")
                .append("liquibase", "Liquibase Version");

        assertThat(converter.fromDocument(maximal))
                .isInstanceOf(MongoRanChangeSet.class)
                .returns("cs4", MongoRanChangeSet::getId)
                .returns("Alex", MongoRanChangeSet::getAuthor)
                .returns("liquibase/file.xml", MongoRanChangeSet::getChangeLog)
                .returns("liquibase/file.xml", MongoRanChangeSet::getStoredChangeLog)
                .returns(dateExecuted, MongoRanChangeSet::getDateExecuted)
                .returns(100, MongoRanChangeSet::getOrderExecuted)
                .returns(ChangeSet.ExecType.EXECUTED, MongoRanChangeSet::getExecType)
                .returns(CheckSum.compute("QWERTY"), MongoRanChangeSet::getLastCheckSum)
                .returns("The Description", MongoRanChangeSet::getDescription)
                .returns("The Comments", MongoRanChangeSet::getComments)
                .returns("Tags", MongoRanChangeSet::getTag)
                .returns(true, c -> c.getContextExpression().getContexts().containsAll(Arrays.asList("context1", "context2")))
                .returns(true, c -> c.getLabels().getLabels().containsAll(Arrays.asList("label1", "label2")))
                .returns("The Deployment Id", MongoRanChangeSet::getDeploymentId)
                .returns("Liquibase Version", MongoRanChangeSet::getLiquibaseVersion);

        // Empty Document
        assertThat(converter.fromDocument(new Document()))
                .isInstanceOf(MongoRanChangeSet.class)
                .hasAllNullFieldsOrPropertiesExcept("contextExpression", "labels");
    }
}