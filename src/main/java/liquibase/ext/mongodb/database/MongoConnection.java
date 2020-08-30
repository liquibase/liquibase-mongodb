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
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import lombok.Getter;
import lombok.Setter;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.UuidCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.sql.Driver;
import java.util.Objects;
import java.util.Properties;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

@Getter
@Setter
public class MongoConnection implements DatabaseConnection {

    public final String MONGO_CONNECTION_STRING_PATTERN = "%s/%s";

    private MongoClient con;
    private com.mongodb.client.MongoDatabase db;
    private ConnectionString connectionString;

    public MongoConnection() {
        con = null;
        db = null;
        connectionString = null;
    }

    public MongoConnection(final String connectionString) {
        initialize(connectionString);
    }

    private void initialize(String connectionString) {
        this.connectionString = new ConnectionString(connectionString);
        this.con = MongoClients.create(this.connectionString);
        this.db = this.con.getDatabase(Objects.requireNonNull(this.connectionString.getDatabase()))
                .withCodecRegistry(uuidCodecRegistry());
    }

    public static CodecRegistry uuidCodecRegistry() {
        return CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD))
                //, fromProviders(PojoCodecProvider.builder().automatic(true).build())
                , MongoClientSettings.getDefaultCodecRegistry()
        );
    }

    public static CodecRegistry pojoCodecRegistry() {
        return CodecRegistries.fromRegistries(
                fromProviders(new UuidCodecProvider(UuidRepresentation.STANDARD))
                , fromProviders(PojoCodecProvider.builder().automatic(true).build())
                , MongoClientSettings.getDefaultCodecRegistry()
        );
    }

    public void close() throws DatabaseException {
        try {
            con.close();
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    public void commit() throws DatabaseException {
        //TODO: implementation
    }

    public boolean getAutoCommit() throws DatabaseException {
        return false;
    }

    public void setAutoCommit(boolean autoCommit) throws DatabaseException {
        //TODO: implementation
    }

    @Override
    public String getCatalog() throws DatabaseException {
        return this.connectionString.getDatabase();
    }

    public String nativeSQL(String sql) throws DatabaseException {
        return null;
    }

    public void rollback() throws DatabaseException {
        //TODO: implementation
    }

    public String getDatabaseProductName() throws DatabaseException {
        //TODO: refer to metadata

        return MongoLiquibaseDatabase.MONGODB_PRODUCT_NAME;
    }

    public String getDatabaseProductVersion() throws DatabaseException {
        return null;
    }

    public int getDatabaseMajorVersion() throws DatabaseException {
        return 0;
    }

    public int getDatabaseMinorVersion() throws DatabaseException {
        return 0;
    }

    @Override
    public String getURL() {
        return String.join(",", this.connectionString.getHosts());
    }

    public String getConnectionUserName() {
        return null;
    }

    public boolean isClosed() throws DatabaseException {
        return false;
    }

    public void attached(Database database) {
        //TODO: implementation
    }

	@Override
	public int getPriority() {
		// If this is set to low, liquibase cli won't work.
		return 102;
	}

	@Override
	public void open(String url, Driver driverObject, Properties driverProperties) throws DatabaseException {
		this.initialize(url);
	}

}
