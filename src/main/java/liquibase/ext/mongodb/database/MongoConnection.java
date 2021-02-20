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

import com.mongodb.ConnectionString;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import liquibase.exception.DatabaseException;
import liquibase.ext.mongodb.statement.BsonUtils;
import liquibase.nosql.database.AbstractNoSqlConnection;
import liquibase.util.StringUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Driver;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static liquibase.ext.mongodb.database.MongoLiquibaseDatabase.MONGODB_PRODUCT_SHORT_NAME;

@Getter
@Setter
@NoArgsConstructor
public class MongoConnection extends AbstractNoSqlConnection {

    public static final int DEFAULT_PORT = 27017;
    public static final String MONGO_PREFIX = MONGODB_PRODUCT_SHORT_NAME + "://";
    public static final String MONGO_DNS_PREFIX = MONGODB_PRODUCT_SHORT_NAME + "+srv://";

    private ConnectionString connectionString;

    protected MongoClient mongoClient;

    protected MongoDatabase mongoDatabase;

    @Override
    public String getCatalog() throws DatabaseException {
        try {
            return mongoDatabase.getName();
        } catch (final Exception e) {
            throw new DatabaseException(e);
        }
    }

    public String getDatabaseProductName() throws DatabaseException {
        return MongoLiquibaseDatabase.MONGODB_PRODUCT_NAME;
    }

    @Override
    public String getURL() {
        return String.join(",", ofNullable(this.connectionString).map(ConnectionString::getHosts).orElse(Collections.emptyList()));
    }

    @Override
    public String getConnectionUserName() {
        return ofNullable(this.connectionString).map(ConnectionString::getCredential)
                .map(MongoCredential::getUserName).orElse("");
    }

    @Override
    public boolean isClosed() throws DatabaseException {
        return isNull(mongoClient);
    }

    @Override
    public void open(final String url, final Driver driverObject, final Properties driverProperties) throws DatabaseException {

        try {
            final String urlWithCredentials = injectCredentials(StringUtil.trimToEmpty(url), driverProperties);

            this.connectionString = new ConnectionString(urlWithCredentials);

            this.mongoClient = ((MongoClientDriver) driverObject).connect(connectionString);

            this.mongoDatabase = this.mongoClient.getDatabase(Objects.requireNonNull(this.connectionString.getDatabase()))
                    .withCodecRegistry(BsonUtils.uuidCodecRegistry());
        } catch (final Exception e) {
            throw new DatabaseException("Could not open connection to database: "
                    + ofNullable(connectionString).map(ConnectionString::getDatabase).orElse("UNKNOWN"), e);
        }
    }

    private String injectCredentials(final String url, final Properties driverProperties) {

        if (nonNull(driverProperties)) {

            final Optional<String> user = Optional.ofNullable(StringUtil.trimToNull(driverProperties.getProperty("user")));
            final Optional<String> password = Optional.ofNullable(StringUtil.trimToNull(driverProperties.getProperty("password")));

            if (user.isPresent()) {
                // injects credentials
                // mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database.collection][?options]]
                // mongodb+srv://[username:password@]host[:port1][?options]]
                final String mongoPrefix = url.startsWith(MONGO_DNS_PREFIX) ? MONGO_DNS_PREFIX : MONGO_PREFIX;
                return mongoPrefix + user.get() + password.map(p -> ":" + p).orElse("") + "@" +
                        url.substring(mongoPrefix.length());
            }
        }
        return url;
    }


    @Override
    public void close() throws DatabaseException {
        try {
            if (!isClosed()) {
                mongoClient.close();
                mongoClient = null;
            }
        } catch (final Exception e) {
            throw new DatabaseException(e);
        }
    }


}
