package liquibase.ext.mongodb.change

import liquibase.ChecksumVersion
import liquibase.database.Database
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.InsertOneStatement
import spock.lang.Specification

class InsertOneChangeTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    
    def "should generate InsertOneStatement"() {
        given:
        def change = new InsertOneChange()
        change.setCollectionName("testCollection")
        change.setDocument('{"name": "John", "age": 30}')
        change.setOptions('{"bypassDocumentValidation": true}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof InsertOneStatement
        def statement = (InsertOneStatement) statements[0]
        statement.getCommand().getString("insert") == "testCollection"
        statement.getCommand().getBoolean("bypassDocumentValidation")
        statement.getCommand().get("documents") instanceof List
        statement.getCommand().getList("documents", Object.class).size() == 1
        statement.getCommand().getList("documents", Object.class)[0].get("name") == "John"
        statement.getCommand().getList("documents", Object.class)[0].get("age") == 30
    }
    
    def "should generate statement without options"() {
        given:
        def change = new InsertOneChange()
        change.setCollectionName("testCollection")
        change.setDocument('{"name": "John", "age": 30}')
        change.setOptions(null)
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof InsertOneStatement
        def statement = (InsertOneStatement) statements[0]
        statement.getCommand().getString("insert") == "testCollection"
        statement.getCommand().get("documents") instanceof List
        statement.getCommand().getList("documents", Object.class).size() == 1
        statement.getCommand().getList("documents", Object.class)[0].get("name") == "John"
    }
    
    def "should provide confirmation message"() {
        given:
        def change = new InsertOneChange()
        change.setCollectionName("testCollection")
        
        when:
        def message = change.getConfirmationMessage()
        
        then:
        message == "Document inserted into collection testCollection"
    }
    
    def "should generate consistent checksum"() {
        given:
        def change1 = new InsertOneChange()
        change1.setCollectionName("testCollection")
        change1.setDocument('{"name": "John", "age": 30}')
        change1.setOptions('{"bypassDocumentValidation": true}')
        
        def change2 = new InsertOneChange()
        change2.setCollectionName("testCollection")
        change2.setDocument('{"name": "John", "age": 30}')
        change2.setOptions('{"bypassDocumentValidation": true}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 == checksum2
    }
    
    def "should generate different checksums for different values"() {
        given:
        def change1 = new InsertOneChange()
        change1.setCollectionName("testCollection")
        change1.setDocument('{"name": "John", "age": 30}')
        
        def change2 = new InsertOneChange()
        change2.setCollectionName("testCollection")
        change2.setDocument('{"name": "Jane", "age": 25}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 != checksum2
    }
    
    def "should extend AbstractMongoChange"() {
        expect:
        new InsertOneChange() instanceof AbstractMongoChange
    }
}