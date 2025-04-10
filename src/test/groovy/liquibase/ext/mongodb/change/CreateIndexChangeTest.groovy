package liquibase.ext.mongodb.change

import liquibase.ChecksumVersion
import liquibase.change.Change
import liquibase.database.Database
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.CreateIndexStatement
import spock.lang.Specification

class CreateIndexChangeTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    
    def "should generate CreateIndexStatement"() {
        given:
        def change = new CreateIndexChange()
        change.setCollectionName("testCollection")
        change.setKeys('{"name": 1, "age": -1}')
        change.setOptions('{"unique": true, "background": true}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof CreateIndexStatement
        def statement = (CreateIndexStatement) statements[0]
        statement.getCommand().getString("createIndexes") == "testCollection"
        statement.getCommand().get("indexes") instanceof List
        statement.getCommand().getList("indexes", Object.class).size() == 1
        statement.getCommand().getList("indexes", Object.class)[0].get("key").get("name") == 1
        statement.getCommand().getList("indexes", Object.class)[0].get("key").get("age") == -1
        statement.getCommand().getList("indexes", Object.class)[0].get("unique") == true
        statement.getCommand().getList("indexes", Object.class)[0].get("background") == true
    }
    
    def "should generate statement without options"() {
        given:
        def change = new CreateIndexChange()
        change.setCollectionName("testCollection")
        change.setKeys('{"name": 1}')
        change.setOptions(null)
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof CreateIndexStatement
        def statement = (CreateIndexStatement) statements[0]
        statement.getCommand().getString("createIndexes") == "testCollection"
        statement.getCommand().get("indexes") instanceof List
        statement.getCommand().getList("indexes", Object.class).size() == 1
        statement.getCommand().getList("indexes", Object.class)[0].get("key").get("name") == 1
    }
    
    def "should support special index types"() {
        given:
        def change = new CreateIndexChange()
        change.setCollectionName("testCollection")
        change.setKeys('{"location": "2dsphere"}')
        change.setOptions('{"name": "locationIndex"}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof CreateIndexStatement
        def statement = (CreateIndexStatement) statements[0]
        statement.getCommand().getString("createIndexes") == "testCollection"
        statement.getCommand().get("indexes") instanceof List
        statement.getCommand().getList("indexes", Object.class).size() == 1
        statement.getCommand().getList("indexes", Object.class)[0].get("key").get("location") == "2dsphere"
        statement.getCommand().getList("indexes", Object.class)[0].get("name") == "locationIndex"
    }
    
    def "should provide confirmation message"() {
        given:
        def change = new CreateIndexChange()
        change.setCollectionName("testCollection")
        
        when:
        def message = change.getConfirmationMessage()
        
        then:
        message == "Index created for collection testCollection"
    }
    
    def "should generate consistent checksum"() {
        given:
        def change1 = new CreateIndexChange()
        change1.setCollectionName("testCollection")
        change1.setKeys('{"name": 1}')
        change1.setOptions('{"unique": true}')
        
        def change2 = new CreateIndexChange()
        change2.setCollectionName("testCollection")
        change2.setKeys('{"name": 1}')
        change2.setOptions('{"unique": true}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 == checksum2
    }
    
    def "should generate different checksums for different values"() {
        given:
        def change1 = new CreateIndexChange()
        change1.setCollectionName("testCollection")
        change1.setKeys('{"name": 1}')
        
        def change2 = new CreateIndexChange()
        change2.setCollectionName("testCollection")
        change2.setKeys('{"age": 1}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 != checksum2
    }
    
    def "should create inverse operation"() {
        given:
        def change = new CreateIndexChange()
        change.setCollectionName("testCollection")
        change.setKeys('{"name": 1}')
        
        when:
        Change[] inverses = change.createInverses()
        
        then:
        inverses.length == 1
        inverses[0] instanceof DropIndexChange
        def inverse = (DropIndexChange) inverses[0]
        inverse.getCollectionName() == "testCollection"
        inverse.getKeys() == '{"name": 1}'
    }
    
    def "should extend AbstractMongoChange"() {
        expect:
        new CreateIndexChange() instanceof AbstractMongoChange
    }
}