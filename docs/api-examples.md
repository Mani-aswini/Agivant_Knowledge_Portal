# REST API Specification & Examples
## Enterprise Knowledge Portal with AI Research Agent

Base URL: `http://localhost:8080`

All endpoints under `/api/knowledge/**` require authentication via HTTP Basic Auth or active session cookie.

---

### 1. Authentication Endpoints

#### A. User Login
- **Method:** `POST`
- **URL:** `/api/auth/login`
- **Headers:** `Content-Type: application/json`
- **Request Body:**
```json
{
  "username": "employee1",
  "password": "password123"
}
```
- **Response (`200 OK`):**
```json
{
  "id": 1,
  "username": "employee1",
  "fullName": "Alice Employee",
  "role": "ROLE_EMPLOYEE"
}
```

#### B. Get Current User Profile
- **Method:** `GET`
- **URL:** `/api/auth/me`
- **Response (`200 OK`):**
```json
{
  "id": 1,
  "username": "employee1",
  "fullName": "Alice Employee",
  "role": "ROLE_EMPLOYEE"
}
```

---

### 2. Document Management Endpoints

#### A. Upload and Index Document
- **Method:** `POST`
- **URL:** `/api/knowledge/documents`
- **Headers:** `Content-Type: multipart/form-data`
- **Form Parameters:**
  - `file`: (binary file, `.txt` or `.pdf`, max 10MB)
  - `title`: `Remote Work Equipment Policy`
  - `description`: `Guidelines on home office reimbursement`
  - `category`: `POLICY` (`POLICY`, `TECHNICAL_RUNBOOK`, `FAQ`)
  - `accessLevel`: `EMPLOYEE` (`PUBLIC`, `EMPLOYEE`, `HR`, `ADMIN`)
- **Response (`201 Created`):**
```json
{
  "id": 6,
  "title": "Remote Work Equipment Policy",
  "description": "Guidelines on home office reimbursement",
  "category": "POLICY",
  "owner": "employee1",
  "accessLevel": "EMPLOYEE",
  "version": 1,
  "status": "ACTIVE",
  "originalFileName": "remote_work.txt",
  "contentType": "text/plain",
  "uploadedAt": "2026-10-13T19:00:00",
  "updatedAt": "2026-10-13T19:00:00",
  "needsReindex": false,
  "archivedAt": null,
  "chunkCount": 3
}
```
- **Validation Errors:**
  - `415 Unsupported Media Type`: if file extension is not `.txt` or `.pdf`.
  - `422 Unprocessable Entity`: if file contains no usable text.
  - `413 Payload Too Large`: if file exceeds 10MB.

#### B. List Accessible Documents
- **Method:** `GET`
- **URL:** `/api/knowledge/documents?category=POLICY&status=ACTIVE`
- **Response (`200 OK`):**
```json
[
  {
    "id": 1,
    "title": "Company Annual Leave and Time Off Policy",
    "description": "Comprehensive annual leave guidelines",
    "category": "POLICY",
    "owner": "hr_user",
    "accessLevel": "EMPLOYEE",
    "version": 1,
    "status": "ACTIVE",
    "chunkCount": 3,
    "updatedAt": "2026-10-13T18:00:00"
  }
]
```

#### C. Get Document Details & Chunks
- **Method:** `GET`
- **URL:** `/api/knowledge/documents/1`
- **Response (`200 OK`):**
```json
{
  "document": {
    "id": 1,
    "title": "Company Annual Leave and Time Off Policy",
    "category": "POLICY",
    "accessLevel": "EMPLOYEE",
    "version": 1,
    "status": "ACTIVE"
  },
  "chunks": [
    {
      "id": 1,
      "documentId": 1,
      "documentVersion": 1,
      "chunkIndex": 1,
      "sectionReference": "Section 1: Annual Leave Entitlement",
      "pageNumber": 1,
      "content": "Full-time regular employees are entitled to 25 days of paid annual leave per calendar year."
    }
  ]
}
```

#### D. Source Preview (Chunk Preview)
- **Method:** `GET`
- **URL:** `/api/knowledge/documents/1/chunks/1`
- **Response (`200 OK`):**
```json
{
  "id": 1,
  "documentId": 1,
  "documentVersion": 1,
  "chunkIndex": 1,
  "sectionReference": "Section 1: Annual Leave Entitlement",
  "pageNumber": 1,
  "content": "Full-time regular employees are entitled to 25 days of paid annual leave per calendar year. Leave must be requested through the HR portal at least 2 weeks in advance of the planned start date."
}
```

#### E. Re-index Document
- **Method:** `PUT`
- **URL:** `/api/knowledge/documents/1/reindex`
- **Response (`200 OK`):**
```json
{
  "id": 1,
  "title": "Company Annual Leave and Time Off Policy",
  "version": 2,
  "status": "ACTIVE",
  "needsReindex": false,
  "chunkCount": 3
}
```

