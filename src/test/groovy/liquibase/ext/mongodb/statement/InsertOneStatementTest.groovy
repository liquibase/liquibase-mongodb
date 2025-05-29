package liquibase.ext.mongodb.statement

import org.bson.Document
import spock.lang.Specification

class InsertOneStatementTest extends Specification {
    
    def "should create InsertOneStatement with string document"() {
        given:
        def collectionName = "testCollection"
        def documentJson = '{"name": "test", "value": 123}'
        def optionsJson = '{"ordered": true}'
        
        when:
        def statement = new InsertOneStatement(collectionName, documentJson, optionsJson)
        
        then:
        statement.getCommand().getString("insert") == collectionName
        def documents = statement.getCommand().getList("documents", Document.class)
        documents.size() == 1
        documents[0] instanceof Document
        documents[0].getString("name") == "test"
        documents[0].getInteger("value") == 123
        statement.getCommand().getBoolean("ordered") == true
    }
    
    def "should create InsertOneStatement with Document"() {
        given:
        def collectionName = "testCollection"
        def document = new Document("name", "test").append("value", 123)
        def options = new Document("ordered", true)
        
        when:
        def statement = new InsertOneStatement(collectionName, document, options)
        
        then:
        statement.getCommand().getString("insert") == collectionName
        def documents = statement.getCommand().getList("documents", Document.class)
        documents.size() == 1
        documents[0] instanceof Document
        documents[0].getString("name") == "test"
        documents[0].getInteger("value") == 123
        statement.getCommand().getBoolean("ordered") == true
    }
    
    def "should create InsertOneStatement with Document without options"() {
        given:
        def collectionName = "testCollection"
        def document = new Document("name", "test").append("value", 123)
        
        when:
        def statement = new InsertOneStatement(collectionName, document)
        
        then:
        statement.getCommand().getString("insert") == collectionName
        def documents = statement.getCommand().getList("documents", Document.class)
        documents.size() == 1
        documents[0] instanceof Document
        documents[0].getString("name") == "test"
        documents[0].getInteger("value") == 123
    }
    
    def "should handle empty or null documents"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new InsertOneStatement(collectionName, (String)null, null)
        
        then:
        statement.getCommand().getString("insert") == collectionName
        def documents = statement.getCommand().getList("documents", Document.class)
        documents.size() == 1
        documents[0] instanceof Document
        documents[0].isEmpty()
    }
    
    def "should extend InsertManyStatement"() {
        given:
        def collectionName = "testCollection"
        def document = new Document("name", "test")
        
        when:
        def statement = new InsertOneStatement(collectionName, document)
        
        then:
        statement instanceof InsertManyStatement
    }
}