package liquibase.ext.mongodb.change;

import liquibase.change.ChangeMetaData;
import liquibase.change.CheckSum;
import liquibase.change.DatabaseChange;
import liquibase.database.Database;
import liquibase.ext.mongodb.statement.UpdateManyStatement;
import liquibase.statement.SqlStatement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseChange(name = "updateMany",
        description = "Updates all documents that match the specified filter for a collection " +
                "https://www.mongodb.com/docs/manual/reference/method/db.collection.updateMany",
        priority = ChangeMetaData.PRIORITY_DEFAULT, appliesTo = "collection")
@NoArgsConstructor
@Getter
@Setter
public class UpdateManyChange extends AbstractMongoChange {

    private String collectionName;
    private String filter;
    private String update;

    @Override
    public String getConfirmationMessage() {
        return "Documents updated in collection " + getCollectionName();
    }

    @Override
    public SqlStatement[] generateStatements(final Database database) {
        return new SqlStatement[]{
                new UpdateManyStatement(collectionName, filter, update)
        };
    }

    @Override
    public CheckSum generateCheckSum() {
        return super.generateCheckSum(collectionName, filter, update);
    }
}