#### F. Safe Archive Document
- **Method:** `DELETE`
- **URL:** `/api/knowledge/documents/1`
- **Response (`200 OK`):**
```json
{
  "id": 1,
  "title": "Company Annual Leave and Time Off Policy",
  "status": "ARCHIVED",
  "archivedAt": "2026-10-13T19:15:00"
}
```

---

### 3. Knowledge Base Search Endpoint

- **Method:** `POST`
- **URL:** `/api/knowledge/search`
- **Headers:** `Content-Type: application/json`
- **Request Body:**
```json
{
  "query": "annual leave days carry over",
  "category": "POLICY",
  "status": "ACTIVE"
}
```
- **Response (`200 OK`):**
```json
{
  "query": "annual leave days carry over",
  "totalResults": 2,
  "results": [
    {
      "documentId": 1,
      "title": "Company Annual Leave and Time Off Policy",
      "category": "POLICY",
      "version": 1,
      "chunkId": 1,
      "chunkIndex": 1,
      "sectionReference": "Section 1: Annual Leave Entitlement",
      "pageNumber": 1,
      "snippet": "Full-time regular employees are entitled to 25 days of paid annual leave...",
      "score": 14.0
    }
  ]
}
```

---

### 4. AI Research Agent Q&A Endpoint

- **Method:** `POST`
- **URL:** `/api/knowledge/ask`
- **Headers:** `Content-Type: application/json`
- **Request Body:**
```json
{
  "question": "How many days of annual leave are available?"
}
```
- **Response (`200 OK`):**
```json
{
  "question": "How many days of annual leave are available?",
  "answer": "According to 'Company Annual Leave and Time Off Policy', Full-time regular employees are entitled to 25 days of paid annual leave per calendar year.",
  "abstained": false,
  "mode": "SIMULATION",
  "sources": [
    {
      "documentId": 1,
      "documentTitle": "Company Annual Leave and Time Off Policy",
      "documentVersion": 1,
      "sectionReference": "Section 1: Annual Leave Entitlement",
      "chunkIndex": 1,
      "excerpt": "Full-time regular employees are entitled to 25 days of paid annual leave per calendar year..."
    }
  ],
  "reasoningNotes": "Prototype simulation mode: Deterministic extractive synthesis from retrieved passages."
}
```

#### Abstention Example (When Evidence is Insufficient)
- **Request Body:**
```json
{
  "question": "What is the recipe for cooking Italian carbonara?"
}
```
- **Response (`200 OK`):**
```json
{
  "question": "What is the recipe for cooking Italian carbonara?",
  "answer": "I cannot find sufficient evidence in the approved enterprise documents accessible to your clearance level to answer this question reliably.",
  "abstained": true,
  "mode": "SIMULATION",
  "sources": [],
  "reasoningNotes": "No matching document passages found within authorized clearance levels."
}
```

---

### 5. Feedback Endpoint

- **Method:** `POST`
- **URL:** `/api/knowledge/feedback`
- **Headers:** `Content-Type: application/json`
- **Request Body:**
```json
{
  "documentId": 1,
  "question": "How many days of annual leave are available?",
  "answerReference": "ANS-POLICY-001",
  "rating": 5,
  "comment": "Accurate, factual citation of the 25 days policy."
}
```
- **Response (`200 OK`):**
```json
{
  "id": 3,
  "documentId": 1,
  "question": "How many days of annual leave are available?",
  "answerReference": "ANS-POLICY-001",
  "rating": 5,
  "comment": "Accurate, factual citation of the 25 days policy.",
  "userId": "employee1",
  "message": "Thank you! Your feedback has been recorded successfully."
}
```

---

### 6. Analytics Endpoints

#### A. Dashboard Reporting Summary
- **Method:** `GET`
- **URL:** `/api/knowledge/analytics`
- **Response (`200 OK`):**
```json
{
  "totalActiveDocuments": 5,
  "totalArchivedDocuments": 0,
  "totalAwaitingReindex": 0,
  "totalFeedbackCount": 2,
  "overallAverageRating": 4.5,
  "documentsByCategoryAndStatus": [
    { "category": "POLICY", "status": "ACTIVE", "count": 2 }
  ],
  "frequentlySearchedCategories": [
    { "category": "POLICY", "searchCount": 1, "avgResults": 3.0 }
  ],
  "feedbackRatingsByDocument": [
    { "documentId": 1, "title": "Company Annual Leave and Time Off Policy", "feedbackCount": 1, "averageRating": 5.0, "fiveStarCount": 1, "lowRatingCount": 0 }
  ]
}
```

#### B. Role Accessibility Simulator (Admin Exclusive)
- **Method:** `GET`
- **URL:** `/api/knowledge/analytics/role-access?role=ROLE_EMPLOYEE`
- **Response (`200 OK`):** Returns list of documents visible to `ROLE_EMPLOYEE` (`PUBLIC` and `EMPLOYEE`).
