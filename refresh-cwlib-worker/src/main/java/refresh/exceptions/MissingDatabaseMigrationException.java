package refresh.exceptions;

/**
 * Exception thrown when the Refresh database is missing necessary migrations.
 */
public class MissingDatabaseMigrationException extends RuntimeException
{
    public MissingDatabaseMigrationException(String message)
    {
        super(message);
    }
}
