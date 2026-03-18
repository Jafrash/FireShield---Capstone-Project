# 🎨 ER Diagram - Quick Visual Guide

## Fire Insurance Management System

---

## 📊 DATABASE OVERVIEW

**Total Entities:** 10  
**Total Relationships:** 18  
**Database Type:** Relational (MySQL/PostgreSQL/H2)  
**ORM:** JPA/Hibernate with Spring Boot

---

## 🏗️ ENTITY HIERARCHY

```
Level 1: AUTHENTICATION
         └── USER (role: ADMIN/CUSTOMER/SURVEYOR)
                    ├────────────────┬────────────────┐
                    ↓                ↓                ↓
Level 2: PROFILES   
         CUSTOMER            SURVEYOR          (ADMIN has no profile)
            |                    |
            ↓                    ↓
Level 3: ASSETS & SERVICES
         PROPERTY            [Conducts Inspections]
            |                    |
            ├────────────────────┤
            ↓                    ↓
Level 4: BUSINESS PROCESSES
         INSPECTION      CLAIM_INSPECTION
            |                    |
            ↓                    ↓
Level 5: CORE BUSINESS
    POLICY_SUBSCRIPTION    CLAIM
         (active policy)   (incident)
            |
            ↓
Level 6: CATALOG
         POLICY (templates)

Everywhere: DOCUMENT (flexible linking)
```

---

## 🔗 RELATIONSHIP MAP

### **Central Hub: USER**
```
         ┌─────────┐
         │  USER   │ (Authentication)
         └────┬────┘
              │
     ┌────────┴────────┐
     │                 │
     ↓                 ↓
 [CUSTOMER]       [SURVEYOR]
     │                 │
     │                 └─→ Conducts INSPECTION
     │                 └─→ Conducts CLAIM_INSPECTION
     │
     ├─→ Owns PROPERTY
     ├─→ Has POLICY_SUBSCRIPTION
     ├─→ Files CLAIM
     └─→ Uploads DOCUMENT
```

### **Property Insurance Flow**
```
CUSTOMER 
   │
   └─→ PROPERTY
          │
          └─→ INSPECTION (by SURVEYOR)
                 │
                 └─→ POLICY_SUBSCRIPTION ←── POLICY (template)
                        │
                        └─→ Can have CLAIM
                               │
                               └─→ CLAIM_INSPECTION (by SURVEYOR)
```

### **Renewal Chain (Self-Reference)**
```
POLICY_SUBSCRIPTION (Year 1, ID=1)
   │
   └─→ previous_subscription_id = NULL
       renewal_count = 0
       
       ↓ (expires)
       
POLICY_SUBSCRIPTION (Year 2, ID=5)
   │
   └─→ previous_subscription_id = 1
       renewal_count = 1
       
       ↓ (expires)
       
POLICY_SUBSCRIPTION (Year 3, ID=12)
   │
   └─→ previous_subscription_id = 5
       renewal_count = 2
```

---

## 📋 ENTITY COLOR CODING

| Entity | Color | Purpose |
|--------|-------|---------|
| **USER** | 🔵 Blue | Authentication core |
| **CUSTOMER** | 🟢 Green | End-user profile |
| **SURVEYOR** | 🟠 Orange | Assessor profile |
| **PROPERTY** | 🟡 Yellow | Insurable asset |
| **POLICY** | 🟣 Purple | Policy catalog/templates |
| **POLICY_SUBSCRIPTION** | 🔵 Teal | Active policy instance |
| **INSPECTION** | 🔷 Light Blue | Property assessment |
| **CLAIM** | 🔴 Red | Insurance claim |
| **CLAIM_INSPECTION** | 🔺 Pink | Damage assessment |
| **DOCUMENT** | ⚫ Gray | File metadata |

---

## 🎯 KEY RELATIONSHIPS EXPLAINED

### **1. One-to-One Relationships (4)**

```sql
USER ←→ CUSTOMER
  One user can be one customer only

USER ←→ SURVEYOR
  One user can be one surveyor only

CLAIM ←→ CLAIM_INSPECTION
  One claim has one inspection

POLICY_SUBSCRIPTION ←→ INSPECTION
  One subscription requires one inspection (optional)
```

### **2. One-to-Many Relationships (14)**

