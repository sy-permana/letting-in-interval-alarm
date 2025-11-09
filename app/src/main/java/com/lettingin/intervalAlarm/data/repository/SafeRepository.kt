package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.util.AppLogger
import javax.inject.Inject

/**
 * Base repository class that provides safe database operation wrappers.
 * All repositories should extend this class to ensure consistent error handling.
 */
abstract class SafeRepository(
    protected val appLogger: AppLogger
) {
    
    /**
     * Wraps a database operation with exception handling and logging.
     * Returns a Result that contains either the successful value or the exception.
     * 
     * @param operation Name of the operation for logging purposes
     * @param block The database operation to execute
     * @return Result containing the operation result or exception
     */
    protected suspend fun <T> safeDbOperation(
        operation: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            appLogger.d(AppLogger.CATEGORY_DATABASE, javaClass.simpleName,
                "Executing database operation: $operation")
            val result = block()
            appLogger.d(AppLogger.CATEGORY_DATABASE, javaClass.simpleName,
                "Database operation successful: $operation")
            Result.success(result)
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, javaClass.simpleName,
                "Database operation failed: $operation", e)
            Result.failure(RepositoryException("Failed: $operation", e))
        }
    }
    
    /**
     * Wraps a database operation that doesn't return a value.
     * Throws RepositoryException on failure.
     * 
     * @param operation Name of the operation for logging purposes
     * @param block The database operation to execute
     */
    protected suspend fun safeDbOperationUnit(
        operation: String,
        block: suspend () -> Unit
    ) {
        try {
            appLogger.d(AppLogger.CATEGORY_DATABASE, javaClass.simpleName,
                "Executing database operation: $operation")
            block()
            appLogger.d(AppLogger.CATEGORY_DATABASE, javaClass.simpleName,
                "Database operation successful: $operation")
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, javaClass.simpleName,
                "Database operation failed: $operation", e)
            throw RepositoryException("Failed: $operation", e)
        }
    }
}
