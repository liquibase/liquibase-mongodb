package liquibase.ext.mongodb.change

import liquibase.ChecksumVersion
import liquibase.database.Database
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.DropCollectionStatement
import spock.lang.Specification

class DropCollectionChangeTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    
    def "should generate DropCollectionStatement"() {
        given:
        def change = new DropCollectionChange()
        change.setCollectionName("testCollection")
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof DropCollectionStatement
        def statement = (DropCollectionStatement) statements[0]
        statement.getCommand().getString("drop") == "testCollection"
    }
    
    def "should provide confirmation message"() {
        given:
        def change = new DropCollectionChange()
        change.setCollectionName("testCollection")
        
        when:
        def message = change.getConfirmationMessage()
        
        then:
        message == "Collection testCollection dropped"
    }
    
    def "should generate consistent checksum"() {
        given:
        def change1 = new DropCollectionChange()
        change1.setCollectionName("testCollection")
        
        def change2 = new DropCollectionChange()
        change2.setCollectionName("testCollection")
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 == checksum2
    }
    
    def "should generate different checksums for different collection names"() {
        given:
        def change1 = new DropCollectionChange()
        change1.setCollectionName("testCollection1")
        
        def change2 = new DropCollectionChange()
        change2.setCollectionName("testCollection2")
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 != checksum2
    }
    
    def "should extend AbstractMongoChange"() {
        expect:
        new DropCollectionChange() instanceof AbstractMongoChange
    }
}