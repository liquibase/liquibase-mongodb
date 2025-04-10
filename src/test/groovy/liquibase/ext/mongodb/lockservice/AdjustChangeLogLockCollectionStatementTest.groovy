package liquibase.ext.mongodb.lockservice

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import org.bson.Document
import spock.lang.Specification

class AdjustChangeLogLockCollectionStatementTest extends Specification {
    
    def "should execute adjust change log lock collection statement"() {
        given:
        def collectionName = "DATABASECHANGELOGLOCK"
        def statement = new AdjustChangeLogLockCollectionStatement(collectionName)
        
        def database = Mock(MongoLiquibaseDatabase)
        def mongoDatabase = Mock(MongoDatabase)
        def mongoCollection = Mock(MongoCollection)
        
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.getCollection(collectionName) >> mongoCollection
        
        when:
        statement.execute(database)
        
        then:
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("id") })
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("locked") })
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("lockGranted") })
    }
    
    def "should have correct command name"() {
        expect:
        def statement = new AdjustChangeLogLockCollectionStatement("DATABASECHANGELOGLOCK")
        statement.getCommandName() == "adjustDatabaseChangelogLockCollection"
    }
    
    def "should build correct command document"() {
        given:
        def collectionName = "DATABASECHANGELOGLOCK"
        def statement = new AdjustChangeLogLockCollectionStatement(collectionName)
        
        when:
        def command = statement.getCommand()
        
        then:
        command instanceof Document
        command.getString("adjustDatabaseChangelogLockCollection") == collectionName
    }
}