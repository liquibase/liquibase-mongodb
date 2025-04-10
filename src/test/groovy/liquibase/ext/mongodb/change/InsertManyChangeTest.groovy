package liquibase.ext.mongodb.change

import liquibase.ChecksumVersion
import liquibase.database.Database
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.InsertManyStatement
import spock.lang.Specification

class InsertManyChangeTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    
    def "should generate InsertManyStatement"() {
        given:
        def change = new InsertManyChange()
        change.setCollectionName("testCollection")
        change.setDocuments('[{"name": "John", "age": 30}, {"name": "Jane", "age": 25}]')
        change.setOptions('{"ordered": true}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof InsertManyStatement
        def statement = (InsertManyStatement) statements[0]
        statement.getCommand().getString("insert") == "testCollection"
        statement.getCommand().getBoolean("ordered")
        statement.getCommand().getList("documents", Object.class).size() == 2
    }
    
    def "should generate statement without options"() {
        given:
        def change = new InsertManyChange()
        change.setCollectionName("testCollection")
        change.setDocuments('[{"name": "John", "age": 30}]')
        change.setOptions(null)
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof InsertManyStatement
        def statement = (InsertManyStatement) statements[0]
        statement.getCommand().getString("insert") == "testCollection"
        statement.getCommand().getList("documents", Object.class).size() == 1
    }
    
    def "should provide confirmation message"() {
        given:
        def change = new InsertManyChange()
        change.setCollectionName("testCollection")
        
        when:
        def message = change.getConfirmationMessage()
        
        then:
        message == "Documents inserted into collection testCollection"
    }
    
    def "should generate consistent checksum"() {
        given:
        def change1 = new InsertManyChange()
        change1.setCollectionName("testCollection")
        change1.setDocuments('[{"name": "John", "age": 30}]')
        change1.setOptions('{"ordered": true}')
        
        def change2 = new InsertManyChange()
        change2.setCollectionName("testCollection")
        change2.setDocuments('[{"name": "John", "age": 30}]')
        change2.setOptions('{"ordered": true}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 == checksum2
    }
    
    def "should generate different checksums for different values"() {
        given:
        def change1 = new InsertManyChange()
        change1.setCollectionName("testCollection")
        change1.setDocuments('[{"name": "John", "age": 30}]')
        
        def change2 = new InsertManyChange()
        change2.setCollectionName("testCollection")
        change2.setDocuments('[{"name": "Jane", "age": 25}]')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 != checksum2
    }
    
    def "should extend AbstractMongoChange"() {
        expect:
        new InsertManyChange() instanceof AbstractMongoChange
    }
}