```sql
CUSTOMER → PROPERTY (1:N)
  One customer owns many properties

PROPERTY → INSPECTION (1:N)
  One property can have multiple inspections

SURVEYOR → INSPECTION (1:N)
  One surveyor conducts many inspections

POLICY → POLICY_SUBSCRIPTION (1:N)
  One policy template creates many subscriptions

POLICY_SUBSCRIPTION → CLAIM (1:N)
  One subscription can have many claims

SURVEYOR → CLAIM_INSPECTION (1:N)
  One surveyor conducts many claim inspections

CUSTOMER → DOCUMENT (1:N, optional)
PROPERTY → DOCUMENT (1:N, optional)
CLAIM → DOCUMENT (1:N, optional)
SURVEYOR → DOCUMENT (1:N, optional)
  Documents can link to multiple entities
```

### **3. Self-Referencing Relationship (1)**

```sql
POLICY_SUBSCRIPTION → POLICY_SUBSCRIPTION (renewal chain)
  previous_subscription_id links to parent subscription
```

---

## 📊 CARDINALITY NOTATION

Using **Crow's Foot Notation**:

```
|     = Exactly one
o     = Zero or one (optional)
>─    = One or many
o>─   = Zero or many (optional)
```

**Examples:**
```
CUSTOMER |────>─ PROPERTY
  (One customer owns one or many properties)

CLAIM |────| CLAIM_INSPECTION
  (One claim has exactly one inspection)

POLICY_SUBSCRIPTION o────| INSPECTION
  (One subscription optionally has one inspection)
```

---

## 🔍 IMPORTANT FIELDS

### **USER** (Authentication)
- `user_id` (PK)
- `username` (UNIQUE, NOT NULL)
- `email` (UNIQUE, NOT NULL)
- `role` (ADMIN/CUSTOMER/SURVEYOR)

### **POLICY_SUBSCRIPTION** (Most Complex)
- `subscription_id` (PK)
- `status` (8 states: REQUESTED → ACTIVE)
- `premium_amount` (risk-adjusted)
- `risk_score` (from inspection)
- `renewal_count` (tracks renewal chain)
- `ncb_discount` (No Claim Bonus)
- `previous_subscription_id` (FK, self-reference)

### **CLAIM** (Incident Management)
- `claim_id` (PK)
- `status` (7 states: SUBMITTED → SETTLED)
- `fraud_score` (AI detection)
- `settlement_amount` (final payout)

### **DOCUMENT** (Flexible Linking)
- `document_id` (PK)
- `claim_id` (FK, nullable)
- `property_id` (FK, nullable)
- `customer_id` (FK, nullable)
- `surveyor_id` (FK, nullable)
- `document_stage` (POLICY/INSPECTION/CLAIM)

---

## 🎭 ENUM STATUS FLOWS

### **Policy Subscription Lifecycle**
```
REQUESTED → PENDING → INSPECTING → INSPECTED → ACTIVE
                                              ↓
                                           EXPIRED
                                              ↓
                                          (renewal)
```

### **Claim Processing Flow**
```
SUBMITTED → UNDER_REVIEW → INSPECTING → INSPECTED → APPROVED → SETTLED
                                                   ↓
                                                REJECTED
```

### **Inspection Status**
```
ASSIGNED → COMPLETED
        ↓
      REJECTED
```

---

## 🔐 SECURITY & ACCESS

### **Role-Based Relationships**

**ADMIN:**
- Full access to all entities
- No profile extension

**CUSTOMER:**
- Owns PROPERTY
- Creates POLICY_SUBSCRIPTION
- Files CLAIM
- Uploads DOCUMENT (policy/claim stage)

**SURVEYOR:**
- Conducts INSPECTION
- Conducts CLAIM_INSPECTION
- Uploads DOCUMENT (inspection stage)

---

## 📈 DATA VOLUME EXPECTATIONS

| Entity | Expected Volume | Growth Rate |
|--------|----------------|-------------|
| USER | Thousands | Moderate |
| CUSTOMER | Thousands | Moderate |
| SURVEYOR | Hundreds | Low |
| PROPERTY | Tens of thousands | High |
| POLICY | Dozens | Very Low |
| POLICY_SUBSCRIPTION | Hundreds of thousands | High |
| INSPECTION | Hundreds of thousands | High |
| CLAIM | Tens of thousands | Moderate |
| CLAIM_INSPECTION | Tens of thousands | Moderate |
| DOCUMENT | Millions | Very High |

---

## 🎨 VISUAL DIAGRAM FEATURES

