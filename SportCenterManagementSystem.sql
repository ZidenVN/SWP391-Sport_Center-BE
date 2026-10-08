CREATE DATABASE IF NOT EXISTS FitnessCenterDB
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE FitnessCenterDB;


-- =========================================================
-- 1. PERMISSIONS
-- =========================================================

CREATE TABLE Permissions (
    PermissionID INT AUTO_INCREMENT,
    Code VARCHAR(50) NOT NULL,
    PermissionName VARCHAR(100) NOT NULL,

    CONSTRAINT PK_Permissions_PermissionID
        PRIMARY KEY (PermissionID),

    CONSTRAINT UQ_Permissions_Code
        UNIQUE (Code)
) ENGINE=InnoDB;


-- =========================================================
-- 2. ROLES
-- =========================================================

CREATE TABLE Roles (
    RoleID INT AUTO_INCREMENT,
    RoleName VARCHAR(50) NOT NULL,
    Description VARCHAR(255),

    CONSTRAINT PK_Roles_RoleID
        PRIMARY KEY (RoleID),

    CONSTRAINT UQ_Roles_RoleName
        UNIQUE (RoleName)
) ENGINE=InnoDB;


-- =========================================================
-- 3. ACCOUNTS
-- =========================================================

CREATE TABLE Accounts (
    AccountID INT AUTO_INCREMENT,
    RoleID INT NOT NULL,
    Email VARCHAR(254) NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    FirstName VARCHAR(50) NOT NULL,
    LastName VARCHAR(50) NOT NULL,
    PhoneNumber VARCHAR(20),
    Status VARCHAR(20) NOT NULL,
    CreatedAt DATETIME NOT NULL,

    CONSTRAINT PK_Accounts_AccountID
        PRIMARY KEY (AccountID),

    CONSTRAINT UQ_Accounts_Email
        UNIQUE (Email),

    CONSTRAINT FK_Accounts_RoleID_Roles_RoleID
        FOREIGN KEY (RoleID)
        REFERENCES Roles(RoleID)
) ENGINE=InnoDB;


-- =========================================================
-- 4. ROLE PERMISSIONS
-- =========================================================

CREATE TABLE RolePermissions (
    RoleID INT NOT NULL,
    PermissionID INT NOT NULL,

    CONSTRAINT PK_RolePermissions_RoleID_PermissionID
        PRIMARY KEY (RoleID, PermissionID),

    CONSTRAINT FK_RolePermissions_RoleID_Roles_RoleID
        FOREIGN KEY (RoleID)
        REFERENCES Roles(RoleID),

    CONSTRAINT FK_RolePermissions_PermissionID_Permissions_PermissionID
        FOREIGN KEY (PermissionID)
        REFERENCES Permissions(PermissionID)
) ENGINE=InnoDB;


-- =========================================================
-- 5. MEMBERS
-- =========================================================

CREATE TABLE Members (
    MemberID INT AUTO_INCREMENT,
    AccountID INT NOT NULL,
    TrainingGoal VARCHAR(255),
    Status VARCHAR(20) NOT NULL,

    CONSTRAINT PK_Members_MemberID
        PRIMARY KEY (MemberID),

    CONSTRAINT UQ_Members_AccountID
        UNIQUE (AccountID),

    CONSTRAINT FK_Members_AccountID_Accounts_AccountID
        FOREIGN KEY (AccountID)
        REFERENCES Accounts(AccountID)
) ENGINE=InnoDB;


-- =========================================================
-- 6. COACHES
-- =========================================================

CREATE TABLE Coaches (
    CoachID INT AUTO_INCREMENT,
    AccountID INT NOT NULL,
    Speciality VARCHAR(100),
    Status VARCHAR(20) NOT NULL,

    CONSTRAINT PK_Coaches_CoachID
        PRIMARY KEY (CoachID),

    CONSTRAINT UQ_Coaches_AccountID
        UNIQUE (AccountID),

    CONSTRAINT FK_Coaches_AccountID_Accounts_AccountID
        FOREIGN KEY (AccountID)
        REFERENCES Accounts(AccountID)
) ENGINE=InnoDB;


-- =========================================================
-- 7. RECEPTIONISTS
-- =========================================================

CREATE TABLE Receptionists (
    ReceptionistID INT AUTO_INCREMENT,
    AccountID INT NOT NULL,
    Status VARCHAR(20) NOT NULL,

    CONSTRAINT PK_Receptionists_ReceptionistID
        PRIMARY KEY (ReceptionistID),

    CONSTRAINT UQ_Receptionists_AccountID
        UNIQUE (AccountID),

    CONSTRAINT FK_Receptionists_AccountID_Accounts_AccountID
        FOREIGN KEY (AccountID)
        REFERENCES Accounts(AccountID)
) ENGINE=InnoDB;


