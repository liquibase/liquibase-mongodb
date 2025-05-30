package liquibase.ext.mongodb.changelog

import liquibase.ContextExpression
import liquibase.Labels
import liquibase.changelog.ChangeSet
import liquibase.change.CheckSum
import org.bson.Document
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class MongoRanChangeSetToDocumentConverterTest extends Specification {
    
    MongoRanChangeSetToDocumentConverter converter
    
    def setup() {
        converter = new MongoRanChangeSetToDocumentConverter()
    }
    
    def "should convert RanChangeSet to Document"() {
        given:
        def date = Date.from(
            LocalDateTime.of(2023, 1, 1, 12, 0, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )
        
        def ranChangeSet = new MongoRanChangeSet(
            "changelog.xml",              // changeLog
            "001",                        // id
            "testAuthor",                 // author
            CheckSum.parse("9:checksum"), // checkSum
            date,                         // dateExecuted
            "v1.0",                       // tag
            ChangeSet.ExecType.EXECUTED,  // execType
            "Test change",                // description
            "Test comment",               // comments
            new ContextExpression("test"),// contexts
            null,                         // inheritableContexts
            new Labels("label1"),// labels
            "deployment1",                // deploymentId
            1,                           // orderExecuted
            "4.25.0"                     // liquibaseVersion
        )
        
        when:
        def document = converter.toDocument(ranChangeSet)
        
        then:
        document instanceof Document
        document.getString(MongoRanChangeSet.Fields.fileName) == "changelog.xml"
        document.getString(MongoRanChangeSet.Fields.changeSetId) == "001"
        document.getString(MongoRanChangeSet.Fields.author) == "testAuthor"
        document.getString(MongoRanChangeSet.Fields.tag) == "v1.0"
        document.getString(MongoRanChangeSet.Fields.execType) == "EXECUTED"
        document.getString(MongoRanChangeSet.Fields.description) == "Test change"
        document.getString(MongoRanChangeSet.Fields.comments) == "Test comment"
        document.getString(MongoRanChangeSet.Fields.contexts) == "test"
        document.getDate(MongoRanChangeSet.Fields.dateExecuted) == date
        document.getString(MongoRanChangeSet.Fields.deploymentId) == "deployment1"
        document.getInteger(MongoRanChangeSet.Fields.orderExecuted) == 1
        document.getString(MongoRanChangeSet.Fields.liquibase) == "4.25.0"
    }
    
    def "should handle null values when converting to Document"() {
        given:
        def date = new Date()
        def ranChangeSet = new MongoRanChangeSet(
            "changelog.xml",              // changeLog
            "001",                        // id
            "testAuthor",                 // author
            null,                         // checkSum
            date,                         // dateExecuted
            null,                         // tag
            ChangeSet.ExecType.EXECUTED,  // execType
            null,                         // description
            null,                         // comments
            null,                         // contexts
            null,                         // inheritableContexts
            null,                         // labels
            null,                         // deploymentId
            1,                           // orderExecuted
            null                         // liquibaseVersion
        )
        
        when:
        def document = converter.toDocument(ranChangeSet)
        
        then:
        document instanceof Document
        document.getString(MongoRanChangeSet.Fields.fileName) == "changelog.xml"
        document.getString(MongoRanChangeSet.Fields.changeSetId) == "001"
        document.getString(MongoRanChangeSet.Fields.author) == "testAuthor"
        document.getString(MongoRanChangeSet.Fields.tag) == null
        document.getString(MongoRanChangeSet.Fields.execType) == "EXECUTED"
        document.getString(MongoRanChangeSet.Fields.description) == null
        document.getString(MongoRanChangeSet.Fields.comments) == null
        document.getString(MongoRanChangeSet.Fields.contexts) == null
        document.getDate(MongoRanChangeSet.Fields.dateExecuted) == date
        document.getString(MongoRanChangeSet.Fields.deploymentId) == null
        document.getInteger(MongoRanChangeSet.Fields.orderExecuted) == 1
    }
    
    def "should convert Document to RanChangeSet"() {
        given:
        def date = new Date()
        def document = new Document()
        document.put(MongoRanChangeSet.Fields.fileName, "changelog.xml")
        document.put(MongoRanChangeSet.Fields.changeSetId, "001")
        document.put(MongoRanChangeSet.Fields.author, "testAuthor")
        document.put(MongoRanChangeSet.Fields.md5sum, "9:abc123")
        document.put(MongoRanChangeSet.Fields.dateExecuted, date)
        document.put(MongoRanChangeSet.Fields.tag, "v1.0")
        document.put(MongoRanChangeSet.Fields.execType, "EXECUTED")
        document.put(MongoRanChangeSet.Fields.description, "Test change")
        document.put(MongoRanChangeSet.Fields.comments, "Test comment")
        document.put(MongoRanChangeSet.Fields.contexts, "test")
        document.put(MongoRanChangeSet.Fields.labels, "label1,label2")
        document.put(MongoRanChangeSet.Fields.deploymentId, "deployment1")
        document.put(MongoRanChangeSet.Fields.orderExecuted, 1)
        document.put(MongoRanChangeSet.Fields.liquibase, "4.10.0")
        
        when:
        def ranChangeSet = converter.fromDocument(document)
        
        then:
        ranChangeSet instanceof MongoRanChangeSet
        ranChangeSet.getChangeLog() == "changelog.xml"
        ranChangeSet.getId() == "001"
        ranChangeSet.getAuthor() == "testAuthor"
        ranChangeSet.getDateExecuted() == date
        ranChangeSet.getTag() == "v1.0"
        ranChangeSet.getExecType() == ChangeSet.ExecType.EXECUTED
        ranChangeSet.getDescription() == "Test change"
        ranChangeSet.getComments() == "Test comment"
        ranChangeSet.getContextExpression().toString() == "test"
        ranChangeSet.getDeploymentId() == "deployment1"
        ranChangeSet.getOrderExecuted() == 1
        ranChangeSet.getLiquibaseVersion() == "4.10.0"
    }
    
    def "should handle null values when converting from Document"() {
        given:
        def date = new Date()
        def document = new Document()
        document.put(MongoRanChangeSet.Fields.fileName, "changelog.xml")
        document.put(MongoRanChangeSet.Fields.changeSetId, "001")
        document.put(MongoRanChangeSet.Fields.author, "testAuthor")
        document.put(MongoRanChangeSet.Fields.dateExecuted, date)
        document.put(MongoRanChangeSet.Fields.execType, "EXECUTED")
        document.put(MongoRanChangeSet.Fields.orderExecuted, 1)
        
        when:
        def ranChangeSet = converter.fromDocument(document)
        
        then:
        ranChangeSet instanceof MongoRanChangeSet
        ranChangeSet.getChangeLog() == "changelog.xml"
        ranChangeSet.getId() == "001"
        ranChangeSet.getAuthor() == "testAuthor"
        ranChangeSet.getDateExecuted() == date
        ranChangeSet.getTag() == null
        ranChangeSet.getExecType() == ChangeSet.ExecType.EXECUTED
        ranChangeSet.getDescription() == null
        ranChangeSet.getComments() == null
        ranChangeSet.getContextExpression() == null
        ranChangeSet.getDeploymentId() == null
        ranChangeSet.getOrderExecuted() == 1
    }
}