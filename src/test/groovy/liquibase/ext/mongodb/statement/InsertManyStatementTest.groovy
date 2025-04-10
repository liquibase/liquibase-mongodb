package liquibase.ext.mongodb.statement

import org.bson.Document
import spock.lang.Specification

class InsertManyStatementTest extends Specification {
    
    def "should create InsertManyStatement with string documents"() {
        given:
        def collectionName = "testCollection"
        def documentsJson = '[{"name": "test1", "value": 123}, {"name": "test2", "value": 456}]'
        def optionsJson = '{"ordered": true}'
        
        when:
        def statement = new InsertManyStatement(collectionName, documentsJson, optionsJson)
        
        then:
        statement.getCommand().getString("insert") == collectionName
        statement.getCommand().getBoolean("ordered") == true
        statement.getCommand().get("documents") instanceof List
        statement.getCommand().getList("documents", Document.class).size() == 2
        statement.getCommand().getList("documents", Document.class)[0].getString("name") == "test1"
        statement.getCommand().getList("documents", Document.class)[0].getInteger("value") == 123
        statement.getCommand().getList("documents", Document.class)[1].getString("name") == "test2"
        statement.getCommand().getList("documents", Document.class)[1].getInteger("value") == 456
    }
    
    def "should create InsertManyStatement with Document List"() {
        given:
        def collectionName = "testCollection"
        def documents = [
            new Document("name", "test1").append("value", 123),
            new Document("name", "test2").append("value", 456)
        ]
        def options = new Document("ordered", true)
        
        when:
        def statement = new InsertManyStatement(collectionName, documents, options)
        
        then:
        statement.getCommand().getString("insert") == collectionName
        statement.getCommand().getBoolean("ordered") == true
        statement.getCommand().get("documents") instanceof List
        statement.getCommand().getList("documents", Document.class).size() == 2
        statement.getCommand().getList("documents", Document.class)[0].getString("name") == "test1"
        statement.getCommand().getList("documents", Document.class)[0].getInteger("value") == 123
        statement.getCommand().getList("documents", Document.class)[1].getString("name") == "test2"
        statement.getCommand().getList("documents", Document.class)[1].getInteger("value") == 456
    }
    
    def "should create InsertManyStatement with Document List without options"() {
        given:
        def collectionName = "testCollection"
        def documents = [
            new Document("name", "test1").append("value", 123),
            new Document("name", "test2").append("value", 456)
        ]
        
        when:
        def statement = new InsertManyStatement(collectionName, documents)
        
        then:
        statement.getCommand().getString("insert") == collectionName
        statement.getCommand().get("documents") instanceof List
        statement.getCommand().getList("documents", Document.class).size() == 2
        statement.getCommand().getList("documents", Document.class)[0].getString("name") == "test1"
        statement.getCommand().getList("documents", Document.class)[0].getInteger("value") == 123
        statement.getCommand().getList("documents", Document.class)[1].getString("name") == "test2"
        statement.getCommand().getList("documents", Document.class)[1].getInteger("value") == 456
    }
    
    def "should handle empty or null documents"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new InsertManyStatement(collectionName, (String)null, null)
        
        then:
        statement.getCommand().getString("insert") == collectionName
        statement.getCommand().get("documents") instanceof List
        statement.getCommand().getList("documents", Document.class).isEmpty()
    }
    
    def "should extend AbstractRunCommandStatement"() {
        given:
        def collectionName = "testCollection"
        def documents = [new Document("name", "test")]
        
        when:
        def statement = new InsertManyStatement(collectionName, documents)
        
        then:
        statement instanceof AbstractRunCommandStatement
    }
    
    def "should have correct command name"() {
        given:
        def collectionName = "testCollection"
        def documents = [new Document("name", "test")]
        
        when:
        def statement = new InsertManyStatement(collectionName, documents)
        
        then:
        statement.getRunCommandName() == "insert"
    }
}