-- =========================================================
-- 8. PACKAGES
-- =========================================================

CREATE TABLE Packages (
    PackageID INT AUTO_INCREMENT,
    PackageName VARCHAR(100) NOT NULL,
    Description VARCHAR(500),
    Price DECIMAL(12,2) NOT NULL,
    DurationDays INT NOT NULL,
    Type VARCHAR(30) NOT NULL,

    CONSTRAINT PK_Packages_PackageID
        PRIMARY KEY (PackageID)
) ENGINE=InnoDB;


-- =========================================================
-- 9. SUBSCRIPTIONS
-- =========================================================

CREATE TABLE Subscriptions (
    SubscriptionID INT AUTO_INCREMENT,
    MemberID INT NOT NULL,
    PackageID INT NOT NULL,
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    Status VARCHAR(20) NOT NULL,

    CONSTRAINT PK_Subscriptions_SubscriptionID
        PRIMARY KEY (SubscriptionID),

    CONSTRAINT FK_Subscriptions_MemberID_Members_MemberID
        FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID),

    CONSTRAINT FK_Subscriptions_PackageID_Packages_PackageID
        FOREIGN KEY (PackageID)
        REFERENCES Packages(PackageID)
) ENGINE=InnoDB;


-- =========================================================
-- 10. PAYMENTS
-- =========================================================

CREATE TABLE Payments (
    PaymentID INT AUTO_INCREMENT,
    SubscriptionID INT NOT NULL,
    ReceptionistID INT,
    Amount DECIMAL(12,2) NOT NULL,
    PaymentMethod VARCHAR(30) NOT NULL,
    PaymentDate DATETIME NOT NULL,
    Status VARCHAR(20) NOT NULL,

    CONSTRAINT PK_Payments_PaymentID
        PRIMARY KEY (PaymentID),

    CONSTRAINT FK_Payments_SubscriptionID_Subscriptions_SubscriptionID
        FOREIGN KEY (SubscriptionID)
        REFERENCES Subscriptions(SubscriptionID),

    CONSTRAINT FK_Payments_ReceptionistID_Receptionists_ReceptionistID
        FOREIGN KEY (ReceptionistID)
        REFERENCES Receptionists(ReceptionistID)
) ENGINE=InnoDB;


-- =========================================================
-- 11. SUBJECTS
-- =========================================================

CREATE TABLE Subjects (
    SubjectID INT AUTO_INCREMENT,
    SubjectName VARCHAR(100) NOT NULL,
    Description VARCHAR(500),

    CONSTRAINT PK_Subjects_SubjectID
        PRIMARY KEY (SubjectID)
) ENGINE=InnoDB;


-- =========================================================
-- 12. ROOMS
-- =========================================================

CREATE TABLE Rooms (
    RoomID INT AUTO_INCREMENT,
    RoomName VARCHAR(100) NOT NULL,
    Capacity INT NOT NULL,
    Status VARCHAR(20) NOT NULL,

    CONSTRAINT PK_Rooms_RoomID
        PRIMARY KEY (RoomID)
) ENGINE=InnoDB;


-- =========================================================
-- 13. CLASSES
-- =========================================================

CREATE TABLE Classes (
    ClassID INT AUTO_INCREMENT,
    SubjectID INT NOT NULL,
    CoachID INT NOT NULL,
    RoomID INT NOT NULL,
    ScheduleTime TIME NOT NULL,
    MaxCapacity INT NOT NULL,
    Status VARCHAR(20) NOT NULL,

    CONSTRAINT PK_Classes_ClassID
        PRIMARY KEY (ClassID),

    CONSTRAINT FK_Classes_SubjectID_Subjects_SubjectID
        FOREIGN KEY (SubjectID)
        REFERENCES Subjects(SubjectID),

    CONSTRAINT FK_Classes_CoachID_Coaches_CoachID
        FOREIGN KEY (CoachID)
        REFERENCES Coaches(CoachID),

    CONSTRAINT FK_Classes_RoomID_Rooms_RoomID
        FOREIGN KEY (RoomID)
        REFERENCES Rooms(RoomID)
) ENGINE=InnoDB;


-- =========================================================
-- 14. WORKOUT PLANS
-- =========================================================

