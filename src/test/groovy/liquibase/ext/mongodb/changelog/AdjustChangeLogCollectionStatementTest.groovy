package liquibase.ext.mongodb.changelog

import com.mongodb.client.ListIndexesIterable
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
        def indexList = Mock(ListIndexesIterable)
        
        database.getMongoDatabase() >> mongoDatabase
        database.getSupportsValidator() >> true
        mongoDatabase.getCollection(collectionName) >> mongoCollection
        mongoDatabase.runCommand(_) >> new Document("ok", 1.0)
        mongoCollection.listIndexes() >> indexList
        indexList.into(_) >> { args -> 
            List list = args[0]
            if (list != null) {
                list.add(new Document("name", "_id_"))
            }
            return list
        }
        
        when:
        statement.execute(database)
        
        then:
        1 * mongoCollection.createIndex({ Document doc -> 
            doc.containsKey("fileName") && doc.containsKey("author") && doc.containsKey("id") 
        }, _)
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
        command.getString("collMod") == collectionName
        command.get("validator") != null
    }
}