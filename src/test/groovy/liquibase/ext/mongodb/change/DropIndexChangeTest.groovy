package liquibase.ext.mongodb.change

import liquibase.database.Database
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.DropIndexStatement
import org.bson.Document
import spock.lang.Specification

class DropIndexChangeTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    
    def "should generate DropIndexStatement"() {
        given:
        def change = new DropIndexChange()
        change.setCollectionName("testCollection")
        change.setKeys('{"name": 1, "email": 1}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof DropIndexStatement
        def statement = (DropIndexStatement) statements[0]
        statement.getCommand().getString("dropIndexes") == "testCollection"
        statement.getCommand().get("index") instanceof Document
        statement.getCommand().get("index").getInteger("name") == 1
        statement.getCommand().get("index").getInteger("email") == 1
    }
    
    def "should generate statement with index name"() {
        given:
        def change = new DropIndexChange()
        change.setCollectionName("testCollection")
        change.setKeys("name_1_email_1")
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof DropIndexStatement
        def statement = (DropIndexStatement) statements[0]
        statement.getCommand().getString("dropIndexes") == "testCollection"
        statement.getCommand().getString("index") == "name_1_email_1"
    }
    
    def "should provide confirmation message"() {
        given:
        def change = new DropIndexChange()
        change.setCollectionName("testCollection")
        
        when:
        def message = change.getConfirmationMessage()
        
        then:
        message == "Index dropped for collection testCollection"
    }
    
    def "should generate consistent checksum"() {
        given:
        def change1 = new DropIndexChange()
        change1.setCollectionName("testCollection")
        change1.setKeys('{"name": 1, "email": 1}')
        
        def change2 = new DropIndexChange()
        change2.setCollectionName("testCollection")
        change2.setKeys('{"name": 1, "email": 1}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 == checksum2
    }
    
    def "should generate different checksums for different values"() {
        given:
        def change1 = new DropIndexChange()
        change1.setCollectionName("testCollection")
        change1.setKeys('{"name": 1}')
        
        def change2 = new DropIndexChange()
        change2.setCollectionName("testCollection")
        change2.setKeys('{"email": 1}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 != checksum2
    }
    
    def "should extend AbstractMongoChange"() {
        expect:
        new DropIndexChange() instanceof AbstractMongoChange
    }
}