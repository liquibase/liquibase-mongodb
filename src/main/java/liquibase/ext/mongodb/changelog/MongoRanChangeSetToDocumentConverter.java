package liquibase.ext.mongodb.changelog;

import liquibase.ContextExpression;
import liquibase.Labels;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeSet;
import liquibase.nosql.changelog.AbstractNoSqlItemToDocumentConverter;
import org.bson.Document;

import java.util.Date;

import static java.util.Optional.ofNullable;

public class MongoRanChangeSetToDocumentConverter extends AbstractNoSqlItemToDocumentConverter<MongoRanChangeSet, Document> {

    @Override
    public Document toDocument(final MongoRanChangeSet item) {

        final Document document = new Document();
        document.append(MongoRanChangeSet.Fields.fileName, item.getChangeLog());
        document.append(MongoRanChangeSet.Fields.changeSetId, item.getId());
        document.append(MongoRanChangeSet.Fields.author, item.getAuthor());
        document.append(MongoRanChangeSet.Fields.md5sum,
                ofNullable(item.getLastCheckSum()).map(CheckSum::toString).orElse(null));
        document.append(MongoRanChangeSet.Fields.dateExecuted,
                ofNullable(item.getDateExecuted()).orElse(null));
        document.append(MongoRanChangeSet.Fields.tag, item.getTag());
        document.append(MongoRanChangeSet.Fields.execType,
                ofNullable(item.getExecType()).map(e -> e.value).orElse(null));
        document.append(MongoRanChangeSet.Fields.description, item.getDescription());
        document.append(MongoRanChangeSet.Fields.comments, item.getComments());
        document.append(MongoRanChangeSet.Fields.contexts, buildFullContext(item.getContextExpression(), item.getInheritableContexts()));
        document.append(MongoRanChangeSet.Fields.labels, buildLabels(item.getLabels()));
        document.append(MongoRanChangeSet.Fields.deploymentId, item.getDeploymentId());
        document.append(MongoRanChangeSet.Fields.orderExecuted, item.getOrderExecuted());
        document.append(MongoRanChangeSet.Fields.liquibase, item.getLiquibaseVersion());

        return document;
    }

    @Override
    public MongoRanChangeSet fromDocument(final Document document) {

        return new MongoRanChangeSet(
                (String) document.get(MongoRanChangeSet.Fields.fileName),
                // Change Set Id which is populated to id POJO field
                (String) document.get(MongoRanChangeSet.Fields.changeSetId),
                (String) document.get(MongoRanChangeSet.Fields.author),
                CheckSum.parse((String) document.get(MongoRanChangeSet.Fields.md5sum)),
                (Date) document.get(MongoRanChangeSet.Fields.dateExecuted),
                (String) document.get(MongoRanChangeSet.Fields.tag),
                ofNullable(document.get(MongoRanChangeSet.Fields.execType))
                        .map(s -> ChangeSet.ExecType.valueOf((String) s)).orElse(null),
                (String) document.get(MongoRanChangeSet.Fields.description),
                (String) document.get(MongoRanChangeSet.Fields.comments),
                new ContextExpression((String)document.get(MongoRanChangeSet.Fields.contexts)),
                // not parsed out
                null,
                new Labels((String)document.get(MongoRanChangeSet.Fields.labels)),
                (String) document.get(MongoRanChangeSet.Fields.deploymentId),
                (Integer) ofNullable(document.get(MongoRanChangeSet.Fields.orderExecuted)).orElse(null),
                (String) document.get(MongoRanChangeSet.Fields.liquibase)
        );
    }
}
