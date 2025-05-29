package liquibase.ext.mongodb.changelog

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import org.bson.Document
import spock.lang.Specification

class CreateChangeLogCollectionStatementTest extends Specification {
    
    def "should execute create change log collection statement"() {
        given:
        def collectionName = "DATABASECHANGELOG"
        def statement = new CreateChangeLogCollectionStatement(collectionName)
        
        def database = Mock(MongoLiquibaseDatabase)
        def mongoDatabase = Mock(MongoDatabase)
        
        database.getMongoDatabase() >> mongoDatabase
        
        when:
        statement.execute(database)
        
        then:
        1 * mongoDatabase.runCommand({ Document doc -> 
            doc.getString("create") == collectionName
        }) >> new Document("ok", 1.0d)
    }
    
    def "should have correct command name"() {
        expect:
        def statement = new CreateChangeLogCollectionStatement("DATABASECHANGELOG")
        statement.getCommandName() == "createDatabaseChangelogCollection"
    }
    
    def "should build correct command document"() {
        given:
        def collectionName = "DATABASECHANGELOG"
        def statement = new CreateChangeLogCollectionStatement(collectionName)
        
        when:
        def command = statement.getCommand()
        
        then:
        command instanceof Document
        command.getString("create") == collectionName
    }
}