# ProManage Solutions Pvt. Ltd.
## Task Scheduling System

A **Java + PostgreSQL** console application that automatically generates
an optimal weekly project schedule to maximise revenue while respecting
project deadlines.

---

## Table of Contents
1. [Business Rules](#business-rules)
2. [Project Structure](#project-structure)
3. [Algorithm Explained](#algorithm-explained)
4. [Prerequisites](#prerequisites)
5. [Database Setup](#database-setup)
6. [Configuration](#configuration)
7. [Build & Run](#build--run)
8. [Menu Walkthrough](#menu-walkthrough)
9. [Sample Run](#sample-run)

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
TaskSchedulingSystem/
│
├── pom.xml                                      ← Maven build file
│
└── src/
    └── main/
        ├── java/com/promanage/
        │   ├── Main.java                        ← Entry point
        │   │
        │   ├── model/
        │   │   ├── Project.java                 ← Domain object
        │   │   └── ScheduleEntry.java           ← Day ↔ Project mapping
        │   │
        │   ├── dao/
        │   │   ├── ProjectDAO.java              ← DB operations for projects
        │   │   └── ScheduleDAO.java             ← DB operations for schedules
        │   │
        │   ├── service/
        │   │   ├── ProjectService.java          ← Business logic (projects)
        │   │   └── SchedulerService.java        ← Scheduling algorithm
        │   │
        │   ├── ui/
        │   │   └── MainMenu.java                ← Console menu / UI
        │   │
        │   └── util/
        │       ├── DatabaseConnection.java      ← JDBC connection helper
        │       ├── DisplayUtil.java             ← Pretty-print helpers
        │       └── InputValidator.java          ← Safe console input
        │
        └── resources/
            └── schema.sql                       ← DB schema (run once)
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
Placing a project as late as possible within its deadline keeps earlier
slots free for projects with tighter deadlines, maximising the number of
high-value projects we can include.

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
- P2 (deadline 1) → Day 1 (latest free slot ≤ 1)
- P3 (deadline 3) → Day 3
- P4 (deadline 3) → no free slot ≤ 3 **❌ skipped**
- P5 (deadline 4) → Day 4

**Schedule: Day1=P2, Day2=P1, Day3=P3, Day4=P5**
**Total: ₹2,40,000**

---

## Prerequisites

| Requirement | Version |
|-------------|---------|
| Java JDK | 17 or higher |
| Apache Maven | 3.8 or higher |
| PostgreSQL | 13 or higher |

---

## Database Setup

1. **Start PostgreSQL** and open `psql` (or pgAdmin / DBeaver).

2. **Create the database:**
   ```sql
   CREATE DATABASE promanage_db;
   ```

3. **Connect and run the schema:**
   ```bash
   psql -U postgres -d promanage_db -f src/main/resources/schema.sql
   ```

   This creates:
   - `projects` table
   - `schedules` table
   - `schedule_entries` table
   - `project_id_seq` sequence (auto-generates PRJ-0001, PRJ-0002 …)
   - `v_latest_schedule` view

---

## Configuration

Open `src/main/java/com/promanage/util/DatabaseConnection.java` and update:

```java
private static final String HOST     = "localhost";   // DB host
private static final String PORT     = "5432";        // DB port
private static final String DATABASE = "promanage_db";// DB name
private static final String USER     = "postgres";    // DB username
private static final String PASSWORD = "yourpassword";// ← CHANGE THIS
```

---

## Build & Run

### Option A – Maven (recommended)

```bash
# From the project root (where pom.xml lives)
mvn clean package

# Run the fat-jar
java -jar target/task-scheduling-system-jar-with-dependencies.jar
```

### Option B – Maven exec plugin

```bash
mvn compile exec:java
```

### Option C – Manual compile

```bash
# Download postgresql-42.7.3.jar into a lib/ folder first
javac -cp "lib/postgresql-42.7.3.jar" \
      -d out \
      $(find src/main/java -name "*.java")

java -cp "out:lib/postgresql-42.7.3.jar" com.promanage.Main
```

---

## Menu Walkthrough

```
┌─────────────────────────────────────────┐
│           MAIN MENU                     │
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
| 1 | Add a project (title, deadline 1-5, revenue) |
| 2 | List all projects as a formatted table |
| 3 | Run algorithm, display optimal schedule, optionally save it |
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
║                  Automated Task Scheduling System                            ║
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

- **Auto-generated IDs**: Uses a PostgreSQL sequence, formatted as `PRJ-XXXX`
  (zero-padded 4 digits).  IDs are never reused even if projects are deleted.
- **Transactional saves**: Schedule header + all entries are saved in a single
  DB transaction — either all succeed or none do.
- **Layered architecture**: `UI → Service → DAO → DB` keeps concerns
  separated and the code easy to test or extend.
- **Algorithm time complexity**: O(n log n) for sorting + O(n × 5) = O(n) for
  placement → effectively O(n log n) overall, plenty fast for weekly batches.

---

*Built for ProManage Solutions Pvt. Ltd.*
