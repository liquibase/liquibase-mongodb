package liquibase.ext.mongodb.lockservice

import org.bson.Document
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId

class MongoChangeLogLockToDocumentConverterTest extends Specification {
    
    MongoChangeLogLockToDocumentConverter converter
    
    def setup() {
        converter = new MongoChangeLogLockToDocumentConverter()
    }
    
    def "should convert MongoChangeLogLock to Document"() {
        given:
        def date = Date.from(
            LocalDateTime.of(2023, 1, 1, 12, 0, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )
        
        def lock = new MongoChangeLogLock(
            1,                  // id
            date,               // lockGranted
            "test-host",        // lockedBy
            true                // locked
        )
        
        when:
        def document = converter.toDocument(lock)
        
        then:
        document instanceof Document
        document.getInteger(MongoChangeLogLock.Fields.id) == 1
        document.getBoolean(MongoChangeLogLock.Fields.locked) == true
        document.getString(MongoChangeLogLock.Fields.lockedBy) == "test-host"
        document.getDate(MongoChangeLogLock.Fields.lockGranted) == date
    }
    
    def "should handle null values when converting to Document"() {
        given:
        def defaultDate = new Date(0)
        def lock = new MongoChangeLogLock(
            1,                  // id
            defaultDate,        // lockGranted
            null,               // lockedBy
            false               // locked
        )
        
        when:
        def document = converter.toDocument(lock)
        
        then:
        document instanceof Document
        document.getInteger(MongoChangeLogLock.Fields.id) == 1
        document.getBoolean(MongoChangeLogLock.Fields.locked) == false
        !document.containsKey(MongoChangeLogLock.Fields.lockedBy) || document.get(MongoChangeLogLock.Fields.lockedBy) == null
        document.getDate(MongoChangeLogLock.Fields.lockGranted) == defaultDate
    }
    
    def "should convert Document to MongoChangeLogLock"() {
        given:
        def date = new Date()
        def document = new Document()
        document.put(MongoChangeLogLock.Fields.id, 1)
        document.put(MongoChangeLogLock.Fields.locked, true)
        document.put(MongoChangeLogLock.Fields.lockedBy, "test-host")
        document.put(MongoChangeLogLock.Fields.lockGranted, date)
        
        when:
        def lock = converter.fromDocument(document)
        
        then:
        lock instanceof MongoChangeLogLock
        lock.getId() == 1
        lock.getLocked() == true
        lock.getLockedBy() == "test-host"
        lock.getLockGranted() == date
    }
    
    def "should handle null values when converting from Document"() {
        given:
        def defaultDate = new Date(0)
        def document = new Document()
        document.put(MongoChangeLogLock.Fields.id, 1)
        document.put(MongoChangeLogLock.Fields.locked, false)
        document.put(MongoChangeLogLock.Fields.lockGranted, defaultDate)
        document.put(MongoChangeLogLock.Fields.lockedBy, null)
        
        when:
        def lock = converter.fromDocument(document)
        
        then:
        lock instanceof MongoChangeLogLock
        lock.getId() == 1
        lock.getLocked() == false
        lock.getLockedBy() == null
        lock.getLockGranted() == defaultDate
    }
}