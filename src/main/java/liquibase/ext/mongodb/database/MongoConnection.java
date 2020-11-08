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

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

@Getter
@Setter
@NoArgsConstructor
public class MongoConnection extends AbstractNoSqlConnection {

    public static final int DEFAULT_PORT = 27017;
    public static final String MONGO_PREFIX = MongoLiquibaseDatabase.MONGODB_PRODUCT_SHORT_NAME + "://";
    public static final String MONGO_CONNECTION_STRING_PATTERN = "%s/%s";

    private ConnectionString connectionString;

    protected MongoClient client;

    protected MongoDatabase database;

    @Override
    public String getCatalog() throws DatabaseException {
        try {
            return database.getName();
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
    public void open(final String url, final Driver driverObject, final Properties driverProperties) throws DatabaseException {

        try {

            String urlWithCredentials = url;

            if (nonNull(driverProperties)) {

                final Optional<String> user = Optional.ofNullable(StringUtil.trimToNull(driverProperties.getProperty("user")));
                final Optional<String> password = Optional.ofNullable(StringUtil.trimToNull(driverProperties.getProperty("password")));

                if (user.isPresent() && password.isPresent()) {
                    // injects credentials
                    // mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database.collection][?options]]
                    urlWithCredentials = MONGO_PREFIX + user.get() + ":" + password.get() + "@" + StringUtil.trimToEmpty(url).replaceFirst(MONGO_PREFIX, "");
                }
            }

            this.connectionString = new ConnectionString(urlWithCredentials);

            this.client = ((MongoClientDriver) driverObject).connect(connectionString);

            this.database = this.client.getDatabase(Objects.requireNonNull(this.connectionString.getDatabase()))
                    .withCodecRegistry(BsonUtils.uuidCodecRegistry());
        } catch (final Exception e) {
            throw new DatabaseException("Could not open connection to database: "
                    + ofNullable(connectionString).map(ConnectionString::getDatabase).orElse("UNKNOWN"), e);
        }
    }

    @Override
    public void close() throws DatabaseException {
        try {
            client.close();
        } catch (final Exception e) {
            throw new DatabaseException(e);
        }
    }


}
