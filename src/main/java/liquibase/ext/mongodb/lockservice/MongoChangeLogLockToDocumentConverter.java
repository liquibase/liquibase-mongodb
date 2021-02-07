package liquibase.ext.mongodb.lockservice;

import liquibase.ContextExpression;
import liquibase.Labels;
import liquibase.nosql.changelog.AbstractNoSqlItemToDocumentConverter;
import org.bson.Document;

import java.util.Collection;
import java.util.Date;

import static liquibase.sqlgenerator.core.MarkChangeSetRanGenerator.*;

public class MongoChangeLogLockToDocumentConverter extends AbstractNoSqlItemToDocumentConverter<MongoChangeLogLock, Document> {

    @Override
    public Document toDocument(final MongoChangeLogLock item) {

        return new Document()
                .append(MongoChangeLogLock.Fields.id, item.getId())
                .append(MongoChangeLogLock.Fields.lockGranted, item.getLockGranted())
                .append(MongoChangeLogLock.Fields.lockedBy, item.getLockedBy())
                .append(MongoChangeLogLock.Fields.locked, item.getLocked());
    }

    @Override
    public MongoChangeLogLock fromDocument(final Document document) {

        return new MongoChangeLogLock(
                document.get(MongoChangeLogLock.Fields.id, Integer.class)
                , document.get(MongoChangeLogLock.Fields.lockGranted, Date.class)
                , document.get(MongoChangeLogLock.Fields.lockedBy, String.class)
                , document.get(MongoChangeLogLock.Fields.locked, Boolean.class)
        );
    }

    /**
     * TODO: raise with liquibase to move into {@link Labels} class
     */
    public String buildLabels(Labels labels) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }
        return labels.toString();
    }

    /**
     * TODO: raise with liquibase to move into {@link ContextExpression} class
     */
    public String buildFullContext(final ContextExpression contextExpression, final Collection<ContextExpression> inheritableContexts) {
        if ((contextExpression == null) || contextExpression.isEmpty()) {
            return null;
        }

        StringBuilder contextExpressionString = new StringBuilder();
        boolean notFirstContext = false;
        for (ContextExpression inheritableContext : inheritableContexts) {
            appendContext(contextExpressionString, inheritableContext.toString(), notFirstContext);
            notFirstContext = true;
        }
        appendContext(contextExpressionString, contextExpression.toString(), notFirstContext);

        return contextExpressionString.toString();
    }

    /**
     * TODO: raise with liquibase to move into {@link ContextExpression} class
     */
    private void appendContext(StringBuilder contextExpression, String contextToAppend, boolean notFirstContext) {
        boolean complexExpression = contextToAppend.contains(COMMA) || contextToAppend.contains(WHITESPACE);
        if (notFirstContext) {
            contextExpression.append(AND);
        }
        if (complexExpression) {
            contextExpression.append(OPEN_BRACKET);
        }
        contextExpression.append(contextToAppend);
        if (complexExpression) {
            contextExpression.append(CLOSE_BRACKET);
        }
    }
}
