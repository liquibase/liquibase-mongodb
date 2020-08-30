package liquibase.ext.mongodb.database;

/*-
 * #%L
 * Liquibase MongoDB Extension
 * %%
 * Copyright (C) 2019 Mastercard
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import liquibase.CatalogAndSchema;
import liquibase.change.Change;
import liquibase.configuration.ConfigurationProperty;
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.sql.visitor.SqlVisitor;
import liquibase.statement.DatabaseFunction;
import liquibase.statement.SqlStatement;
import liquibase.structure.DatabaseObject;
import lombok.Setter;

import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public class MongoLiquibaseDatabase extends AbstractJdbcDatabase {

    public static final String MONGODB_PRODUCT_NAME = "MongoDB";
    public static final String MONGODB_PRODUCT_SHORT_NAME = "mongodb";
    public static final String DATABASE_CHANGE_LOG_TABLE_NAME = "databaseChangeLog";
    public static final String DATABASE_CHANGE_LOG_LOCK_TABLE_NAME = "databaseChangeLogLock";

    @Setter
    private String databaseChangeLogTableName;

    @Setter
    private String databaseChangeLogLockTableName;

    @Setter
    private String liquibaseCatalogName;
    private MongoConnection connection;

    public MongoLiquibaseDatabase() {
        databaseChangeLogTableName = DATABASE_CHANGE_LOG_TABLE_NAME;
        databaseChangeLogLockTableName = DATABASE_CHANGE_LOG_LOCK_TABLE_NAME;
    }

    @Override
    public MongoConnection getConnection() {
        return connection;
    }

    @Override
    public void setConnection(final DatabaseConnection connection) {
        this.connection = (MongoConnection) connection;
    }

    @Override
    public String getDatabaseChangeLogTableName() {
        if (databaseChangeLogTableName != null) {
            return databaseChangeLogTableName;
        }

        return LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class).getDatabaseChangeLogTableName();
    }

    @Override
    public void setDatabaseChangeLogTableName(final String tableName) {
        this.databaseChangeLogTableName = tableName;
    }

    @Override
    public String getDatabaseChangeLogLockTableName() {
        if (databaseChangeLogLockTableName != null) {
            return databaseChangeLogLockTableName;
        }

        return LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class).getDatabaseChangeLogLockTableName();
    }

    public String getShortName() {
        return MONGODB_PRODUCT_SHORT_NAME;
    }

    @Override
    public String getDefaultCatalogName() {
        return null;
    }

    @Override
    public void setDefaultCatalogName(String catalogName) {
        //TODO: implementation
    }

    @Override
    public String getDefaultSchemaName() {
        return null;
    }

    @Override
    public void setDefaultSchemaName(String schemaName) {
        //TODO: implementation
    }

    @Override
    public Integer getDefaultScaleForNativeDataType(String nativeDataType) {
        return null;
    }

    public Integer getDefaultPort() {
        return 27017;
    }

    @Override
    public Integer getFetchSize() {
        return null;
    }

    @Override
    public String getLiquibaseCatalogName() {
        if (liquibaseCatalogName != null) {
            return liquibaseCatalogName;
        }

        ConfigurationProperty configuration = LiquibaseConfiguration.getInstance().getProperty(GlobalConfiguration.class, GlobalConfiguration.LIQUIBASE_CATALOG_NAME);
        if (configuration.getWasOverridden()) {
            return configuration.getValue(String.class);
        }

        return getDefaultCatalogName();
    }

    public boolean supportsInitiallyDeferrableColumns() {
        return false;
    }

    @Override
    public boolean supportsSequences() {
        return false;
    }

    @Override
    public boolean supportsDropTableCascadeConstraints() {
        return false;
    }

    @Override
    public boolean supportsAutoIncrement() {
        return false;
    }

    @Override
    public String getLineComment() {
        return "//";
    }

    @Override
    public String getAutoIncrementClause(BigInteger startWith, BigInteger incrementBy, String generationType, Boolean defaultOnNull) {
        return null;
    }

    @Override
    public boolean isSystemObject(DatabaseObject example) {
        return false;
    }

    @Override
    public boolean isLiquibaseObject(DatabaseObject object) {
        return false;
    }

    @Override
    public String getViewDefinition(CatalogAndSchema schema, String name) throws DatabaseException {
        return null;
    }

    @Override
    public String escapeObjectName(String catalogName, String schemaName, String objectName, Class<? extends DatabaseObject> objectType) {
        return null;
    }

    @Override
    public String escapeTableName(String catalogName, String schemaName, String tableName) {
        return null;
    }

    @Override
    public String escapeIndexName(String catalogName, String schemaName, String indexName) {
        return null;
    }

    @Override
    public String escapeObjectName(String objectName, Class<? extends DatabaseObject> objectType) {
        return null;
    }

    @Override
    public String escapeColumnName(String catalogName, String schemaName, String tableName, String columnName) {
        return null;
    }

    @Override
    public String escapeColumnName(String catalogName, String schemaName, String tableName, String columnName, boolean quoteNamesThatMayBeFunctions) {
        return null;
    }

    @Override
    public String escapeColumnNameList(String columnNames) {
        return null;
    }

    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    public boolean supportsCatalogs() {
        return false;
    }

    @Override
    public CatalogAndSchema.CatalogAndSchemaCase getSchemaAndCatalogCase() {
        return CatalogAndSchema.CatalogAndSchemaCase.ORIGINAL_CASE;
    }

    @Override
    public boolean supportsSchemas() {
        return false;
    }

    @Override
    public boolean supportsCatalogInObjectName(Class<? extends DatabaseObject> type) {
        return false;
    }

    @Override
    public String generatePrimaryKeyName(String tableName) {
        return null;
    }

    @Override
    public String escapeSequenceName(String catalogName, String schemaName, String sequenceName) {
        return null;
    }

    @Override
    public String escapeViewName(String catalogName, String schemaName, String viewName) {
        return null;
    }

    @Override
    public void commit() throws DatabaseException {
        //TODO: implementation
    }

    @Override
    public void rollback() throws DatabaseException {
        //TODO: implementation
    }

    @Override
    public String escapeStringForDatabase(String string) {
        return null;
    }

    @Override
    public void close() throws DatabaseException {
        //TODO: implementation
    }

    @Override
    public boolean supportsRestrictForeignKeys() {
        return false;
    }

    @Override
    public String escapeConstraintName(String constraintName) {
        return null;
    }

    @Override
    public boolean isSafeToRunUpdate() throws DatabaseException {
        //TODO: Add the check to not be admin, etc.
        return false;
    }

    @Override
    public void saveStatements(Change change, List<SqlVisitor> sqlVisitors, Writer writer) throws IOException {
        //TODO: implementation
    }

    @Override
    public void executeRollbackStatements(Change change, List<SqlVisitor> sqlVisitors) throws LiquibaseException {
        //TODO: implementation
    }

    @Override
    public void executeRollbackStatements(SqlStatement[] statements, List<SqlVisitor> sqlVisitors) throws LiquibaseException {
        //TODO: implementation
    }

    @Override
    public void saveRollbackStatement(Change change, List<SqlVisitor> sqlVisitors, Writer writer) throws IOException, LiquibaseException {
        //TODO: implementation
    }

    @Override
    public List<DatabaseFunction> getDateFunctions() {
        //TODO: proper implementation
        return Collections.emptyList();
    }

    @Override
    public boolean supportsForeignKeyDisable() {
        return false;
    }

    @Override
    public boolean disableForeignKeyChecks() throws DatabaseException {
        return false;
    }

    @Override
    public void enableForeignKeyChecks() throws DatabaseException {
        //TODO: implementation
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }

    @Override
    public boolean isReservedWord(String string) {
        return false;
    }

    @Override
    public String correctObjectName(String name, Class<? extends DatabaseObject> objectType) {
        return null;
    }

    @Override
    public boolean isFunction(String string) {
        return false;
    }

    @Override
    public int getDataTypeMaxParameters(String dataTypeName) {
        return 0;
    }

    @Override
    public CatalogAndSchema getDefaultSchema() {
        return null;
    }

    @Override
    public boolean dataTypeIsNotModifiable(String typeName) {
        return false;
    }

    @Override
    public String generateDatabaseFunctionValue(DatabaseFunction databaseFunction) {
        return null;
    }

    @Override
    public ObjectQuotingStrategy getObjectQuotingStrategy() {
        return null;
    }

    @Override
    public void setObjectQuotingStrategy(ObjectQuotingStrategy quotingStrategy) {
        //TODO: implementation
    }

    @Override
    public boolean createsIndexesForForeignKeys() {
        return false;
    }

    @Override
    public boolean getOutputDefaultSchema() {
        return false;
    }

    @Override
    public void setOutputDefaultSchema(boolean outputDefaultSchema) {
        //TODO: implementation
    }

    @Override
    public boolean isDefaultSchema(String catalog, String schema) {
        return false;
    }

    @Override
    public boolean isDefaultCatalog(String catalog) {
        return false;
    }

    @Override
    public boolean getOutputDefaultCatalog() {
        return false;
    }

    @Override
    public void setOutputDefaultCatalog(boolean outputDefaultCatalog) {
        //TODO: implementation
    }

    @Override
    public boolean supportsPrimaryKeyNames() {
        return false;
    }

    @Override
    public boolean supportsNotNullConstraintNames() {
        return false;
    }

    @Override
    public boolean supportsBatchUpdates() throws DatabaseException {
        return false;
    }

    @Override
    public boolean requiresExplicitNullForColumns() {
        return false;
    }

    @Override
    public String getSystemSchema() {
        return null;
    }

    @Override
    public String escapeDataTypeName(String dataTypeName) {
        return null;
    }

    @Override
    public String unescapeDataTypeName(String dataTypeName) {
        return null;
    }

    @Override
    public String unescapeDataTypeString(String dataTypeString) {
        return null;
    }

    @Override
    public ValidationErrors validate() {
        return null;
    }

    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    public String getDefaultDriver(String url) {
        if (url.startsWith("mongodb://")) {
            return "liquibase.ext.mongodb.database.adapters.MongoDriver";
        }
        return null;
    }

    @Override
    public boolean requiresUsername() {
        return false;
    }

    @Override
    public boolean requiresPassword() {
        return false;
    }

    @Override
    public boolean getAutoCommitMode() {
        return false;
    }

    @Override
    public boolean supportsDDLInTransaction() {
        return false;
    }

    @Override
    public String getDatabaseProductName() {
        return MONGODB_PRODUCT_NAME;
    }

    protected String getDefaultDatabaseProductName() {
        return MONGODB_PRODUCT_NAME;
    }

    @Override
    public String getDatabaseProductVersion() throws DatabaseException {
        return null;
    }

    @Override
    public int getDatabaseMajorVersion() throws DatabaseException {
        return 0;
    }

    @Override
    public int getDatabaseMinorVersion() throws DatabaseException {
        return 0;
    }

    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        // If it looks like a MongoDB, swims like a MongoDB and quacks like a MongoDB,
        return getDatabaseProductName().equals(conn.getDatabaseProductName());
    }

    @Override
    public String toString() {
        return getShortName() + " Database";
    }

}
