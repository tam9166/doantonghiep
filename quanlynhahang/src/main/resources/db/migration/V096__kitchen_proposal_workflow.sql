IF OBJECT_ID('dbo.kitchen_proposals', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.kitchen_proposals (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_kitchen_proposals PRIMARY KEY,
        proposal_type VARCHAR(20) NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_kitchen_proposals_status DEFAULT 'PENDING',
        proposed_by VARCHAR(50) NOT NULL,
        proposer_role VARCHAR(30) NOT NULL,
        payload NVARCHAR(MAX) NOT NULL,
        reason NVARCHAR(1000) NOT NULL,
        review_note NVARCHAR(1000) NULL,
        reviewed_by VARCHAR(50) NULL,
        created_entity_type VARCHAR(30) NULL,
        created_entity_id VARCHAR(50) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_kitchen_proposals_created_at DEFAULT SYSDATETIME(),
        reviewed_at DATETIME2(0) NULL,
        CONSTRAINT CK_kitchen_proposals_type CHECK (proposal_type IN ('INGREDIENT','DISH','RECIPE')),
        CONSTRAINT CK_kitchen_proposals_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
    );
    CREATE INDEX IX_kitchen_proposals_status_created ON dbo.kitchen_proposals(status, created_at DESC);
    CREATE INDEX IX_kitchen_proposals_author ON dbo.kitchen_proposals(proposed_by, created_at DESC);
END;
