package liquibase.ext.mongodb.database;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.util.StringUtil;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Properties;
import java.util.logging.Logger;

public class MongoClientDriver implements Driver {

    @Override
    public Connection connect(final String url, final Properties info) {
        //Not applicable for non JDBC DBs
        throw new UnsupportedOperationException("Cannot initiate a SQL Connection for a NoSql DB");
    }

    public MongoClient connect(final ConnectionString connectionString) throws DatabaseException {
        final MongoClient client;
        try {
           client = MongoClients.create(connectionString);
        } catch (final Exception e) {
            throw new DatabaseException("Connection could not be established to: "
                    + connectionString.getConnectionString(), e);
        }
        return client;
    }

    @Override
    public boolean acceptsURL(final String url) {
        final String trimmedUrl = StringUtil.trimToEmpty(url);
        return trimmedUrl.startsWith(MongoConnection.MONGO_DNS_PREFIX) || trimmedUrl.startsWith(MongoConnection.MONGO_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() {
        return (Logger) Scope.getCurrentScope().getLog(getClass());
    }
}