CREATE TABLE WorkoutPlans (
    PlanID INT AUTO_INCREMENT,
    PlanName VARCHAR(100) NOT NULL,
    CoachID INT NOT NULL,
    DurationValue INT NOT NULL,
    DurationUnit VARCHAR(10) NOT NULL,

    CONSTRAINT PK_WorkoutPlans_PlanID
        PRIMARY KEY (PlanID),

    CONSTRAINT FK_WorkoutPlans_CoachID_Coaches_CoachID
        FOREIGN KEY (CoachID)
        REFERENCES Coaches(CoachID)
) ENGINE=InnoDB;


-- =========================================================
-- 15. CLASS PLANS
-- =========================================================

CREATE TABLE ClassPlans (
    ClassPlanID INT AUTO_INCREMENT,
    ClassID INT NOT NULL,
    PlanID INT NOT NULL,
    AssignedDate DATE,

    CONSTRAINT PK_ClassPlans_ClassPlanID
        PRIMARY KEY (ClassPlanID),

    CONSTRAINT FK_ClassPlans_ClassID_Classes_ClassID
        FOREIGN KEY (ClassID)
        REFERENCES Classes(ClassID),

    CONSTRAINT FK_ClassPlans_PlanID_WorkoutPlans_PlanID
        FOREIGN KEY (PlanID)
        REFERENCES WorkoutPlans(PlanID)
) ENGINE=InnoDB;


-- =========================================================
-- 16. CLASS ENROLLMENTS
-- =========================================================

CREATE TABLE ClassEnrollments (
    ClassEnrollmentID INT AUTO_INCREMENT,
    ClassID INT NOT NULL,
    MemberID INT NOT NULL,
    EnrollDate DATE NOT NULL,
    Status VARCHAR(20) NOT NULL,

    CONSTRAINT PK_ClassEnrollments_ClassEnrollmentID
        PRIMARY KEY (ClassEnrollmentID),

    CONSTRAINT FK_ClassEnrollments_ClassID_Classes_ClassID
        FOREIGN KEY (ClassID)
        REFERENCES Classes(ClassID),

    CONSTRAINT FK_ClassEnrollments_MemberID_Members_MemberID
        FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID)
) ENGINE=InnoDB;


-- =========================================================
-- 17. CLASS ATTENDANCES
-- =========================================================

CREATE TABLE ClassAttendances (
    AttendanceID INT AUTO_INCREMENT,
    ClassID INT NOT NULL,
    MemberID INT NOT NULL,
    AttendedDate DATE NOT NULL,
    Status VARCHAR(20) NOT NULL,

    CONSTRAINT PK_ClassAttendances_AttendanceID
        PRIMARY KEY (AttendanceID),

    CONSTRAINT FK_ClassAttendances_ClassID_Classes_ClassID
        FOREIGN KEY (ClassID)
        REFERENCES Classes(ClassID),

    CONSTRAINT FK_ClassAttendances_MemberID_Members_MemberID
        FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID)
) ENGINE=InnoDB;


-- =========================================================
-- 18. CENTER CHECK-INS
-- =========================================================

CREATE TABLE CenterCheckIns (
    CheckInID INT AUTO_INCREMENT,
    MemberID INT NOT NULL,
    ReceptionistID INT,
    CheckInTime DATETIME NOT NULL,

    CONSTRAINT PK_CenterCheckIns_CheckInID
        PRIMARY KEY (CheckInID),

    CONSTRAINT FK_CenterCheckIns_MemberID_Members_MemberID
        FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID),

    CONSTRAINT FK_CenterCheckIns_ReceptionistID_Receptionists_ReceptionistID
        FOREIGN KEY (ReceptionistID)
        REFERENCES Receptionists(ReceptionistID)
) ENGINE=InnoDB;


-- =========================================================
-- 19. MEMBER PLAN
-- =========================================================

CREATE TABLE MemberPlan (
    MemberPlanID INT AUTO_INCREMENT,
    MemberID INT NOT NULL,
    PlanID INT NOT NULL,
    CreateDate DATE NOT NULL,
    DueDate DATE,

    CONSTRAINT PK_MemberPlan_MemberPlanID
        PRIMARY KEY (MemberPlanID),

    CONSTRAINT FK_MemberPlan_MemberID_Members_MemberID
        FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID),

    CONSTRAINT FK_MemberPlan_PlanID_WorkoutPlans_PlanID
        FOREIGN KEY (PlanID)
        REFERENCES WorkoutPlans(PlanID)
) ENGINE=InnoDB;


-- =========================================================
-- 20. WORKOUT RESULTS
-- =========================================================

