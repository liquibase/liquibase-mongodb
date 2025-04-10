package liquibase.ext.mongodb.changelog

import liquibase.changelog.ChangeSet
import liquibase.changelog.RanChangeSet
import liquibase.exception.DatabaseException
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.CountCollectionByNameStatement
import liquibase.ext.mongodb.statement.CountDocumentsInCollectionStatement
import liquibase.ext.mongodb.statement.DeleteManyStatement
import liquibase.ext.mongodb.statement.DropCollectionStatement
import liquibase.ext.mongodb.statement.FindAllStatement
import liquibase.ext.mongodb.statement.InsertOneStatement
import liquibase.ext.mongodb.statement.UpdateManyStatement
import liquibase.nosql.executor.NoSqlExecutor
import org.bson.Document
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MongoHistoryServiceTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    def executor = Mock(NoSqlExecutor)
    def mockClock = Clock.fixed(Instant.ofEpochMilli(1234567890000), ZoneId.systemDefault())
    
    def historyService = new MongoHistoryService()
    
    def setup() {
        historyService.setDatabase(database)
        database.getDatabaseChangeLogTableName() >> "DATABASECHANGELOG"
        database.getDatabaseProductName() >> MongoLiquibaseDatabase.MONGODB_PRODUCT_NAME
        database.getConnection() >> Mock(liquibase.database.DatabaseConnection) {
            getCatalog() >> "testdb"
        }
        executor.queryForLong(_ as CountCollectionByNameStatement) >> 1L
        executor.queryForLong(_ as CountDocumentsInCollectionStatement) >> 0L
        historyService.setClock(mockClock)
        
        // Mock executor access through NoSqlService
        database.getNoSqlExecutor() >> executor
    }
    
    def "should return priority for specialized services"() {
        expect:
        historyService.getPriority() == Integer.valueOf(10)
    }
    
    def "should support MongoDB database"() {
        expect:
        historyService.supports(database)
    }
    
    def "should check if repository exists"() {
        given:
        executor.queryForLong(_ as CountCollectionByNameStatement) >> 1L
        
        when:
        def result = historyService.existsRepository()
        
        then:
        result
    }
    
    def "should create repository"() {
        when:
        historyService.createRepository()
        
        then:
        1 * executor.execute(_ as CreateChangeLogCollectionStatement)
    }
    
    def "should drop repository"() {
        when:
        historyService.dropRepository()
        
        then:
        1 * executor.execute(_ as DropCollectionStatement)
    }
    
    def "should query ran change sets"() {
        given:
        def document1 = new Document("fileName", "file1.xml")
                .append("id", "1")
                .append("author", "author1")
                .append("dateExecuted", new Date())
                .append("orderExecuted", 1)
                .append("md5sum", "checksumValue")
                .append("execType", "EXECUTED")
        def document2 = new Document("fileName", "file2.xml")
                .append("id", "2")
                .append("author", "author2")
                .append("dateExecuted", new Date())
                .append("orderExecuted", 2)
                .append("md5sum", "checksumValue2")
                .append("execType", "EXECUTED")
        
        executor.queryForList(_ as FindAllStatement, Document.class) >> [document1, document2]
        
        when:
        def result = historyService.queryRanChangeSets()
        
        then:
        result.size() == 2
        result[0].id == "1"
        result[0].author == "author1"
        result[1].id == "2"
        result[1].author == "author2"
    }
    
    def "should mark change set as run for first time"() {
        given:
        def changeSet = Mock(ChangeSet) {
            getFilePath() >> "file1.xml"
            getId() >> "1"
            getAuthor() >> "author1"
            getDescription() >> "description"
            getComments() >> "comments"
            generateCheckSum(_) >> "8:checksumvalue"
        }
        
        when:
        historyService.markChangeSetRun(changeSet, ChangeSet.ExecType.EXECUTED, 1)
        
        then:
        1 * executor.execute(_ as InsertOneStatement)
    }
    
    def "should mark change set as run for update"() {
        given:
        def changeSet = Mock(ChangeSet) {
            getFilePath() >> "file1.xml"
            getId() >> "1"
            getAuthor() >> "author1"
            generateCheckSum(_) >> "8:checksumvalue"
        }
        
        when:
        historyService.markChangeSetRun(changeSet, ChangeSet.ExecType.RERAN, 2)
        
        then:
        1 * executor.update(_ as UpdateManyStatement)
    }
    
    def "should remove ran change set"() {
        given:
        def changeSet = Mock(ChangeSet) {
            getFilePath() >> "file1.xml"
            getId() >> "1"
            getAuthor() >> "author1"
        }
        
        when:
        historyService.removeRanChangeSet(changeSet)
        
        then:
        1 * executor.update(_ as DeleteManyStatement)
    }
    
    def "should clear checksums"() {
        when:
        historyService.clearChekSums()
        
        then:
        1 * executor.update(_ as UpdateManyStatement)
    }
    
    def "should count tags"() {
        given:
        executor.queryForLong(_ as CountDocumentsInCollectionStatement) >> 3L
        
        when:
        def result = historyService.countTags("version1.0")
        
        then:
        result == 3L
    }
    
    def "should update checksum"() {
        given:
        def changeSet = Mock(ChangeSet) {
            getFilePath() >> "file1.xml"
            getId() >> "1"
            getAuthor() >> "author1"
            generateCheckSum(_) >> "8:newchecksum"
        }
        
        when:
        historyService.updateCheckSum(changeSet)
        
        then:
        1 * executor.update(_ as UpdateManyStatement)
    }
    
    def "should handle database exceptions"() {
        given:
        executor.queryForList(_ as FindAllStatement, Document.class) >> { throw new DatabaseException("Test exception") }
        
        when:
        historyService.getRanChangeSets()
        
        then:
        thrown(DatabaseException)
    }
}