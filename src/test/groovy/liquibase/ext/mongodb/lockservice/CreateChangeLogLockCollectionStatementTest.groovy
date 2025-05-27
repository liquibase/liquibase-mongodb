package liquibase.ext.mongodb.lockservice

import com.mongodb.client.MongoDatabase
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import org.bson.Document
import spock.lang.Specification

class CreateChangeLogLockCollectionStatementTest extends Specification {
    
    def "should execute create change log lock collection statement"() {
        given:
        def collectionName = "DATABASECHANGELOGLOCK"
        def statement = new CreateChangeLogLockCollectionStatement(collectionName)
        
        def database = Mock(MongoLiquibaseDatabase)
        def mongoDatabase = Mock(MongoDatabase)
        
        database.getMongoDatabase() >> mongoDatabase
        
        when:
        statement.execute(database)
        
        then:
        1 * mongoDatabase.runCommand({ Document cmd ->
            cmd.getString("create") == collectionName
        }) >> new Document("ok", 1)
    }
    
    def "should have correct command name"() {
        expect:
        def statement = new CreateChangeLogLockCollectionStatement("DATABASECHANGELOGLOCK")
        statement.getCommandName() == "runCommand"
        statement.getRunCommandName() == "create"
    }
    
    def "should build correct command document"() {
        given:
        def collectionName = "DATABASECHANGELOGLOCK"
        def statement = new CreateChangeLogLockCollectionStatement(collectionName)
        
        when:
        def command = statement.getCommand()
        
        then:
        command instanceof Document
        command.getString("create") == collectionName
    }
}