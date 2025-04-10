package liquibase.ext.mongodb.change

import liquibase.ChecksumVersion
import liquibase.change.Change
import liquibase.database.Database
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.CreateCollectionStatement
import spock.lang.Specification

class CreateCollectionChangeTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    
    def "should generate CreateCollectionStatement"() {
        given:
        def change = new CreateCollectionChange()
        change.setCollectionName("testCollection")
        change.setOptions('{"capped": true, "size": 1000000, "max": 1000}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof CreateCollectionStatement
        def statement = (CreateCollectionStatement) statements[0]
        statement.getCommand().getString("create") == "testCollection"
        statement.getCommand().getBoolean("capped")
        statement.getCommand().getInteger("size") == 1000000
        statement.getCommand().getInteger("max") == 1000
    }
    
    def "should generate statement without options"() {
        given:
        def change = new CreateCollectionChange()
        change.setCollectionName("testCollection")
        change.setOptions(null)
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof CreateCollectionStatement
        def statement = (CreateCollectionStatement) statements[0]
        statement.getCommand().getString("create") == "testCollection"
    }
    
    def "should provide confirmation message"() {
        given:
        def change = new CreateCollectionChange()
        change.setCollectionName("testCollection")
        
        when:
        def message = change.getConfirmationMessage()
        
        then:
        message == "Collection testCollection created"
    }
    
    def "should generate consistent checksum"() {
        given:
        def change1 = new CreateCollectionChange()
        change1.setCollectionName("testCollection")
        change1.setOptions('{"capped": true, "size": 1000000}')
        
        def change2 = new CreateCollectionChange()
        change2.setCollectionName("testCollection")
        change2.setOptions('{"capped": true, "size": 1000000}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 == checksum2
    }
    
    def "should generate different checksums for different values"() {
        given:
        def change1 = new CreateCollectionChange()
        change1.setCollectionName("testCollection1")
        change1.setOptions('{"capped": true}')
        
        def change2 = new CreateCollectionChange()
        change2.setCollectionName("testCollection2")
        change2.setOptions('{"capped": true}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 != checksum2
    }
    
    def "should create inverse operation"() {
        given:
        def change = new CreateCollectionChange()
        change.setCollectionName("testCollection")
        
        when:
        Change[] inverses = change.createInverses()
        
        then:
        inverses.length == 1
        inverses[0] instanceof DropCollectionChange
        def inverse = (DropCollectionChange) inverses[0]
        inverse.getCollectionName() == "testCollection"
    }
    
    def "should extend AbstractMongoChange"() {
        expect:
        new CreateCollectionChange() instanceof AbstractMongoChange
    }
}