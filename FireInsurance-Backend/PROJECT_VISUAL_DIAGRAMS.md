# 🎨 FIRE INSURANCE SYSTEM - VISUAL DIAGRAMS & FLOW CHARTS

## Project: Fire Insurance Management System Backend
## Visual Guide to Understanding the Complete Flow

---

## 📋 TABLE OF CONTENTS

1. [System Architecture Overview](#system-architecture-overview)
2. [Request Flow Diagram](#request-flow-diagram)
3. [Security Flow](#security-flow)
4. [Database Schema](#database-schema)
5. [API Endpoint Map](#api-endpoint-map)
6. [User Journey Flows](#user-journey-flows)
7. [Component Interaction](#component-interaction)

---

## 🏗️ SYSTEM ARCHITECTURE OVERVIEW

```
┌────────────────────────────────────────────────────────────────────────┐
│                                                                        │
│                        FIRE INSURANCE SYSTEM                            │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                             │
│                         (Angular Frontend - Port 4200)                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │  Login   │  │Customer  │  │  Policy  │  │  Claim   │              │
│  │  Page    │  │Dashboard │  │  Page    │  │  Page    │              │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘              │
└────────────────────────────┬────────────────────────────────────────────┘
                             │ HTTP Requests (JSON)
                             │ JWT Token in Header
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                       SECURITY FILTER LAYER                              │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                        JwtFilter                                   │  │
│  │  - Extracts JWT token from Authorization header                   │  │
│  │  - Validates token signature and expiration                       │  │
│  │  - Sets authentication in SecurityContext                         │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────────────┘
                             │ Authenticated Request
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                        CONTROLLER LAYER (REST APIs)                      │
│                          (Port 8080 - Spring Boot)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │    Auth      │  │  Customer    │  │    Policy    │                 │
│  │ Controller   │  │ Controller   │  │ Controller   │                 │
│  │              │  │              │  │              │                 │
│  │ /api/auth/** │  │/api/customers│  │/api/policies │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
│                                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │    Claim     │  │  Surveyor    │  │  Document    │                 │
│  │ Controller   │  │ Controller   │  │ Controller   │                 │
│  │              │  │              │  │              │                 │
│  │/api/claims/**│  │/api/surveyors│  │/api/documents│                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
└────────────────────────────┬────────────────────────────────────────────┘
                             │ Business Logic Calls
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                         SERVICE LAYER (Business Logic)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │    Auth      │  │  Customer    │  │    Policy    │                 │
│  │   Service    │  │   Service    │  │   Service    │                 │
│  │              │  │              │  │              │                 │
│  │ - Login      │  │ - Register   │  │ - Create     │                 │
│  │ - Register   │  │ - Update     │  │ - Calculate  │                 │
│  │ - Validate   │  │ - GetProfile │  │ - Subscribe  │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
│                                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │    Claim     │  │  Surveyor    │  │  Document    │                 │
│  │   Service    │  │   Service    │  │   Service    │                 │
│  │              │  │              │  │              │                 │
│  │ - FileClaim  │  │ - Assign     │  │ - Upload     │                 │
│  │ - Process    │  │ - Inspect    │  │ - Download   │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
└────────────────────────────┬────────────────────────────────────────────┘
                             │ JPA Operations
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                      REPOSITORY LAYER (Data Access)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │    User      │  │  Customer    │  │    Policy    │                 │
│  │ Repository   │  │ Repository   │  │ Repository   │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
│                                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │    Claim     │  │  Surveyor    │  │  Document    │                 │
│  │ Repository   │  │ Repository   │  │ Repository   │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
└────────────────────────────┬────────────────────────────────────────────┘
                             │ SQL Queries
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATABASE LAYER (H2)                              │
│                      Location: data/demo.mv.db                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │    users     │  │  customers   │  │   policies   │                 │
│  │    table     │  │    table     │  │    table     │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
│                                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │   claims     │  │  surveyors   │  │  documents   │                 │
│  │    table     │  │    table     │  │    table     │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
└─────────────────────────────────────────────────────────────────────────┘

                             ┌──────────────┐
                             │  FILE SYSTEM │
                             │              │
                             │ uploads/     │
                             │ - PDFs       │
                             │ - Documents  │
                             └──────────────┘
```

---

## 🔄 REQUEST FLOW DIAGRAM

### **Example: Customer Login Flow**

```
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 1: User enters credentials                                         │
└─────────────────────────────────────────────────────────────────────────┘
                             │
                             ↓
                    ┌────────────────┐
                    │  FRONTEND      │
                    │  (Angular)     │
                    │                │
                    │  POST Request  │
                    │  /api/auth/    │
                    │  login         │
                    └────────┬───────┘
                             │
                             │ HTTP POST
                             │ Body: { username, password }
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 2: Request enters Spring Boot (Port 8080)                          │
└─────────────────────────────────────────────────────────────────────────┘
                             │
                             ↓
                    ┌────────────────┐
                    │  JwtFilter     │
                    │                │
                    │  Check Path:   │
                    │  /api/auth/*   │
                    │                │
                    │  ✅ Public     │
                    │  Skip JWT      │
                    └────────┬───────┘
                             │
                             │ Pass through
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 3: SecurityConfig allows public access                             │
└─────────────────────────────────────────────────────────────────────────┘
                             │
                             ↓
                    ┌────────────────┐
                    │ Security       │
                    │ Config         │
                    │                │
                    │ .requestMatche │
                    │ rs("/api/auth  │
                    │ /**")          │
                    │ .permitAll()   │
                    └────────┬───────┘
                             │
                             │ Authorized
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 4: AuthController receives request                                 │
└─────────────────────────────────────────────────────────────────────────┘
                             │
                             ↓
                    ┌────────────────┐
                    │ Auth           │
                    │ Controller     │
                    │                │
                    │ @PostMapping   │
                    │ ("/login")     │
                    │                │
                    │ Extract:       │
                    │ - username     │
                    │ - password     │
                    └────────┬───────┘
                             │
                             │ Call authentication
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 5: AuthenticationManager validates credentials                     │
└─────────────────────────────────────────────────────────────────────────┘
                             │
                             ↓
                    ┌────────────────┐
                    │ Authentication │
                    │ Manager        │
                    │                │
                    │ 1. Find user   │
                    │ 2. Check pwd   │
                    │ 3. Validate    │
                    └────────┬───────┘
                             │
                             │ Success
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 6: UserService loads user details                                  │
└─────────────────────────────────────────────────────────────────────────┘
                             │
                             ↓
                    ┌────────────────┐
                    │ UserService    │
                    │                │
                    │ loadUserBy     │
                    │ Username()     │
                    │                │
                    │ Query DB for   │
                    │ user details   │
                    └────────┬───────┘
                             │
                             │ User object returned
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 7: JwtUtil generates token                                         │
└─────────────────────────────────────────────────────────────────────────┘
                             │
                             ↓
                    ┌────────────────┐
                    │ JwtUtil        │
                    │                │
                    │ generateToken  │
                    │ (username,     │
                    │  role)         │
                    │                │
                    │ Creates JWT    │
                    │ with expiry    │
                    └────────┬───────┘
                             │
                             │ JWT Token
                             │ eyJhbGciOiJI...
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ STEP 8: Response sent to frontend                                       │
└─────────────────────────────────────────────────────────────────────────┘
                             │
                             ↓
                    ┌────────────────┐
                    │ Response       │
                    │ Entity         │
                    │                │
                    │ {              │
                    │   "token":     │
                    │   "eyJ..."     │
                    │ }              │
                    │                │
                    │ HTTP 200 OK    │
                    └────────┬───────┘
                             │
                             ↓
                    ┌────────────────┐
                    │  FRONTEND      │
                    │  (Angular)     │
                    │                │
                    │  Store token   │
                    │  in localStorage│
                    │                │
                    │  Redirect to   │
                    │  Dashboard     │
                    └────────────────┘
```

---

## 🔐 SECURITY FLOW

### **JWT Authentication Flow**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         1. USER LOGS IN                                  │
└─────────────────────────────────────────────────────────────────────────┘
                             │
                             ↓
                    ┌────────────────┐
                    │  Username:     │
                    │  john@ex.com   │
                    │  Password:     │
                    │  Pass@123      │
                    └────────┬───────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    2. BACKEND VALIDATES                                  │
└─────────────────────────────────────────────────────────────────────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
                    ↓                 ↓
            ┌──────────────┐  ┌──────────────┐
            │ Check User   │  │ Verify Pwd   │
            │ Exists       │  │ with BCrypt  │
            │              │  │              │
            │ ✅ Found     │  │ ✅ Match     │
            └──────┬───────┘  └──────┬───────┘
                   │                 │
                   └────────┬────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    3. GENERATE JWT TOKEN                                 │
└─────────────────────────────────────────────────────────────────────────┘
                            │
                            ↓
                    ┌──────────────┐
                    │  JWT Token   │
                    │              │
                    │  Header:     │
                    │  {           │
                    │   "alg":"HS256"│
                    │  }           │
                    │              │
                    │  Payload:    │
                    │  {           │
                    │   "sub":"john"│
                    │   "role":"CUSTOMER"│
                    │   "exp":1234567│
                    │  }           │
                    │              │
                    │  Signature:  │
                    │  HMACSHA256( │
                    │   header +   │
                    │   payload +  │
                    │   secret     │
                    │  )           │
                    └──────┬───────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    4. RETURN TOKEN TO USER                               │
└─────────────────────────────────────────────────────────────────────────┘
                           │
                           ↓
                    ┌──────────────┐
                    │  Response:   │
                    │  {           │
                    │   "token":   │
                    │   "eyJhbGc..."│
                    │  }           │
                    └──────┬───────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                5. USER MAKES AUTHENTICATED REQUEST                       │
└─────────────────────────────────────────────────────────────────────────┘
                           │
                           ↓
                    ┌──────────────┐
                    │  GET /api/   │
                    │  customers/me│
                    │              │
                    │  Headers:    │
                    │  Authorization:│
                    │  Bearer      │
                    │  eyJhbGc...  │
                    └──────┬───────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    6. JWTFILTER INTERCEPTS                               │
└─────────────────────────────────────────────────────────────────────────┘
                           │
                           ↓
                    ┌──────────────┐
                    │ JwtFilter    │
                    │              │
                    │ 1. Extract   │
                    │    token     │
                    │              │
                    │ 2. Validate: │
                    │    ✓ Signature│
                    │    ✓ Expiry  │
                    │    ✓ Username│
                    │              │
                    │ 3. Set Auth  │
                    │    in Context│
                    └──────┬───────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    7. AUTHORIZATION CHECK                                │
└─────────────────────────────────────────────────────────────────────────┘
                           │
                           ↓
                    ┌──────────────┐
                    │ @PreAuthorize│
                    │ ("hasRole(   │
                    │  'CUSTOMER')"│
                    │              │
                    │ User Role:   │
                    │ CUSTOMER ✅  │
                    │              │
                    │ Access       │
                    │ GRANTED      │
                    └──────┬───────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    8. PROCESS REQUEST                                    │
└─────────────────────────────────────────────────────────────────────────┘
                           │
                           ↓
                    ┌──────────────┐
                    │ Controller → │
                    │ Service →    │
                    │ Repository → │
                    │ Database     │
                    │              │
                    │ Return Data  │
                    └──────────────┘
```

---

## 🗄️ DATABASE SCHEMA

### **Complete Entity Relationship Diagram**

```
┌────────────────────────────────────────────────────────────────────────┐
│                          USERS TABLE                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ PK  id            BIGINT                                         │   │
│  │     username      VARCHAR(255) UNIQUE NOT NULL                   │   │
│  │     email         VARCHAR(255) UNIQUE NOT NULL                   │   │
│  │     password      VARCHAR(255) NOT NULL (BCrypt encrypted)       │   │
│  │     phone_number  VARCHAR(20)                                    │   │
│  │     roles         VARCHAR(50)  (ADMIN, CUSTOMER, SURVEYOR)       │   │
│  │     active        BOOLEAN      DEFAULT true                      │   │
│  │     created_at    TIMESTAMP                                      │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└───────────────────────────┬────────────────────────────────────────────┘
                            │
                            │ One-to-One
            ┌───────────────┼───────────────┐
            │               │               │
            ↓               ↓               ↓
┌───────────────────┐ ┌───────────────────┐ ┌──────────────┐
│  CUSTOMERS TABLE  │ │  SURVEYORS TABLE  │ │ ADMINS       │
│  ┌─────────────┐  │ │  ┌─────────────┐  │ │ (No table -  │
│  │ PK customer │  │ │  │ PK surveyor │  │ │  just role)  │
│  │    _id      │  │ │  │    _id      │  │ └──────────────┘
│  │ FK user_id  │◄─┤ │  │ FK user_id  │◄─┤
│  │    address  │  │ │  │ license_num │  │
│  │    city     │  │ │  │ experience  │  │
│  │    state    │  │ │  │ assigned_   │  │
│  └─────────────┘  │ │  │ region      │  │
└─────┬─────────────┘ └───────┬───────────┘
      │                       │
      │ One-to-Many           │ One-to-Many
      │                       │
      ↓                       ↓
┌──────────────────┐    ┌─────────────────┐
│ PROPERTIES TABLE │    │ INSPECTIONS     │
│  ┌────────────┐  │    │ TABLE           │
│  │ PK property│  │    │  ┌────────────┐ │
│  │    _id     │  │    │  │ PK inspect │ │
│  │ FK customer│◄─┤    │  │    ion_id  │ │
│  │    _id     │  │    │  │ FK surveyor│◄┤
│  │    address │  │    │  │    _id     │ │
│  │    type    │  │    │  │ FK property│ │
│  │    area_   │  │    │  │    _id     │ │
│  │    sqft    │  │    │  │    status  │ │
│  │    constr_ │  │    │  │    report  │ │
│  │    type    │  │    │  └────────────┘ │
│  │    city    │  │    └─────────────────┘
│  └────────────┘  │
└─────┬────────────┘
      │
      │ Many-to-Many (through POLICY_SUBSCRIPTIONS)
      │
      ↓
┌──────────────────────────────────┐
│ POLICIES TABLE                   │
│  ┌────────────────────────────┐  │
│  │ PK policy_id               │  │
│  │    name                    │  │
│  │    description             │  │
│  │    base_premium            │  │
│  │    coverage_type           │  │
│  │    coverage_amount         │  │
│  │    deductible              │  │
│  │    active                  │  │
│  └────────────────────────────┘  │
└─────────────┬────────────────────┘
              │
              │ One-to-Many
              │
              ↓
┌─────────────────────────────────────┐
│ POLICY_SUBSCRIPTIONS TABLE          │
│  ┌───────────────────────────────┐  │
│  │ PK subscription_id            │  │
│  │ FK customer_id                │◄─┤ Links Customer
│  │ FK policy_id                  │◄─┤ Links Policy
│  │ FK property_id                │◄─┤ Links Property
│  │    start_date                 │  │
│  │    end_date                   │  │
│  │    premium_amount             │  │
│  │    status (ACTIVE/EXPIRED)    │  │
│  │    payment_status             │  │
│  └───────────────────────────────┘  │
└──────────────┬──────────────────────┘
               │
               │ One-to-Many
               │
               ↓
┌─────────────────────────────────────┐
│ CLAIMS TABLE                        │
│  ┌───────────────────────────────┐  │
│  │ PK claim_id                   │  │
│  │ FK subscription_id            │◄─┤
│  │    claim_date                 │  │
│  │    incident_date              │  │
│  │    description                │  │
│  │    claimed_amount             │  │
│  │    approved_amount            │  │
│  │    status (PENDING/APPROVED)  │  │
│  └───────────────────────────────┘  │
└──────────────┬──────────────────────┘
               │
               │ One-to-Many
               │
               ↓
┌─────────────────────────────────────┐
│ CLAIM_INSPECTIONS TABLE             │
│  ┌───────────────────────────────┐  │
│  │ PK inspection_id              │  │
│  │ FK claim_id                   │◄─┤
│  │ FK surveyor_id                │◄─┤
│  │    inspection_date            │  │
│  │    report                     │  │
│  │    recommendation             │  │
│  │    estimated_repair_cost      │  │
│  │    status                     │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ DOCUMENTS TABLE                     │
│  ┌───────────────────────────────┐  │
│  │ PK document_id                │  │
│  │    file_name                  │  │
│  │    file_path                  │  │
│  │    file_type                  │  │
│  │    upload_date                │  │
│  │    uploaded_by (FK user_id)   │  │
│  │    document_type              │  │
│  │    linked_entity_type         │  │
│  │    linked_entity_id           │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### **Table Relationships Summary:**

1. **USER** → **CUSTOMER** (1:1)
2. **USER** → **SURVEYOR** (1:1)
3. **CUSTOMER** → **PROPERTY** (1:Many)
4. **PROPERTY** → **INSPECTION** (1:Many)
5. **SURVEYOR** → **INSPECTION** (1:Many)
6. **CUSTOMER** + **POLICY** + **PROPERTY** → **SUBSCRIPTION** (Many:Many)
7. **SUBSCRIPTION** → **CLAIM** (1:Many)
8. **CLAIM** + **SURVEYOR** → **CLAIM_INSPECTION** (1:Many)

---

## 🗺️ API ENDPOINT MAP

### **Public Endpoints (No Authentication)**

```
┌────────────────────────────────────────────────────────────┐
│                    PUBLIC ENDPOINTS                         │
│                  (No JWT Token Required)                    │
└────────────────────────────────────────────────────────────┘

POST   /api/auth/login
       ├─ Body: { username, password }
       └─ Returns: { token }

POST   /api/auth/register/customer
       ├─ Body: { username, email, password, address, city, state }
       └─ Returns: CustomerRegistrationResponse

POST   /api/auth/register/surveyor
       ├─ Body: { username, email, password, licenseNumber, experience }
       └─ Returns: SurveyorRegistrationResponse

POST   /api/quotes
       ├─ Body: { propertyType, areaSqft, constructionType, city }
       └─ Returns: { quoteId, estimatedPremium }

GET    /api/policies
       └─ Returns: List of available policies
```

### **Customer Endpoints**

```
┌────────────────────────────────────────────────────────────┐
│                  CUSTOMER ENDPOINTS                         │
│         (Requires JWT with CUSTOMER role)                   │
└────────────────────────────────────────────────────────────┘

GET    /api/customers/me
       └─ Returns: Current customer's profile

PUT    /api/customers/me
       ├─ Body: { address, city, state, phoneNumber }
       └─ Returns: Updated customer profile

GET    /api/properties/my
       └─ Returns: List of customer's properties

POST   /api/properties
       ├─ Body: { address, type, areaSqft, constructionType }
       └─ Returns: Created property

GET    /api/subscriptions/my
       └─ Returns: Customer's policy subscriptions

POST   /api/subscriptions
       ├─ Body: { policyId, propertyId, startDate }
       └─ Returns: Created subscription

POST   /api/claims
       ├─ Body: { subscriptionId, description, claimedAmount }
       └─ Returns: Created claim

GET    /api/claims/my
       └─ Returns: Customer's claims
```

### **Admin Endpoints**

```
┌────────────────────────────────────────────────────────────┐
│                    ADMIN ENDPOINTS                          │
│           (Requires JWT with ADMIN role)                    │
└────────────────────────────────────────────────────────────┘

GET    /api/customers
       └─ Returns: List of all customers

GET    /api/customers/{id}
       └─ Returns: Specific customer details

DELETE /api/customers/{id}
       └─ Deletes customer

GET    /api/surveyors
       └─ Returns: List of all surveyors

POST   /api/policies
       ├─ Body: { name, description, basePremium, coverageType }
       └─ Returns: Created policy

GET    /api/quotes
       └─ Returns: All quote requests

GET    /api/claims
       └─ Returns: All claims in system

PUT    /api/claims/{id}/status
       ├─ Body: { status, approvedAmount }
       └─ Returns: Updated claim
```

### **Surveyor Endpoints**

```
┌────────────────────────────────────────────────────────────┐
│                  SURVEYOR ENDPOINTS                         │
│         (Requires JWT with SURVEYOR role)                   │
└────────────────────────────────────────────────────────────┘

GET    /api/inspections/assigned
       └─ Returns: Inspections assigned to surveyor

POST   /api/inspections
       ├─ Body: { propertyId, inspectionDate, report }
       └─ Returns: Created inspection

PUT    /api/inspections/{id}
       ├─ Body: { report, status }
       └─ Returns: Updated inspection

GET    /api/claims/assigned
       └─ Returns: Claims assigned for inspection

POST   /api/claim-inspections
       ├─ Body: { claimId, report, estimatedCost }
       └─ Returns: Created claim inspection
```

---

## 👤 USER JOURNEY FLOWS

### **Journey 1: New Customer Registration & Policy Purchase**

```
START
  │
  ├─> Step 1: Visit Homepage
  │   │
  │   ├─> Fill Quote Form
  │   │   (propertyType, area, city)
  │   │
  │   └─> POST /api/quotes
  │       Response: "Estimated Premium: $500/year"
  │
  ├─> Step 2: Decide to Register
  │   │
  │   ├─> Fill Registration Form
  │   │   (username, email, password, address)
  │   │
  │   └─> POST /api/auth/register/customer
  │       Response: "Registration Successful"
  │
  ├─> Step 3: Login
  │   │
  │   ├─> Enter credentials
  │   │
  │   └─> POST /api/auth/login
  │       Response: { token: "eyJhbGc..." }
  │       Store token in localStorage
  │
  ├─> Step 4: View Dashboard
  │   │
  │   └─> GET /api/customers/me
  │       (with JWT token in header)
  │       Response: Customer profile
  │
  ├─> Step 5: Add Property
  │   │
  │   ├─> Fill Property Form
  │   │   (address, type, area, construction)
  │   │
  │   └─> POST /api/properties
  │       (with JWT token)
  │       Response: Property created
  │
  ├─> Step 6: Browse Policies
  │   │
  │   └─> GET /api/policies
  │       Response: [Gold Plan, Silver Plan, Bronze Plan]
  │
  ├─> Step 7: Subscribe to Policy
  │   │
  │   ├─> Select Gold Plan
  │   │   Select Property
  │   │
  │   └─> POST /api/subscriptions
  │       (with JWT token)
  │       Body: { policyId, propertyId, startDate }
  │       Response: Subscription created
  │
  └─> Step 8: View Active Policies
      │
      └─> GET /api/subscriptions/my
          (with JWT token)
          Response: Active subscriptions
END
```

### **Journey 2: Filing and Processing Insurance Claim**

```
START (Customer has active subscription)
  │
  ├─> Step 1: Incident Occurs
  │   │
  │   └─> Fire damages property
  │
  ├─> Step 2: Customer Files Claim
  │   │
  │   ├─> Login and go to Claims section
  │   │
  │   ├─> Fill Claim Form
  │   │   - Select subscription
  │   │   - Describe incident
  │   │   - Claim amount: $10,000
  │   │   - Upload photos
  │   │
  │   └─> POST /api/claims
  │       (with JWT token)
  │       Response: Claim created (Status: PENDING)
  │
  ├─> Step 3: Admin Reviews Claim
  │   │
  │   ├─> Admin logs in
  │   │
  │   ├─> GET /api/claims
  │   │   (with ADMIN JWT token)
  │   │   Response: All claims
  │   │
  │   └─> Admin assigns Surveyor
  │       PUT /api/claims/{id}/assign
  │       Body: { surveyorId }
  │
  ├─> Step 4: Surveyor Inspects Property
  │   │
  │   ├─> Surveyor logs in
  │   │
  │   ├─> GET /api/claims/assigned
  │   │   (with SURVEYOR JWT token)
  │   │   Response: Assigned claims
  │   │
  │   ├─> Surveyor visits property
  │   │
  │   └─> POST /api/claim-inspections
  │       (with SURVEYOR JWT token)
  │       Body: {
  │         claimId,
  │         report: "Fire damage confirmed",
  │         estimatedCost: 8500,
  │         recommendation: "APPROVE"
  │       }
  │
  ├─> Step 5: Admin Approves Claim
  │   │
  │   ├─> Admin reviews inspection report
  │   │
  │   └─> PUT /api/claims/{id}/status
  │       (with ADMIN JWT token)
  │       Body: {
  │         status: "APPROVED",
  │         approvedAmount: 8500
  │       }
  │
  └─> Step 6: Customer Notified
      │
      └─> Customer checks claim status
          GET /api/claims/my
          (with CUSTOMER JWT token)
          Response: Claim approved $8,500
END
```

---

## 🔄 COMPONENT INTERACTION

### **How Components Talk to Each Other**

```
┌─────────────────────────────────────────────────────────────────┐
│                  AUTHENTICATION FLOW                             │
└─────────────────────────────────────────────────────────────────┘

  AuthController
       │
       ├─> calls
       │
       ↓
  AuthenticationManager
       │
       ├─> delegates to
       │
       ↓
  UserDetailsService (UserService)
       │
       ├─> queries
       │
       ↓
  UserRepository
       │
       ├─> executes SQL
       │
       ↓
  Database (users table)
       │
       └─> returns User object

Back up the chain:
  Database → UserRepository → UserService → AuthenticationManager
       │
       └─> AuthController receives authenticated User
               │
               └─> JwtUtil generates token
                       │
                       └─> Return token to client


┌─────────────────────────────────────────────────────────────────┐
│               CUSTOMER PROFILE UPDATE FLOW                       │
└─────────────────────────────────────────────────────────────────┘

  Client sends: PUT /api/customers/me
  Header: Authorization: Bearer <token>
  Body: { address: "New Address", city: "New City" }
       │
       ↓
  JwtFilter intercepts
       ├─> Validates token
       ├─> Extracts username from token
       └─> Sets Authentication in SecurityContext
       │
       ↓
  SecurityConfig checks
       ├─> Endpoint requires CUSTOMER role
       ├─> User has CUSTOMER role ✅
       └─> Passes to controller
       │
       ↓
  CustomerController.updateMyProfile()
       ├─> Gets username from Authentication object
       ├─> Calls customerService.updateCustomerProfile()
       │
       ↓
  CustomerService
       ├─> Finds User by username (via UserRepository)
       ├─> Finds Customer by User (via CustomerRepository)
       ├─> Updates customer fields
       ├─> Saves customer (via CustomerRepository)
       │
       ↓
  CustomerRepository
       ├─> Generates SQL: UPDATE customers SET ...
       ├─> Executes in database
       │
       ↓
  Database updates record
       │
       └─> Returns updated Customer

Back up the chain:
  Database → CustomerRepository → CustomerService → CustomerController
       │
       └─> ResponseEntity with updated customer returned to client


┌─────────────────────────────────────────────────────────────────┐
│                    CLAIM FILING FLOW                             │
└─────────────────────────────────────────────────────────────────┘

  Client: POST /api/claims
  Body: { subscriptionId, description, claimedAmount }
       │
       ↓
  ClaimController
       │
       ├─> Validates request
       ├─> Calls claimService.createClaim()
       │
       ↓
  ClaimService
       ├─> Validates subscriptionId exists
       │   (via SubscriptionRepository)
       │
       ├─> Creates Claim object
       │   - claimDate = now()
       │   - status = PENDING
       │   - links to subscription
       │
       ├─> Saves claim (via ClaimRepository)
       │
       ↓
  ClaimRepository
       ├─> INSERT INTO claims ...
       │
       ↓
  Database
       │
       └─> Claim record created

  Notification flow (if implemented):
       │
       ├─> NotificationService sends email to admin
       └─> NotificationService sends SMS to customer
```

---

## 📱 SWAGGER UI WORKFLOW

```
┌─────────────────────────────────────────────────────────────────┐
│              TESTING API WITH SWAGGER                            │
└─────────────────────────────────────────────────────────────────┘

1. Open Browser
   └─> http://localhost:8080/swagger-ui.html

2. Find "auth-controller"
   └─> POST /api/auth/login

3. Click "Try it out"
   └─> Enter credentials:
       {
         "username": "admin",
         "password": "Admin@123"
       }

4. Click "Execute"
   └─> Response:
       {
         "token": "eyJhbGciOiJIUzI1NiJ9..."
       }

5. Copy the token (without quotes)
   └─> Click "Authorize" button (top right)
       └─> Paste token
           └─> Click "Authorize"
               └─> Click "Close"

6. Now all requests will include token
   └─> Try protected endpoint:
       └─> GET /api/customers/me
           └─> Should return your profile ✅

7. Try admin-only endpoint:
   └─> GET /api/customers
       └─> Should list all customers (ADMIN only) ✅

8. Try with wrong role:
   └─> Login as CUSTOMER
       └─> Try GET /api/surveyors
           └─> Should get 403 Forbidden ❌
```

---

## 🎯 SUMMARY DIAGRAM

```
╔═══════════════════════════════════════════════════════════════╗
║         FIRE INSURANCE SYSTEM - COMPLETE OVERVIEW             ║
╠═══════════════════════════════════════════════════════════════╣
║                                                               ║
║  USERS                                                        ║
║  ├─ ADMIN      → Manage everything                           ║
║  ├─ CUSTOMER   → Buy policies, file claims                   ║
║  └─ SURVEYOR   → Inspect properties, evaluate claims         ║
║                                                               ║
║  MAIN FLOWS                                                   ║
║  ├─ Registration & Login → JWT Token                         ║
║  ├─ Policy Purchase → Subscription                           ║
║  ├─ Claim Filing → Inspection → Approval                     ║
║  └─ Document Upload → Storage → Retrieval                    ║
║                                                               ║
║  SECURITY                                                     ║
║  ├─ JWT Authentication                                        ║
║  ├─ Role-Based Authorization                                 ║
║  ├─ Password Encryption (BCrypt)                             ║
║  └─ CORS Protection                                           ║
║                                                               ║
║  DATABASE                                                     ║
║  ├─ Users, Customers, Surveyors                              ║
║  ├─ Properties, Policies, Subscriptions                      ║
║  ├─ Claims, Inspections                                      ║
║  └─ Documents                                                 ║
║                                                               ║
║  ARCHITECTURE                                                 ║
║  Frontend (Angular) ↔ REST API (Spring Boot) ↔ Database (H2) ║
║                                                               ║
╚═══════════════════════════════════════════════════════════════╝
```

---

**Document Created:** March 12, 2026  
**Project:** Fire Insurance Management System  
**Type:** Visual Diagrams & Flow Charts  
**Version:** 1.0  

---

**THIS DOCUMENT IS DOWNLOADABLE** - Contains all visual representations of your system architecture, flows, and interactions.


