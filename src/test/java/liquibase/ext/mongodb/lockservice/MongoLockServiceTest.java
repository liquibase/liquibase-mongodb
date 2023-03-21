package liquibase.ext.mongodb.lockservice;

import liquibase.Scope;
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.core.DB2Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.LockException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.CountCollectionByNameStatement;
import liquibase.ext.mongodb.statement.DropCollectionStatement;
import liquibase.ext.mongodb.statement.FindAllStatement;
import liquibase.lockservice.DatabaseChangeLogLock;
import liquibase.lockservice.LockServiceFactory;
import liquibase.nosql.executor.NoSqlExecutor;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static liquibase.nosql.executor.NoSqlExecutor.EXECUTOR_NAME;
import static liquibase.plugin.Plugin.PRIORITY_SPECIALIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class MongoLockServiceTest {

    @Mock
    protected NoSqlExecutor executorMock;

    @Mock
    protected MongoConnection connectionMock;

    @Mock
    protected Clock clockMock;

    protected MongoLiquibaseDatabase database;

    protected MongoLockService lockService;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        lockService = new MongoLockService();
        database = new MongoLiquibaseDatabase();
        database.setConnection(connectionMock);
        resetServices();
    }

    protected void resetServices() {
        LockServiceFactory.reset();
        Scope.getCurrentScope().getSingleton(ExecutorService.class).reset();
    }

    @AfterEach
    void tearDown() {
        resetServices();
    }

    @Test
    void getPriority() {
        assertThat(lockService.getPriority())
                .isEqualTo(PRIORITY_SPECIALIZED)
                .isEqualTo(10);
    }

    @Test
    void getDatabase() {
        assertThat(lockService.getDatabase()).isNull();
        lockService.setDatabase(database);
        assertThat(lockService.getDatabase()).isSameAs(database);
    }

    @Test
    void getExecutor() {
        final MongoLiquibaseDatabase database = new MongoLiquibaseDatabase();
        lockService.setDatabase(database);
        final Executor executor;
        try {
            executor = lockService.getExecutor();
            assertThat(executor).isInstanceOf(NoSqlExecutor.class);
            assertThat(lockService.getExecutor()).isSameAs(executor);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }

    @Test
    void getLogger() {
        assertThat(lockService.getLogger()).isNotNull();
    }

    @Test
    void supports() {
        assertThat(lockService.supports(database)).isTrue();
        assertThat(lockService.supports(new DB2Database())).isFalse();
    }

    @Test
    void getConverter() {
        assertThat(lockService.getConverter()).isNotNull().isInstanceOf(MongoChangeLogLockToDocumentConverter.class);
    }

    @SneakyThrows
    @Test
    void init() {

        final ArgumentCaptor<AdjustChangeLogLockCollectionStatement> adjustChangeLogLockCollectionStatementArgumentCaptor =
                ArgumentCaptor.forClass(AdjustChangeLogLockCollectionStatement.class);


        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(0L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(adjustChangeLogLockCollectionStatementArgumentCaptor.capture());

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        lockService.init();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).execute(any(CreateChangeLogLockCollectionStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogLockCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        // Repeated init shouldn't call Create and Adjust statements nor Find By Name
        lockService.init();
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void initWhenExistsRepository() {
        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(any(AdjustChangeLogLockCollectionStatement.class));

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        lockService.init();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogLockCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        // Repeated init shouldn't call Create and Adjust statements nor Find By Name
        lockService.init();
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void initWhenNoRepositoryNoAdjustment() {
        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(0L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(any(CreateChangeLogLockCollectionStatement.class));

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        database.setAdjustTrackingTablesOnStartup(FALSE);
        lockService.init();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).execute(any(CreateChangeLogLockCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void initWhenExistsRepositoryNoAdjustment() {
        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        database.setAdjustTrackingTablesOnStartup(FALSE);
        lockService.init();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void initOnException() {
        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        // Exception in CountCollectionByNameStatement
        doThrow(DatabaseException.class).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));

        assertThatExceptionOfType(DatabaseException.class).isThrownBy(lockService::init);
        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verifyNoMoreInteractions(executorMock);

        // Exception in CreateChangeLogLockCollectionStatement
        lockService.reset();
        doReturn(0L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doThrow(DatabaseException.class).when(executorMock).execute(any(CreateChangeLogLockCollectionStatement.class));

        assertThatExceptionOfType(DatabaseException.class).isThrownBy(lockService::init);
        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isFalse();

        verify(executorMock, times(2)).queryForLong(any());
        verify(executorMock, times(1)).execute(any(CreateChangeLogLockCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        // Exception in AdjustChangeLogLockCollectionStatement
        lockService.reset();
        doNothing().when(executorMock).execute(any(CreateChangeLogLockCollectionStatement.class));
        doThrow(DatabaseException.class).when(executorMock).execute(any(AdjustChangeLogLockCollectionStatement.class));

        assertThatExceptionOfType(DatabaseException.class).isThrownBy(lockService::init);
        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        verify(executorMock, times(3)).queryForLong(any());
        verify(executorMock, times(2)).execute(any(CreateChangeLogLockCollectionStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogLockCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);
    }

    @SneakyThrows
    @Test
    void waitForLock() {
        final MongoChangeLogLock lockedLock = new MongoChangeLogLock(1, new Date(), "lockedByMock", true);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(any(AdjustChangeLogLockCollectionStatement.class));
        doReturn(lockService.getConverter().toDocument(lockedLock))
                .when(executorMock).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        doReturn(Collections.singletonList((Object) lockService.getConverter().toDocument(lockedLock)))
                .when(executorMock).queryForList(any(FindAllStatement.class), eq(Document.class));

        lockService.setClock(clockMock);
        lockService.setChangeLogLockRecheckTime(0);
        final Instant instantMock = Clock.systemUTC().instant();
        // On two times will exit as returns a future +24 h timestamp from mock timestamp
        doReturn(instantMock, instantMock, instantMock, instantMock.plusSeconds(60 * 60 * 24)).when(clockMock).instant();

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.hasChangeLogLock()).isFalse();
        assertThatExceptionOfType(LockException.class).isThrownBy(lockService::waitForLock)
                .withMessageStartingWith("Could not acquire change log lock.  Currently locked by lockedByMock since");

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogLockCollectionStatement.class));
        verify(executorMock, times(2)).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        verify(executorMock, times(1)).queryForList(any(FindAllStatement.class), eq(Document.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void acquireLock() {

        final ArgumentCaptor<ReplaceChangeLogLockStatement> replaceLockChangeLogStatementArgumentCaptor =
                ArgumentCaptor.forClass(ReplaceChangeLogLockStatement.class);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);
        database.setAdjustTrackingTablesOnStartup(FALSE);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doReturn(null).when(executorMock).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        doReturn(1).when(executorMock).update(replaceLockChangeLogStatementArgumentCaptor.capture());

        assertThat(lockService.hasChangeLogLock()).isFalse();
        assertThat(lockService.acquireLock()).isTrue();
        assertThat(replaceLockChangeLogStatementArgumentCaptor.getValue().isLocked()).isTrue();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        verify(executorMock, times(1)).update(any(ReplaceChangeLogLockStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isTrue();

        // Repeated call should return true instantly with no interaction
        assertThat(lockService.acquireLock()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isTrue();
        verifyNoMoreInteractions(executorMock);
    }

    @SneakyThrows
    @Test
    void acquireLockWhenLocked() {

        final MongoChangeLogLock lockedLock = new MongoChangeLogLock(1, new Date(), "lockedByMock", true);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(any(AdjustChangeLogLockCollectionStatement.class));
        doReturn(lockService.getConverter().toDocument(lockedLock))
                .when(executorMock).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.hasChangeLogLock()).isFalse();
        assertThat(lockService.acquireLock()).isFalse();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogLockCollectionStatement.class));
        verify(executorMock, times(1)).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        // Repeated call should return false and do not enter table creation
        assertThat(lockService.acquireLock()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();
        verifyNoMoreInteractions(executorMock);
    }

    @SneakyThrows
    @Test
    void acquireLockWhenConcurrentLocked() {

        final MongoChangeLogLock lockedLock = new MongoChangeLogLock(1, new Date(), "lockedByMock", FALSE);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(any(AdjustChangeLogLockCollectionStatement.class));
        doReturn(lockService.getConverter().toDocument(lockedLock))
                .when(executorMock).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        doReturn(0).when(executorMock).update(any(ReplaceChangeLogLockStatement.class));

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.hasChangeLogLock()).isFalse();
        assertThat(lockService.acquireLock()).isFalse();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogLockCollectionStatement.class));
        verify(executorMock, times(1)).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        verify(executorMock, times(1)).update(any(ReplaceChangeLogLockStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void acquireLockOnException() {

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(0L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(any(CreateChangeLogLockCollectionStatement.class));
        doNothing().when(executorMock).execute(any(AdjustChangeLogLockCollectionStatement.class));
        doThrow(DatabaseException.class).when(executorMock).update(any(ReplaceChangeLogLockStatement.class));

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.hasChangeLogLock()).isFalse();
        assertThatExceptionOfType(LockException.class).isThrownBy(lockService::acquireLock);

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogLockCollectionStatement.class));
        verify(executorMock, times(1)).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        verify(executorMock, times(1)).update(any(ReplaceChangeLogLockStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void acquireLockOnExceptionMultipleUpdated() {

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(any(AdjustChangeLogLockCollectionStatement.class));
        doReturn(null).when(executorMock).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        doReturn(2).when(executorMock).update(any(ReplaceChangeLogLockStatement.class));

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.hasChangeLogLock()).isFalse();
        assertThatExceptionOfType(LockException.class).isThrownBy(lockService::acquireLock);

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogLockCollectionStatement.class));
        verify(executorMock, times(1)).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        verify(executorMock, times(1)).update(any(ReplaceChangeLogLockStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void releaseLock() {
        final ArgumentCaptor<ReplaceChangeLogLockStatement> replaceLockChangeLogStatementArgumentCaptor =
                ArgumentCaptor.forClass(ReplaceChangeLogLockStatement.class);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doReturn(1).when(executorMock).update(replaceLockChangeLogStatementArgumentCaptor.capture());

        assertThat(lockService.hasChangeLogLock()).isFalse();
        lockService.releaseLock();
        assertThat(replaceLockChangeLogStatementArgumentCaptor.getValue().isLocked()).isFalse();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).update(any(ReplaceChangeLogLockStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void releaseLockOnException() {
        final ArgumentCaptor<ReplaceChangeLogLockStatement> replaceLockChangeLogStatementArgumentCaptor =
                ArgumentCaptor.forClass(ReplaceChangeLogLockStatement.class);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doThrow(DatabaseException.class).when(executorMock).update(replaceLockChangeLogStatementArgumentCaptor.capture());

        assertThat(lockService.hasChangeLogLock()).isFalse();
        assertThatExceptionOfType(LockException.class).isThrownBy(lockService::releaseLock);
        assertThat(replaceLockChangeLogStatementArgumentCaptor.getValue().isLocked()).isFalse();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).update(any(ReplaceChangeLogLockStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void listLocks() {
        final MongoChangeLogLock lockedLock = new MongoChangeLogLock(1, new Date(), "lockedByMock", TRUE);
        final MongoChangeLogLock unlockedLock = new MongoChangeLogLock(2, new Date(), "unlockedByMock", FALSE);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doReturn(Arrays.asList(
                lockService.getConverter().toDocument(lockedLock),
                (Object) lockService.getConverter().toDocument(unlockedLock)))
                .when(executorMock).queryForList(any(FindAllStatement.class), eq(Document.class));

        DatabaseChangeLogLock[] databaseChangeLogLocks = lockService.listLocks();
        assertThat(databaseChangeLogLocks).hasSize(1).allMatch(l -> ((MongoChangeLogLock) l).getLocked());

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).queryForList(any(FindAllStatement.class), eq(Document.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();

    }

    @SneakyThrows
    @Test
    void listLocksRepositoryNotExists() {

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(0L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));

        assertThat(lockService.listLocks()).isEmpty();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();

    }

    @SneakyThrows
    @Test
    void forceReleaseLock() {
        final ArgumentCaptor<ReplaceChangeLogLockStatement> replaceLockChangeLogStatementArgumentCaptor =
                ArgumentCaptor.forClass(ReplaceChangeLogLockStatement.class);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(any(AdjustChangeLogLockCollectionStatement.class));
        doReturn(1).when(executorMock).update(replaceLockChangeLogStatementArgumentCaptor.capture());

        assertThat(lockService.hasChangeLogLock()).isFalse();
        lockService.forceReleaseLock();
        assertThat(replaceLockChangeLogStatementArgumentCaptor.getValue().isLocked()).isFalse();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogLockCollectionStatement.class));
        verify(executorMock, times(1)).update(any(ReplaceChangeLogLockStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void reset() {

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);
        database.setAdjustTrackingTablesOnStartup(FALSE);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        doReturn("catalogMock").when(connectionMock).getCatalog();
        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doReturn(null).when(executorMock).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        doReturn(1).when(executorMock).update(any(ReplaceChangeLogLockStatement.class));

        assertThat(lockService.acquireLock()).isTrue();

        verify(executorMock, times(1)).queryForLong(any(CountCollectionByNameStatement.class));
        verify(executorMock, times(1)).queryForObject(any(SelectChangeLogLockStatement.class), eq(Document.class));
        verify(executorMock, times(1)).update(any(ReplaceChangeLogLockStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isTrue();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isTrue();
        assertThat(lockService.hasChangeLogLock()).isTrue();

        // Reset
        lockService.reset();
        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();
        verifyNoMoreInteractions(executorMock);
    }

    @SneakyThrows
    @Test
    void destroy() {
        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);
        database.setAdjustTrackingTablesOnStartup(FALSE);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        doNothing().when(executorMock).execute(any(DropCollectionStatement.class));

        lockService.destroy();

        verify(executorMock, times(1)).execute(any(DropCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        // Retry
        lockService.destroy();
        verify(executorMock, times(2)).execute(any(DropCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @SneakyThrows
    @Test
    void destroyOnException() {
        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        lockService.setDatabase(database);
        database.setAdjustTrackingTablesOnStartup(FALSE);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();

        doThrow(DatabaseException.class).when(executorMock).execute(any(DropCollectionStatement.class));

        assertThatExceptionOfType(UnexpectedLiquibaseException.class).isThrownBy(lockService::destroy);

        verify(executorMock, times(1)).execute(any(DropCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(lockService.getHasDatabaseChangeLogLockTable()).isNull();
        assertThat(lockService.getAdjustedChangeLogLockTable()).isFalse();
        assertThat(lockService.hasChangeLogLock()).isFalse();
    }

    @Test
    void getDatabaseChangeLogLockTableName() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(lockService::getDatabaseChangeLogLockTableName);
        lockService.setDatabase(database);
        assertThat(lockService.getDatabaseChangeLogLockTableName())
                .isEqualTo(database.getDatabaseChangeLogLockTableName())
                .isNotEqualTo("newTableName")
                .isEqualTo("DATABASECHANGELOGLOCK");
        database.setDatabaseChangeLogLockTableName("newTableName");
        assertThat(lockService.getDatabaseChangeLogLockTableName())
                .isEqualTo(database.getDatabaseChangeLogLockTableName())
                .isEqualTo("newTableName");
    }

    @Test
    void setChangeLogLockRecheckTime() {
        assertThat(lockService.getChangeLogLockRecheckTime())
                .isEqualTo(LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class).getDatabaseChangeLogLockPollRate())
                .isNotEqualTo(1000L);
        lockService.setChangeLogLockRecheckTime(1000L);
        assertThat(lockService.getChangeLogLockRecheckTime())
                .isNotEqualTo(LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class).getDatabaseChangeLogLockPollRate())
                .isEqualTo(1000L);
    }

    @Test
    void setChangeLogLockWaitTime() {
        assertThat(lockService.getChangeLogLockWaitTime())
                .isEqualTo(LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class).getDatabaseChangeLogLockWaitTime())
                .isNotEqualTo(1000L);
        lockService.setChangeLogLockWaitTime(1000L);
        assertThat(lockService.getChangeLogLockWaitTime())
                .isNotEqualTo(LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class).getDatabaseChangeLogLockWaitTime())
                .isEqualTo(1000L);
    }
}
