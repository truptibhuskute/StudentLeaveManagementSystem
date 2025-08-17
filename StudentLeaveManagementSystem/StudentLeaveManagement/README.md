# Student Leave Management System

A comprehensive Java-based console application for managing student leave requests in educational institutions. This project demonstrates advanced Java programming concepts, database integration, and enterprise-level software architecture.

## 🎯 Project Overview

The Student Leave Management System is designed to streamline the process of leave applications, approvals, and management in educational institutions. It provides separate interfaces for students and administrators with role-based access control.

## ✨ Key Features

### Student Features
- **User Registration & Authentication**: Secure student registration and login system
- **Leave Application**: Apply for various types of leaves with proper validation
- **Leave Tracking**: View status and history of all leave applications
- **Leave Management**: Cancel pending or future approved leaves
- **Profile Management**: Update personal information and change passwords

### Admin Features
- **Dashboard Overview**: Comprehensive system metrics and pending requests
- **Leave Approval Workflow**: Approve or reject leave requests with comments
- **Student Management**: View, search, and manage student accounts
- **Reporting & Analytics**: Generate detailed reports and statistics
- **User Management**: Manage admin accounts and permissions (role-based)
- **System Administration**: Full system control for super admins

### System Features
- **Role-Based Access Control**: Multiple admin roles with different permission levels
- **Data Validation**: Comprehensive input validation and error handling
- **Audit Trail**: Complete history tracking of all leave operations
- **Business Logic**: Enforced business rules and constraints
- **Connection Pooling**: Efficient database connection management
- **Reporting**: Advanced analytics and reporting capabilities

## 🏗️ Technical Architecture

### Design Patterns Used
- **Model-View-Controller (MVC)**: Clear separation of concerns
- **Service Layer Pattern**: Business logic abstraction
- **Data Access Object (DAO)**: Database access abstraction
- **Singleton Pattern**: Database connection management
- **Factory Pattern**: Object creation and management

### Technologies & Frameworks
- **Language**: Java 17+
- **Database**: MySQL 8.0+
- **Database Connectivity**: JDBC with connection pooling
- **Architecture**: Layered architecture with service layer
- **Design**: Object-Oriented Programming principles


 🚀 Getting Started
 Prerequisites
- Java Development Kit (JDK) 17 or higher
- MySQL Server 8.0 or higher
- MySQL JDBC Driver (mysql-connector-java)

### Installation Steps

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd StudentLeaveManagement
   ```

2. **Database Setup**
   ```bash
   # Start MySQL server
   mysql -u root -p
   
   # Execute the schema script
   source database/schema.sql
   ```

3. **Configure Database Connection**
   ```properties
   # Edit src/main/resources/database.properties
   db.url=jdbc:mysql://localhost:3306/student_leave_db
   db.username=your_username
   db.password=your_password
   ```

4. **Add MySQL JDBC Driver**
   - Download MySQL Connector/J from the official website
   - Add the JAR file to your classpath

5. **Compile and Run**
   ```bash
   # Compile the project
   javac -cp ".:mysql-connector-java-8.0.33.jar" src/main/java/com/studentleave/*.java src/main/java/com/studentleave/*/*.java
   
   # Run the application
   java -cp ".:mysql-connector-java-8.0.33.jar:src/main/java" com.studentleave.StudentLeaveManagementApp
   ```

## 📊 Database Schema

The system uses a well-designed relational database schema:

### Core Tables
- **students**: Student information and credentials
- **admins**: Administrator accounts with role-based permissions
- **leaves**: Leave applications with status tracking
- **leave_history**: Audit trail for all leave operations

### Reference Tables
- **departments**: Department information
- **leave_types_config**: Configurable leave types
- **system_settings**: Application configuration

### Views
- **active_leaves**: Currently active leave applications
- **pending_leaves**: Leaves awaiting approval
- **leave_statistics**: Departmental leave statistics

## 🔐 User Roles & Permissions

### Admin Roles (Hierarchical)
1. **Super Admin** (Level 5): Full system access
2. **Department Head** (Level 4): Department management
3. **Academic Coordinator** (Level 3): Leave approvals
4. **Assistant Admin** (Level 2): Reports and student management
5. **Viewer** (Level 1): Read-only access

### Permission Matrix
| Feature | Viewer | Assistant | Coordinator | Dept Head | Super Admin |
|---------|--------|-----------|-------------|-----------|-------------|
| View Dashboard | ✓ | ✓ | ✓ | ✓ | ✓ |
| Approve Leaves | ✗ | ✗ | ✓ | ✓ | ✓ |
| View Reports | ✗ | ✓ | ✓ | ✓ | ✓ |
| Manage Users | ✗ | ✗ | ✗ | ✓ | ✓ |
| System Admin | ✗ | ✗ | ✗ | ✗ | ✓ |

## 🎮 Usage Guide

### Student Workflow
1. Register with student credentials
2. Login to access the student dashboard
3. Apply for leave with required details
4. Track application status
5. Cancel leaves if needed
6. View leave history and statistics

### Admin Workflow
1. Login with admin credentials
2. View dashboard for system overview
3. Process pending leave requests
4. Generate reports and analytics
5. Manage students and other admins
6. Configure system settings

### Default Accounts (Demo)
```
Admin:
- ID: ADMIN001
- Password: admin123
- Role: Super Admin

