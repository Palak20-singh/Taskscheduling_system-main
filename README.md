# ProManage Solutions Pvt. Ltd.
## Intelligent Task Scheduling System

A full-stack **Java + PostgreSQL + Python AI** application that automatically generates an optimal weekly project schedule to maximise revenue while respecting project deadlines — enriched with machine learning predictions for smarter planning.

---

## Table of Contents
1. [System Architecture](#system-architecture)
2. [Business Rules](#business-rules)
3. [Project Structure](#project-structure)
4. [Algorithm Explained](#algorithm-explained)
5. [AI Prediction Service](#ai-prediction-service)
6. [Prerequisites](#prerequisites)
7. [Database Setup](#database-setup)
8. [Configuration](#configuration)
9. [Build & Run](#build--run)
10. [Menu Walkthrough](#menu-walkthrough)
11. [Sample Run](#sample-run)
12. [Key Design Decisions](#key-design-decisions)

---

## System Architecture

```
Browser / Console Frontend
          │
          ▼
Java Spring Boot (CRUD, DB, Scheduling, Menu)
          │  server-to-server HTTP POST /predict
          ▼
Python FastAPI — ML Microservice  (this repo)
          │
          ▼
   7 Pre-trained ML Models (.pkl)
```

The system is split into two focused services:

- **Java backend** — handles the console UI, database operations, CRUD, and the greedy scheduling algorithm.
- **Python ML microservice** — receives 4 project fields and returns 11 AI-enriched fields (complexity, delay rate, predicted completion days, final revenue, etc.). It does **not** touch the database.

---

## Business Rules

| Rule | Detail |
|------|--------|
| Work week | Monday – Friday (5 working days) |
| Projects received | Every Saturday |
| Planning window | Saturday – Sunday |
| Work starts | Monday |
| Max projects/week | 5 |
| Max projects/day | 1 |
| Deadline meaning | If deadline = 3, the project **must** be placed on Day 1, 2, or 3 |
| Revenue rule | A project completed after its deadline earns **₹0** |
| Goal | Maximise total weekly revenue |

---

## Project Structure

```
ProManage/
│
├── README.md
│
├── task-scheduling-system/              ← Java backend
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/promanage/
│           │   ├── Main.java
│           │   ├── model/
│           │   │   ├── Project.java
│           │   │   └── ScheduleEntry.java
│           │   ├── dao/
│           │   │   ├── ProjectDAO.java
│           │   │   └── ScheduleDAO.java
│           │   ├── service/
│           │   │   ├── ProjectService.java
│           │   │   └── SchedulerService.java
│           │   ├── ui/
│           │   │   └── MainMenu.java
│           │   └── util/
│           │       ├── DatabaseConnection.java
│           │       ├── DisplayUtil.java
│           │       └── InputValidator.java
│           └── resources/
│               └── schema.sql
│
└── project_scheduler_ai/               ← Python ML microservice
    ├── app/
    │   ├── main.py                     ← FastAPI app & /predict endpoint
    │   ├── predictor.py                ← Core ML prediction pipeline
    │   ├── model_loader.py             ← Loads all 7 models at startup
    │   └── schemas.py                  ← Pydantic input/output schemas
    └── models/                         ← Pre-trained .pkl files (not tracked in git)
        ├── model_complexity.pkl
        ├── model_client_priority.pkl
        ├── model_team_experience.pkl
        ├── model_delay_rate.pkl
        ├── model_completion_days.pkl
        ├── delay_encoder.pkl
        └── target_label_encoders.pkl
```

---

## Algorithm Explained

The scheduler uses the classic **Deadline-Constrained Job Scheduling** greedy algorithm:

```
1. Sort all projects by revenue (highest first)
2. For each project (greedy, most valuable first):
     Find the LATEST free day-slot ≤ project.deadline
     If found → assign project to that slot
3. Output the filled slots in day order
```

**Why "latest available slot"?**
Placing a project as late as possible within its deadline keeps earlier slots free for projects with tighter deadlines, maximising the number of high-value projects we can include.

**Example**

| Project | Revenue | Deadline |
|---------|---------|----------|
| P1 | ₹80,000 | 2 |
| P2 | ₹70,000 | 1 |
| P3 | ₹60,000 | 3 |
| P4 | ₹50,000 | 3 |
| P5 | ₹40,000 | 4 |

Sorted order: P1, P2, P3, P4, P5

- P1 (deadline 2) → Day 2 (latest free slot ≤ 2)
- P2 (deadline 1) → Day 1
- P3 (deadline 3) → Day 3
- P4 (deadline 3) → no free slot ≤ 3 **❌ skipped**
- P5 (deadline 4) → Day 4

**Schedule: Day1=P2, Day2=P1, Day3=P3, Day4=P5**
**Total: ₹2,40,000**

**Time Complexity:** O(n log n) for sorting + O(n × 5) for placement → effectively **O(n log n)** overall.

---

## AI Prediction Service

The Python microservice runs 7 trained ML models to enrich each project with scheduling intelligence before the greedy algorithm runs.

### `POST /predict`

**Request** — 4 fields sent by the Java backend:
```json
{
  "project_title": "Website Redesign",
  "project_type": "Web Development",
  "deadline_days": 30,
  "base_revenue": 15000.00
}
```

**Response** — 11 enriched fields returned:
```json
{
  "project_title": "Website Redesign",
  "project_type": "Web Development",
  "deadline_days": 30,
  "base_revenue": 15000.00,
  "complexity_level": "Medium",
  "client_priority": "High",
  "team_experience_level": "Senior",
  "predicted_delay_rate": "12%",
  "predicted_completion_days": 33.5,
  "delay_days": "3 days",
  "completed_on_time": "No",
  "final_revenue_realized": 12750.00
}
```

**Revenue Penalty Formula:** `5% of base_revenue × number of delay days`

The Java `PythonApiClient` calls this endpoint server-to-server (not from the browser) at `http://localhost:8000/predict`.

---

## Prerequisites

| Requirement | Version |
|-------------|---------|
| Java JDK | 17 or higher |
| Apache Maven | 3.8 or higher |
| PostgreSQL | 13 or higher |
| Python | 3.10 or higher |

---

## Database Setup

1. **Start PostgreSQL** and open `psql` (or pgAdmin / DBeaver).

2. **Create the database:**
   ```sql
   CREATE DATABASE promanage_db;
   ```

3. **Connect and run the schema:**
   ```bash
   psql -U postgres -d promanage_db -f task-scheduling-system/src/main/resources/schema.sql
   ```

   This creates:
   - `projects` table
   - `schedules` table
   - `schedule_entries` table
   - `project_id_seq` sequence (auto-generates PRJ-0001, PRJ-0002 …)
   - `v_latest_schedule` view

---

## Configuration

### Java — Database Connection

Open `task-scheduling-system/src/main/java/com/promanage/util/DatabaseConnection.java` and update:

```java
private static final String HOST     = "localhost";    // DB host
private static final String PORT     = "5432";         // DB port
private static final String DATABASE = "promanage_db"; // DB name
private static final String USER     = "postgres";     // DB username
private static final String PASSWORD = "yourpassword"; // ← CHANGE THIS
```

### Python — Model Files

Place all pre-trained `.pkl` model files inside `project_scheduler_ai/models/` before starting the service. This folder is excluded from version control (see `.gitignore`).

---

## Build & Run

### 1. Start the Python ML Service

```bash
cd project_scheduler_ai/app
pip install fastapi uvicorn scikit-learn pandas joblib pydantic
uvicorn main:app --reload
```

The ML service will be available at `http://localhost:8000`.
Interactive API docs: `http://localhost:8000/docs`

### 2. Start the Java Application

**Option A – Maven (recommended)**
```bash
cd task-scheduling-system
mvn clean package
java -jar target/task-scheduling-system-jar-with-dependencies.jar
```

**Option B – Maven exec plugin**
```bash
mvn compile exec:java
```

**Option C – Manual compile**
```bash
# Download postgresql-42.7.3.jar into a lib/ folder first
javac -cp "lib/postgresql-42.7.3.jar" \
      -d out \
      $(find src/main/java -name "*.java")

java -cp "out:lib/postgresql-42.7.3.jar" com.promanage.Main
```

> **Note:** Start the Python service before the Java application so AI enrichment is available on startup.

---

## Menu Walkthrough

```
┌─────────────────────────────────────────┐
│               MAIN MENU                 │
├─────────────────────────────────────────┤
│  1. Add New Project                     │
│  2. View All Projects                   │
│  3. Generate Optimal Schedule           │
│  4. View Latest Schedule                │
│  5. Schedule History                    │
│  6. Search Project by ID                │
│  7. Delete Project                      │
│  8. Exit                                │
└─────────────────────────────────────────┘
```

| Option | Action |
|--------|--------|
| 1 | Add a project (title, deadline 1–5, revenue); AI predictions are fetched and stored automatically |
| 2 | List all projects as a formatted table, including AI-enriched fields |
| 3 | Run the greedy algorithm, display the optimal schedule, optionally save it |
| 4 | Display the most recently saved schedule |
| 5 | Show the last 10 saved schedules (summary) |
| 6 | Look up a project by its ID (e.g. PRJ-0003) |
| 7 | Delete a project by ID (with confirmation) |
| 8 | Exit gracefully |

---

## Sample Run

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                    ProManage Solutions Pvt. Ltd.                             ║
║               Intelligent Automated Task Scheduling System                   ║
╚══════════════════════════════════════════════════════════════════════════════╝

Testing database connection …
✔  Connected to PostgreSQL  →  promanage_db

  Enter choice [1-8]: 3

  Found 6 project(s). Running scheduling algorithm …

  ┌──────────────────────────────────────────────────────────────────────────┐
  │                     OPTIMAL WEEKLY SCHEDULE                              │
  └──────────────────────────────────────────────────────────────────────────┘

  Day    Day Name     Project ID  Title                                 Revenue (₹)
  ──────────────────────────────────────────────────────────────────────────────
  1      Monday       PRJ-0002    UI Design – Client Alpha              70,000.00
  2      Tuesday      PRJ-0001    Web Development – FinTech App         80,000.00
  3      Wednesday    PRJ-0003    API Integration – E-commerce          60,000.00
  4      Thursday     PRJ-0005    Performance Testing Suite             40,000.00
  5      Friday       PRJ-0006    Cloud Deployment – Banking App        35,000.00
  ──────────────────────────────────────────────────────────────────────────────
  TOTAL REVENUE (₹):                                                    2,85,000.00
  ──────────────────────────────────────────────────────────────────────────────

  Projects NOT scheduled (lower revenue / missed deadline):
  PRJ-0004   Mobile App QA Testing       Deadline: Day 3   Revenue: 50,000.00

  Save this schedule to the database? (y/n): y
  ✔  Schedule saved (ID: 1).
```

---

## Key Design Decisions

- **Separation of concerns** — The Python service handles only ML inference; the Java service owns all data, business logic, and UI. Each can be developed, scaled, or replaced independently.
- **Auto-generated IDs** — Uses a PostgreSQL sequence, formatted as `PRJ-XXXX` (zero-padded 4 digits). IDs are never reused even if projects are deleted.
- **Transactional saves** — Schedule header + all entries are saved in a single DB transaction — either all succeed or none do.
- **Layered architecture** — `UI → Service → DAO → DB` keeps concerns separated and the code easy to test or extend.
- **Models loaded once** — All 7 ML models are loaded into memory at Python service startup, keeping per-request inference fast.
- **Algorithm efficiency** — O(n log n) overall, plenty fast for weekly project batches.

---

## .gitignore Recommendations

```
# Python
models/
__pycache__/
*.pyc
.ipynb_checkpoints/

# Java
target/
*.class

# Environment
*.env
```

---

*Built for ProManage Solutions Pvt. Ltd.*
