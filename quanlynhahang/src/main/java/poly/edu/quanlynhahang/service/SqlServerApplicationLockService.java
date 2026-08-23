package poly.edu.quanlynhahang.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Executes SQL Server transaction-owned application locks on the current Spring transaction. */
@Service
public class SqlServerApplicationLockService {
    private final JdbcTemplate jdbcTemplate;

    public SqlServerApplicationLockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int acquireExclusive(String resource, int timeoutMillis) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Application lock requires an active Spring transaction");
        }
        Integer result = jdbcTemplate.queryForObject("""
                SET NOCOUNT ON;
                IF @@TRANCOUNT = 0 BEGIN TRANSACTION;
                DECLARE @result int;
                EXEC @result = sys.sp_getapplock
                    @Resource = ?,
                    @LockMode = 'Exclusive',
                    @LockOwner = 'Transaction',
                    @LockTimeout = ?,
                    @DbPrincipal = 'public';
                SELECT @result;
                """, Integer.class, resource, timeoutMillis);
        return result == null ? -999 : result;
    }
}
