package liquibase.ext.mongodb;

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

import liquibase.Liquibase;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.LiquibaseException;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.bson.Document;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class TestUtils {
    public static final String BUILD_INFO_1 = "{ buildInfo: 1 }";
    public static final String COLLECTION_NAME_1 = "collectionName";
    public static final String EMPTY_STRING = "";
    public static final String EMPTY_OPTION = EMPTY_STRING;
    public static final String OPTION_1 = "{\"opt1\":\"option 1\"}";
    public static final String DB_CONNECTION_PATH = "url";
    public static final String PROPERTY_FILE = "liquibase.properties";
    private static final String CMD_DROP_ALL_ROLES = "{dropAllRolesFromDatabase: 1, writeConcern: { w: \"majority\" }}";
    private static final String CMD_DROP_ALL_USERS = "{dropAllUsersFromDatabase: 1, writeConcern: { w: \"majority\" }}";
    //private static final String CMD_GET_ALL_ROLES = "{getAllRolesFromDatabase: 1,  showPrivileges:false, showBuiltinRoles: false }}";

    public static List<String> getCollections(final MongoConnection connection) {
        return StreamSupport.stream(connection.getDatabase().listCollectionNames().spliterator(), false)
            .collect(Collectors.toList());
    }

    @SneakyThrows
    public static Properties loadProperties(final String propertyFile) {
        final Properties properties = new Properties();
        properties.load(TestUtils.class.getClassLoader().getResourceAsStream(propertyFile));
        return properties;
    }

    public static String loadProperty(final String propertyFile, final String propertyName) {
        final Properties properties = loadProperties(propertyFile);
        return properties.getProperty(propertyName);
    }

    public static void dropAllRoles(final MongoConnection connection) {
        connection.getDatabase().runCommand(Document.parse(CMD_DROP_ALL_ROLES));
    }

    public static List<Document> getAllRoles(final MongoConnection connection) {
        return connection.getDatabase()
            .runCommand(Document.parse("{ rolesInfo: 1, showPrivileges:false, showBuiltinRoles: false }"))
            .getList("roles", Document.class);
    }

    public static void dropAllUsers(final MongoConnection connection) {
        connection.getDatabase().runCommand(Document.parse(CMD_DROP_ALL_USERS));
    }

    public static void liquibaseUpdate(final Liquibase liquibase) throws LiquibaseException {
        liquibase.update("{}");
    }

    public static List<ChangeSet> getChangesets(final String changeSetPath, final MongoLiquibaseDatabase database) throws LiquibaseException {
        final ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
        final ChangeLogParser parser =
            ChangeLogParserFactory.getInstance().getParser(
                    //FilenameUtils.getExtension(changeSetPath), resourceAccessor
                    changeSetPath, resourceAccessor
                    );

        final DatabaseChangeLog changeLog =
            parser.parse(changeSetPath, new ChangeLogParameters(database), resourceAccessor);
        return changeLog.getChangeSets();
    }
}
