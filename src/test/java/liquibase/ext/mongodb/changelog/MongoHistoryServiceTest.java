package liquibase.ext.mongodb.changelog;

import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.database.core.DB2Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.CountCollectionByNameStatement;
import liquibase.nosql.executor.NoSqlExecutor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.lang.Boolean.FALSE;
import static liquibase.nosql.executor.NoSqlExecutor.EXECUTOR_NAME;
import static liquibase.plugin.Plugin.PRIORITY_SPECIALIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class MongoHistoryServiceTest {

    @Mock
    protected NoSqlExecutor executorMock;

    @Mock
    protected MongoConnection connectionMock;

    protected MongoLiquibaseDatabase database;

    protected MongoHistoryService historyService;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        historyService = new MongoHistoryService();
        database = new MongoLiquibaseDatabase();
        database.setConnection(connectionMock);
        historyService.setDatabase(database);
        resetServices();
    }

    @AfterEach
    void tearDown() {
        resetServices();
    }

    protected void resetServices() {
        ChangeLogHistoryServiceFactory.reset();
        Scope.getCurrentScope().getSingleton(ExecutorService.class).reset();
    }

    @Test
    void getPriority() {
        assertThat(historyService.getPriority())
                .isEqualTo(PRIORITY_SPECIALIZED)
                .isEqualTo(10);
    }

    @Test
    void getDatabase() {
        final MongoHistoryService noDbHistoryService = new MongoHistoryService();
        assertThat(noDbHistoryService.getDatabase()).isNull();
        noDbHistoryService.setDatabase(database);
        assertThat(noDbHistoryService.getDatabase()).isSameAs(database);
    }

    @Test
    void getExecutor() {
        final MongoLiquibaseDatabase database = new MongoLiquibaseDatabase();
        historyService.setDatabase(database);
        final Executor executor;
        try {
            executor = historyService.getExecutor();
            assertThat(executor).isInstanceOf(NoSqlExecutor.class);
            assertThat(historyService.getExecutor()).isSameAs(executor);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }

    @Test
    void getLogger() {
        assertThat(historyService.getLogger()).isNotNull();
    }

    @Test
    void supports() {
        assertThat(historyService.supports(database)).isTrue();
        assertThat(historyService.supports(new DB2Database())).isFalse();
    }

    @Test
    void getConverter() {
        assertThat(historyService.getConverter()).isNotNull().isInstanceOf(MongoRanChangeSetToDocumentConverter.class);
    }

    @Test
    void getDatabaseChangeLogTableName() {
        // No Database
        final MongoHistoryService noDbHistoryService = new MongoHistoryService();
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(noDbHistoryService::getDatabaseChangeLogTableName);
        assertThat(historyService.getDatabaseChangeLogTableName())
                .isEqualTo(database.getDatabaseChangeLogTableName())
                .isNotEqualTo("newTableName")
                .isEqualTo("DATABASECHANGELOG");
        database.setDatabaseChangeLogTableName("newTableName");
        assertThat(historyService.getDatabaseChangeLogTableName())
                .isEqualTo(database.getDatabaseChangeLogTableName())
                .isEqualTo("newTableName");
    }

    @Test
    void canCreateChangeLogTable() {
        assertThat(historyService.canCreateChangeLogTable()).isTrue();
    }

    @SneakyThrows
    @Test
    void init() {

        final ArgumentCaptor<AdjustChangeLogCollectionStatement> adjustChangeLogCollectionStatementArgumentCaptor =
                ArgumentCaptor.forClass(AdjustChangeLogCollectionStatement.class);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        historyService.setDatabase(database);

        doReturn(0L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(adjustChangeLogCollectionStatementArgumentCaptor.capture());

        assertThat(historyService.getHasDatabaseChangeLogTable()).isNull();
        assertThat(historyService.getAdjustedChangeLogTable()).isFalse();
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        historyService.init();

        verify(executorMock, times(1)).queryForLong(any());
        verify(executorMock, times(1)).execute(any(CreateChangeLogCollectionStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(historyService.getHasDatabaseChangeLogTable()).isTrue();
        assertThat(historyService.getAdjustedChangeLogTable()).isTrue();
        assertThat(historyService.isServiceInitialized()).isTrue();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        // Repeated init shouldn't call Create and Adjust statements nor Find By Name
        historyService.init();
        verify(executorMock, times(1)).queryForLong(any());
        verify(executorMock, times(1)).execute(any(CreateChangeLogCollectionStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(historyService.getHasDatabaseChangeLogTable()).isTrue();
        assertThat(historyService.getAdjustedChangeLogTable()).isTrue();
        assertThat(historyService.isServiceInitialized()).isTrue();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();
    }

    @SneakyThrows
    @Test
    void initWhenExistsRepositoryValidatorUnsupported() {

        final ArgumentCaptor<AdjustChangeLogCollectionStatement> adjustChangeLogCollectionStatementArgumentCaptor =
                ArgumentCaptor.forClass(AdjustChangeLogCollectionStatement.class);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        database.setSupportsValidator(FALSE);
        historyService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(adjustChangeLogCollectionStatementArgumentCaptor.capture());

        assertThat(historyService.getHasDatabaseChangeLogTable()).isNull();
        assertThat(historyService.getAdjustedChangeLogTable()).isFalse();
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        historyService.init();

        verify(executorMock, times(1)).queryForLong(any());
        verify(executorMock, times(1)).execute(any(AdjustChangeLogCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(historyService.getHasDatabaseChangeLogTable()).isTrue();
        assertThat(historyService.getAdjustedChangeLogTable()).isTrue();
        assertThat(historyService.isServiceInitialized()).isTrue();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        // Repeated init shouldn't call Create and Adjust statements nor Find By Name
        historyService.init();
        verifyNoMoreInteractions(executorMock);

        assertThat(historyService.getHasDatabaseChangeLogTable()).isTrue();
        assertThat(historyService.getAdjustedChangeLogTable()).isTrue();
        assertThat(historyService.isServiceInitialized()).isTrue();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();
    }

    @SneakyThrows
    @Test
    void initWhenExistsRepositoryNoAdjustment() {

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        database.setAdjustTrackingTablesOnStartup(FALSE);
        historyService.setDatabase(database);

        doReturn(1L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));

        assertThat(historyService.getHasDatabaseChangeLogTable()).isNull();
        assertThat(historyService.getAdjustedChangeLogTable()).isFalse();
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        historyService.init();

        verify(executorMock, times(1)).queryForLong(any());
        verifyNoMoreInteractions(executorMock);

        assertThat(historyService.getHasDatabaseChangeLogTable()).isTrue();
        assertThat(historyService.getAdjustedChangeLogTable()).isTrue();
        assertThat(historyService.isServiceInitialized()).isTrue();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        // Repeated init shouldn't call Create and Adjust statements nor Find By Name
        historyService.init();
        verifyNoMoreInteractions(executorMock);

        assertThat(historyService.getHasDatabaseChangeLogTable()).isTrue();
        assertThat(historyService.getAdjustedChangeLogTable()).isTrue();
        assertThat(historyService.isServiceInitialized()).isTrue();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();
    }

    @SneakyThrows
    @Test
    void initOnException() {

        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(EXECUTOR_NAME, database, executorMock);
        historyService.setDatabase(database);

        // Exception in CountCollectionByNameStatement
        doThrow(DatabaseException.class).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));

        assertThat(historyService.getHasDatabaseChangeLogTable()).isNull();
        assertThat(historyService.getAdjustedChangeLogTable()).isFalse();
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        assertThatExceptionOfType(UnexpectedLiquibaseException.class).isThrownBy(historyService::init)
                .withCauseExactlyInstanceOf(DatabaseException.class);

        verify(executorMock, times(1)).queryForLong(any());
        verifyNoMoreInteractions(executorMock);

        assertThat(historyService.getHasDatabaseChangeLogTable()).isNull();
        assertThat(historyService.getAdjustedChangeLogTable()).isFalse();
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        // Exception in CreateChangeLogCollectionStatement
        historyService.reset();

        assertThat(historyService.getHasDatabaseChangeLogTable()).isNull();
        assertThat(historyService.getAdjustedChangeLogTable()).isFalse();
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        doReturn(0L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doThrow(DatabaseException.class).when(executorMock).execute(any(CreateChangeLogCollectionStatement.class));

        assertThatExceptionOfType(DatabaseException.class).isThrownBy(historyService::init);

        verify(executorMock, times(2)).queryForLong(any());
        verify(executorMock, times(1)).execute(any(CreateChangeLogCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(historyService.getHasDatabaseChangeLogTable()).isFalse();
        assertThat(historyService.getAdjustedChangeLogTable()).isFalse();
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        // Exception in AdjustChangeLogCollectionStatement
        historyService.reset();

        assertThat(historyService.getHasDatabaseChangeLogTable()).isNull();
        assertThat(historyService.getAdjustedChangeLogTable()).isFalse();
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();

        doReturn(0L).when(executorMock).queryForLong(any(CountCollectionByNameStatement.class));
        doNothing().when(executorMock).execute(any(CreateChangeLogCollectionStatement.class));
        doThrow(DatabaseException.class).when(executorMock).execute(any(AdjustChangeLogCollectionStatement.class));

        assertThatExceptionOfType(DatabaseException.class).isThrownBy(historyService::init);

        verify(executorMock, times(3)).queryForLong(any());
        verify(executorMock, times(2)).execute(any(CreateChangeLogCollectionStatement.class));
        verify(executorMock, times(1)).execute(any(AdjustChangeLogCollectionStatement.class));
        verifyNoMoreInteractions(executorMock);

        assertThat(historyService.getHasDatabaseChangeLogTable()).isTrue();
        assertThat(historyService.getAdjustedChangeLogTable()).isFalse();
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.getLastChangeSetSequenceValue()).isNull();
        assertThat(historyService.getRanChangeSetList()).isNull();
    }

    @Test
    void getRanChangeSets() {
    }

    @Test
    void replaceChecksum() {
    }

    @Test
    void setExecType() {
    }

    @Test
    void removeFromHistory() {
    }

    @Test
    void getNextSequenceValue() {
    }

    @Test
    void tag() {
    }

    @Test
    void tagExists() {
    }

    @Test
    void clearAllCheckSums() {
    }

    @Test
    void destroy() {
    }

    @Test
    void getAdjustedChangeLogTable() {
    }

    @Test
    void getRunStatus() {
    }

    @Test
    void upgradeChecksums() {
    }

    @Test
    void getRanChangeSet() {
    }

    @Test
    void getRanDate() {
    }

    @Test
    void getDeploymentId() {
    }

    @Test
    void resetDeploymentId() {
    }

    @Test
    void generateDeploymentId() {
    }

    @Test
    void existsRepository() {
    }

    @Test
    void createRepository() {
    }

    @Test
    void adjustRepository() {
    }

    @Test
    void dropRepository() {
    }

    @Test
    void queryRanChangeSets() {
    }

    @Test
    void generateNextSequence() {
    }

    @Test
    void markChangeSetRun() {
    }

    @Test
    void removeRanChangeSet() {
    }

    @Test
    void clearChekSums() {
    }

    @Test
    void countTags() {
    }

    @Test
    void countRanChangeSets() {
    }

    @Test
    void updateCheckSum() {
    }

}