Students: (Use for testing)
- ID: CS2021001, Password: student123
- ID: CS2021002, Password: student123
```

## 📈 Business Rules & Validations

### Leave Application Rules
- Minimum 1-day advance notice required
- Maximum 30 days per single application
- Cannot apply for past dates
- Comprehensive reason required (min 10 characters)

### Data Validations
- Email format validation
- Password strength requirements
- Date range validations
- Duplicate prevention
- Business constraint enforcement

## 🔧 Configuration

### Database Configuration
```properties
# Connection settings
db.url=jdbc:mysql://localhost:3306/student_leave_db
db.username=root
db.password=your_password

# Pool settings
db.pool.initialSize=5
db.pool.maxActive=20
```

### Application Settings
```properties
# Security
security.password.minLength=6
security.session.timeout=3600

# Business rules
app.maxLeaveDuration=30
app.advanceNoticeDays=1
```

## 📋 System Requirements

### Minimum Requirements
- Java Runtime Environment (JRE) 17+
- MySQL Server 8.0+
- 512 MB RAM
- 100 MB disk space

### Recommended Requirements
- Java Development Kit (JDK) 17+
- MySQL Server 8.0+ with InnoDB engine
- 1 GB RAM
- 500 MB disk space

## 🚦 Error Handling

The system implements comprehensive error handling:

### Exception Types
- **ValidationException**: Input validation errors
- **DatabaseException**: Database operation failures
- **AuthenticationException**: Login/authentication errors
- **AuthorizationException**: Permission-related errors
- **BusinessLogicException**: Business rule violations

### Error Recovery
- Graceful error messages for users
- Automatic transaction rollback
- Connection pool management
- Detailed logging for debugging

## 📊 Reporting Features

### Available Reports
1. **Department Statistics**: Leave patterns by department
2. **Monthly Summary**: Month-wise leave analysis
3. **Student Reports**: Individual student leave history
4. **Pending Approvals**: Current approval queue
5. **Trend Analysis**: Historical trends and patterns

### Report Formats
- Console-based tabular display
- Statistical summaries
- Detailed breakdowns
- Interactive drill-down options

## 🔒 Security Features

### Authentication & Authorization
- Password-based authentication
- Role-based access control
- Session management
- Permission validation

### Data Security
- Input sanitization
- SQL injection prevention
- Data validation at multiple layers
- Secure password handling

### Best Practices Implemented
- Prepared statements for database queries
- Input validation and sanitization
- Error message standardization
- Logging for audit purposes

### Code Quality
- Comprehensive documentation
- Consistent naming conventions
- Modular design principles
- Error handling best practices

## 🚀 Future Enhancements

### Planned Features
1. Web Interface: Spring Boot web application
2. Email Notifications: Automated email alerts
3. File Attachments: Support for leave documents
4. Mobile App: Android/iOS mobile applications
5. API Integration: RESTful web services
6. Advanced Reporting: Charts and visualizations
7. Calendar Integration: Calendar view of leaves
8. Workflow Engine: Configurable approval workflows

### Technical Improvements
- Microservices architecture
- Cloud deployment support
- Performance optimization
- Enhanced security features
- Internationalization support


## 📞 Support & Contact

For questions, suggestions, or support:
- Create an issue in the repository
- Review the documentation
- Check the code comments for implementation details

---

## 🎓 Learning Outcomes

### Core Java Concepts
- Object-Oriented Programming (OOP)
- Encapsulation, Inheritance, and Polymorphism
- Abstract classes and Interfaces
- Exception handling and custom exceptions
- Collections Framework and Generics
- Enums and utility classes
- Date and time handling (LocalDate, LocalDateTime)

### Database Integration
- JDBC connectivity and operations
- PreparedStatement and SQL injection prevention
- Connection pooling and resource management
- Database transaction handling
- Complex SQL queries and joins
- Database schema design and normalization

### Software Engineering Practices
- Layered architecture implementation
- Design patterns (MVC, Service Layer, Singleton)
- Code organization and package structure
- Input validation and error handling
- Logging and debugging techniques
- Configuration management

### Enterprise Development Skills
- Business logic implementation
- User authentication and authorization
- Role-based access control
- Data validation and constraints
- Reporting and analytics
- Audit trail and history tracking



