package liquibase.ext.mongodb.changelog;

import liquibase.ContextExpression;
import liquibase.Labels;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
public class MongoRanChangeSet extends RanChangeSet {

    public static class Fields {
        public static final String fileName = "fileName";
        public static final String changeSetId = "id";
        public static final String author = "author";
        public static final String md5sum = "md5sum";
        public static final String dateExecuted = "dateExecuted";
        public static final String tag = "tag";
        public static final String execType = "execType";
        public static final String description = "description";
        public static final String comments = "comments";
        public static final String contexts = "contexts";
        public static final String labels = "labels";
        public static final String deploymentId = "deploymentId";
        public static final String orderExecuted = "orderExecuted";
        public static final String liquibase = "liquibase";
    }

    @Getter
    @Setter
    private Collection<ContextExpression> inheritableContexts;

    public MongoRanChangeSet(final String changeLog, final String id, final String author, final CheckSum lastCheckSum, final Date dateExecuted
            , final String tag, final ChangeSet.ExecType execType, final String description, final String comments, final ContextExpression contextExpression, final Collection<ContextExpression> inheritableContexts
            , final Labels labels, final String deploymentId, final Integer orderExecuted, final String liquibase) {
        super(changeLog, id, author, lastCheckSum, dateExecuted, tag, execType, description, comments, contextExpression, labels, deploymentId, changeLog);
        super.setOrderExecuted(orderExecuted);
        super.setLiquibaseVersion(liquibase);
        this.inheritableContexts = inheritableContexts;
    }

    public MongoRanChangeSet(final ChangeSet changeSet, final ChangeSet.ExecType execType, final ContextExpression contextExpression, final Labels labels) {
        super(changeSet, execType, contextExpression, labels);
    }
}