CREATE TABLE WorkoutResults (
    ResultID INT AUTO_INCREMENT,
    MemberID INT NOT NULL,
    SubjectID INT,
    ActivityDetails VARCHAR(1000),
    CoachFeedback VARCHAR(1000),
    LoggedDate DATETIME NOT NULL,

    CONSTRAINT PK_WorkoutResults_ResultID
        PRIMARY KEY (ResultID),

    CONSTRAINT FK_WorkoutResults_MemberID_Members_MemberID
        FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID),

    CONSTRAINT FK_WorkoutResults_SubjectID_Subjects_SubjectID
        FOREIGN KEY (SubjectID)
        REFERENCES Subjects(SubjectID)
) ENGINE=InnoDB;


-- =========================================================
-- 21. NOTIFICATIONS
-- =========================================================

CREATE TABLE Notifications (
    NotificationID INT AUTO_INCREMENT,
    Title VARCHAR(200) NOT NULL,
    Content TEXT NOT NULL,
    SenderID INT NOT NULL,
    SentDate DATETIME NOT NULL,
    TargetType VARCHAR(20),

    CONSTRAINT PK_Notifications_NotificationID
        PRIMARY KEY (NotificationID),

    CONSTRAINT FK_Notifications_SenderID_Accounts_AccountID
        FOREIGN KEY (SenderID)
        REFERENCES Accounts(AccountID)
) ENGINE=InnoDB;


-- =========================================================
-- 22. NOTIFICATION CLASS
-- =========================================================

CREATE TABLE NotificationClass (
    NotificationClassID INT AUTO_INCREMENT,
    NotificationID INT NOT NULL,
    ClassID INT NOT NULL,

    CONSTRAINT PK_NotificationClass_NotificationClassID
        PRIMARY KEY (NotificationClassID),

    CONSTRAINT FK_NotificationClass_NotificationID_Notifications_NotificationID
        FOREIGN KEY (NotificationID)
        REFERENCES Notifications(NotificationID),

    CONSTRAINT FK_NotificationClass_ClassID_Classes_ClassID
        FOREIGN KEY (ClassID)
        REFERENCES Classes(ClassID)
) ENGINE=InnoDB;


-- =========================================================
-- 23. NOTIFICATION ACCOUNT
-- =========================================================

CREATE TABLE NotificationAccount (
    NotificationAccountID INT AUTO_INCREMENT,
    NotificationID INT NOT NULL,
    AccountID INT NOT NULL,

    CONSTRAINT PK_NotificationAccount_NotificationAccountID
        PRIMARY KEY (NotificationAccountID),

    CONSTRAINT FK_NotificationAccount_NotificationID_Notifications_NotificationID
        FOREIGN KEY (NotificationID)
        REFERENCES Notifications(NotificationID),

    CONSTRAINT FK_NotificationAccount_AccountID_Accounts_AccountID
        FOREIGN KEY (AccountID)
        REFERENCES Accounts(AccountID)
) ENGINE=InnoDB;


-- =========================================================
-- 24. SUPPORT TICKETS
-- =========================================================

CREATE TABLE SupportTickets (
    TicketID INT AUTO_INCREMENT,
    MemberID INT NOT NULL,
    ReceptionistID INT,
    Content VARCHAR(1000) NOT NULL,
    CreatedDate DATETIME NOT NULL,
    Status VARCHAR(20) NOT NULL,

    CONSTRAINT PK_SupportTickets_TicketID
        PRIMARY KEY (TicketID),

    CONSTRAINT FK_SupportTickets_MemberID_Members_MemberID
        FOREIGN KEY (MemberID)
        REFERENCES Members(MemberID),

    CONSTRAINT FK_SupportTickets_ReceptionistID_Receptionists_ReceptionistID
        FOREIGN KEY (ReceptionistID)
        REFERENCES Receptionists(ReceptionistID)
) ENGINE=InnoDB;


-- =========================================================
-- 25. AUDIT LOGS
-- =========================================================

CREATE TABLE AuditLogs (
    LogID BIGINT AUTO_INCREMENT,
    AccountID INT NOT NULL,
    ActionType VARCHAR(20) NOT NULL,
    TargetTable VARCHAR(64),
    Details TEXT,
    Timestamp DATETIME NOT NULL,

    CONSTRAINT PK_AuditLogs_LogID
        PRIMARY KEY (LogID),

    CONSTRAINT FK_AuditLogs_AccountID_Accounts_AccountID
        FOREIGN KEY (AccountID)
        REFERENCES Accounts(AccountID)
) ENGINE=InnoDB;