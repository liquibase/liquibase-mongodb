package liquibase.ext.mongodb.changelog

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import org.bson.Document
import spock.lang.Specification

class AdjustChangeLogCollectionStatementTest extends Specification {
    
    def "should execute adjust change log collection statement"() {
        given:
        def collectionName = "DATABASECHANGELOG"
        def statement = new AdjustChangeLogCollectionStatement(collectionName)
        
        def database = Mock(MongoLiquibaseDatabase)
        def mongoDatabase = Mock(MongoDatabase)
        def mongoCollection = Mock(MongoCollection)
        
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.getCollection(collectionName) >> mongoCollection
        
        when:
        statement.execute(database)
        
        then:
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("id") && doc.containsKey("author") && doc.containsKey("fileName") })
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("id") })
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("author") })
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("fileName") })
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("dateExecuted") })
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("tag") })
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("execType") })
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("md5sum") })
        1 * mongoCollection.createIndex({ Document doc -> doc.containsKey("orderExecuted") })
    }
    
    def "should have correct command name"() {
        expect:
        def statement = new AdjustChangeLogCollectionStatement("DATABASECHANGELOG")
        statement.getCommandName() == "adjustDatabaseChangelogCollection"
    }
    
    def "should build correct command document"() {
        given:
        def collectionName = "DATABASECHANGELOG"
        def statement = new AdjustChangeLogCollectionStatement(collectionName)
        
        when:
        def command = statement.getCommand()
        
        then:
        command instanceof Document
        command.getString("adjustDatabaseChangelogCollection") == collectionName
    }
}