### **Must Include:**
✅ Entity boxes with all attributes  
✅ Primary Keys underlined  
✅ Foreign Keys marked (FK)  
✅ Relationship lines with cardinality  
✅ Crow's foot notation  
✅ Color-coded entities  
✅ Enum value annotations  
✅ Self-reference arrow for renewal  
✅ Dashed lines for optional relationships  
✅ Legend/key  

### **Layout Tips:**
- Place USER at the top center
- Group related entities (CUSTOMER-PROPERTY-INSPECTION)
- Keep POLICY_SUBSCRIPTION in the center (most connections)
- Show DOCUMENT on the side (links to many)
- Use curved arrow for self-reference

---

## 🛠️ HOW TO CREATE THE DIAGRAM

### **Option 1: Online Tool (Fastest)**
1. Go to https://dbdiagram.io/d
2. Copy the content from `database_schema.dbml`
3. Paste into the editor
4. ✨ Instant ER diagram!
5. Export as PNG/PDF

### **Option 2: Manual Drawing**
1. Open Draw.io (https://app.diagrams.net/)
2. Use Entity Relationship template
3. Follow the specifications in `ER_DIAGRAM_PROMPT.md`
4. Apply colors and styling
5. Export as high-resolution image

### **Option 3: Database Tool**
1. Open MySQL Workbench or DBeaver
2. Connect to your database
3. Use reverse engineering feature
4. Auto-generate EER diagram
5. Customize and export

---

## 📚 RELATED FILES

1. **ER_DIAGRAM_PROMPT.md** - Complete detailed specifications (10+ pages)
2. **database_schema.dbml** - Ready-to-use DBML code for dbdiagram.io
3. **This file** - Quick visual reference guide

---

## 🎯 BUSINESS CONTEXT

### **What This System Does:**

**Pre-Policy:**
1. Customer registers (USER → CUSTOMER)
2. Customer adds property (PROPERTY)
3. Surveyor inspects property (INSPECTION)
4. Risk score determines premium
5. Customer subscribes to policy (POLICY_SUBSCRIPTION)

**Active Policy:**
1. Policy is ACTIVE
2. Tracks renewal eligibility
3. Tracks No Claim Bonus (NCB)

**Claim Processing:**
1. Incident occurs
2. Customer files claim (CLAIM)
3. Surveyor inspects damage (CLAIM_INSPECTION)
4. Claim approved/rejected
5. Settlement processed

**Renewal:**
1. Policy expires
2. System creates new subscription
3. Links to previous via `previous_subscription_id`
4. Applies NCB discount if claim-free

---

## 🚀 QUICK START

```bash
# Step 1: Open dbdiagram.io
https://dbdiagram.io/d

# Step 2: Load schema
Copy content from database_schema.dbml

# Step 3: Generate diagram
Paste into editor → Auto-generates

# Step 4: Customize
- Adjust layout
- Change colors
- Add notes

# Step 5: Export
Export as PNG (2000x1500 or larger)
```

---

## ✅ VALIDATION CHECKLIST

Before considering the diagram complete:

- [ ] All 10 entities present
- [ ] All 18 relationships shown
- [ ] Cardinality labels on all lines
- [ ] Primary keys underlined
- [ ] Foreign keys marked
- [ ] Color scheme applied
- [ ] Renewal self-reference visible
- [ ] Document optional links (dashed)
- [ ] Enum values annotated
- [ ] Legend included
- [ ] Title and date added
- [ ] High resolution (min 1920x1080)
- [ ] Professional appearance

---

## 💡 TIPS FOR BEST RESULTS

1. **Use dbdiagram.io** - Fastest and most accurate
2. **Apply consistent colors** - Helps identify entity types quickly
3. **Show only key attributes** - Don't overcrowd (can omit some)
4. **Label relationship lines** - Makes flow obvious
5. **Add business context notes** - Explains purpose of relationships
6. **Group related entities** - Logical proximity improves readability
7. **Use proper notation** - Crow's foot is industry standard
8. **Export high-res** - Diagram should be zoomable

---

## 📞 SUPPORT

For detailed specifications, see:
- **ER_DIAGRAM_PROMPT.md** (complete guide)
- **database_schema.dbml** (code to auto-generate)

For questions about relationships or business logic, refer to your Spring Boot service classes.

---

**Created:** March 2026  
**Version:** 1.0  
**System:** Fire Insurance Management System  
**Database:** Relational (JPA/Hibernate)

---

🎨 **Happy Diagramming!** 📊

