package liquibase.ext.mongodb.statement

import org.bson.Document
import spock.lang.Specification

class CreateCollectionStatementTest extends Specification {
    
    def "should create statement with collection name only"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new CreateCollectionStatement(collectionName)
        
        then:
        statement.getCommand() instanceof Document
        statement.getCommand().getString("create") == collectionName
        statement.getRunCommandName() == "create"
    }
    
    def "should create statement with string options"() {
        given:
        def collectionName = "testCollection"
        def options = '{"capped": true, "size": 1048576, "max": 1000}'
        
        when:
        def statement = new CreateCollectionStatement(collectionName, options)
        
        then:
        statement.getCommand().getString("create") == collectionName
        statement.getCommand().getBoolean("capped")
        statement.getCommand().getInteger("size") == 1048576
        statement.getCommand().getInteger("max") == 1000
    }
    
    def "should create statement with Document options"() {
        given:
        def collectionName = "testCollection"
        def options = new Document("capped", true)
                .append("size", 1048576)
                .append("max", 1000)
        
        when:
        def statement = new CreateCollectionStatement(collectionName, options)
        
        then:
        statement.getCommand().getString("create") == collectionName
        statement.getCommand().getBoolean("capped")
        statement.getCommand().getInteger("size") == 1048576
        statement.getCommand().getInteger("max") == 1000
    }
    
    def "should handle empty options"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new CreateCollectionStatement(collectionName, (String)null)
        
        then:
        statement.getCommand().getString("create") == collectionName
        statement.getCommand().size() == 1
    }
    
    def "should extend AbstractRunCommandStatement"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new CreateCollectionStatement(collectionName)
        
        then:
        statement instanceof AbstractRunCommandStatement
    }
}