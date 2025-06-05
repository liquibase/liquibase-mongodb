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
        
        database.getMongoDatabase() >> mongoDatabase
        database.getSupportsValidator() >> true
        
        when:
        statement.execute(database)
        
        then:
        1 * mongoDatabase.runCommand({ Document doc -> 
            doc.getString("collMod") == collectionName && doc.get("validator") != null 
        }) >> new Document("ok", 1.0)
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
        command.getString("collMod") == collectionName
        command.get("validator") != null
    }
}