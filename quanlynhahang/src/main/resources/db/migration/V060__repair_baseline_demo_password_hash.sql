SET NOCOUNT ON;

-- V002 documented password 123, but its BCrypt value did not match that password.
-- Restrict the repair to the exact bad seed hash and known demo identities so
-- existing/custom passwords are never overwritten.
IF OBJECT_ID(N'dbo.Accounts', N'U') IS NOT NULL
BEGIN
    EXEC sp_executesql N'
        UPDATE dbo.Accounts
        SET password = ''$2a$10$0r5iekadl0XG5jZLv9zCwOch9ZEYyraGPhmy/sWMYd2bWQBkS2dna''
        WHERE username IN (''admin'', ''manager'', ''waiter'', ''kitchen'', ''cashier'', ''customer'')
          AND password = ''$2a$10$5BZYd0dIfh5tsrkYljTxF.1dGgNJAHZto3e374.iz3aPoJp9tTZJS'';';
END;
