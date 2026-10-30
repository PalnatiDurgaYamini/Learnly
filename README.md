# Learnly – Learning Management System (LMS)

Full-stack LMS: **React + Vite + TypeScript** · **Spring Boot 3 (Java 17)** · **PostgreSQL (pgAdmin)** · **JWT** ·
**Razorpay payments** · **asynchronous e-mail / invoice / notification jobs with live WebSocket updates** · **Docker, Render, Vercel, GitHub Actions**

## Features
| Area | What it does |
|---|---|
| Auth | Register/login, BCrypt passwords, JWT, roles `STUDENT`, `INSTRUCTOR`, `ADMIN` |
| Courses | Instructors create courses (free or paid, price in ₹) and upload materials, assignments, quizzes |
| Enrollment | Free courses enroll directly; paid courses go through Razorpay Checkout |
| Assignments / Quizzes / Forums | Submissions + grading, auto-graded MCQs, per-course discussions |
| Progress & Certificates | % progress, PDF certificate at 100%, public verification page `/verify/:code` |
| **Payments (CO-Payment)** | Razorpay order → popup → backend signature check → enrollment, plus a **webhook** safety net |
| **Background jobs (CO5)** | After a verified payment: `EMAIL`, `INVOICE`, `NOTIFICATION` jobs run with Spring `@Async`; status goes `PENDING → PROCESSING → COMPLETED/FAILED` and is pushed live to the Admin dashboard (WebSocket + STOMP) |
| **Deployment (CO6)** | Env-driven config, Dockerfiles, `render.yaml`, `vercel.json`, GitHub Actions CI |

Default admin (created on first start): `admin@lms.com` / `admin123` — **change it before deploying.**

## How the payment flow works
```
Student clicks "Buy"  ──► POST /api/payments/checkout/{courseId}
                           backend reads the price from the DB, creates a Razorpay order
        ◄── keyId + razorpayOrderId + amount (the secret key never leaves the backend)
Razorpay popup opens, student pays
        ──► POST /api/payments/verify  (order id, payment id, signature)
                           backend recomputes HMAC-SHA256(orderId|paymentId, keySecret)
                           ✔ match → payment PAID, student enrolled, 3 background jobs start
Razorpay ──► POST /api/payments/webhook  (public URL, authenticated by X-Razorpay-Signature)
                           same result if the browser was closed right after paying; idempotent
```
The browser never sends the amount, so it cannot be changed to ₹1. The Admin dashboard → **Payments** tab and the
instructor's course → **Students** tab list payments; the student/instructor/admin can download an invoice from
`GET /api/payments/{id}/invoice`.

## Run locally (without Docker)
1. Create a PostgreSQL database `lms_db` (e.g. in pgAdmin).
2. Backend (PowerShell users: use `$env:NAME="value"` instead of `export`):
   ```bash
   cd backend
   export DATABASE_URL=jdbc:postgresql://localhost:5432/lms_db DATABASE_USERNAME=postgres DATABASE_PASSWORD=yourpassword
   export RAZORPAY_KEY_ID=rzp_test_xxx RAZORPAY_KEY_SECRET=xxx RAZORPAY_WEBHOOK_SECRET=any-long-random-string
   export MAIL_USERNAME=you@gmail.com MAIL_PASSWORD="your 16 char app password"   # optional
   mvn spring-boot:run                      # http://localhost:8080
   ```
3. Frontend:
   ```bash
   cd frontend
   cp .env.example .env
   npm install && npm run dev               # http://localhost:5173
   ```
Tables (`payment`, `background_jobs`, …) are created automatically (`ddl-auto=update`).
If you ran an older version of this project with Stripe, the old `session_id` column in `payment` is simply left unused.

## Razorpay setup (Test Mode)
1. Sign in at https://dashboard.razorpay.com/ and switch on **Test Mode**.
2. *Account & Settings → API Keys → Generate Test Key*. Copy the **Key ID** (`rzp_test_…`) and **Key Secret** (shown once).
   Put them in `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET`. Never use `rzp_live_` keys for coursework.
3. Webhook (optional but recommended): Razorpay cannot call `localhost`, so expose the backend with ngrok:
   ```bash
   ngrok config add-authtoken <YOUR_OWN_TOKEN>
   ngrok http 8080            # gives https://abc123.ngrok-free.app
   ```
   Dashboard → *Webhooks → Add New Webhook*: URL `https://abc123.ngrok-free.app/api/payments/webhook`, secret = the value you
   set in `RAZORPAY_WEBHOOK_SECRET`, events **payment.captured**, **payment.failed** (and `order.paid`).
4. Test payment: log in as a **student**, open a paid course, click **Buy this course**.
   Test card `5267 3181 8797 5449` (Razorpay's documented domestic test card), any future expiry, any CVV, any name, any OTP.
   (UPI test ID `success@razorpay` succeeds, `failure@razorpay` fails.)

## E-mail setup (Gmail)
Enable 2-Step Verification, then create an **App Password** at https://myaccount.google.com/apppasswords.
Set `MAIL_USERNAME` (your Gmail address) and `MAIL_PASSWORD` (the 16-character app password). Never use your normal password
and never commit these. If they are missing, the `EMAIL` job shows **FAILED** with a clear message while the other jobs still
complete – this is intentional so you can demonstrate failure tracking.

## Background jobs (Spring `@Async` + WebSocket)
* `BackgroundJobService` creates three rows in `background_jobs` (EMAIL, INVOICE, NOTIFICATION) and calls
  `BackgroundJobProcessor.process()` (`@Async`, thread pool `LMS-Async-*`, 3–5 threads).
* Each status change is saved in PostgreSQL and published to `/topic/jobs` through `JobNotificationService`.
* Admin dashboard → **Jobs** tab loads `GET /api/jobs` (ADMIN only) and then subscribes to `/topic/jobs` at `ws://…/ws-lms`.
  The WebSocket handshake is checked: only a valid ADMIN JWT may connect.
* Invoices are written as HTML to the `invoices/` folder of the backend (`INVOICE_DIR` to change). Missing files are re-created on download.
* Limitation: `@Async` runs in memory – a job queued when the server stops is lost. A durable queue (RabbitMQ, etc.) would be the next step.

### Testing checklist
- [ ] Backend on 8080, frontend on 5173, PostgreSQL running
- [ ] Log in as admin, open **Admin → Jobs** and keep it open
- [ ] In another browser (or private window) pay for a course as a student
- [ ] Student is enrolled; `payment.status = PAID`; three rows appear in `background_jobs`
- [ ] Jobs change status live without refresh; invoice file exists in `backend/invoices/`; confirmation e-mail arrives
- [ ] Remove `MAIL_PASSWORD`, pay again → EMAIL job becomes FAILED, the others COMPLETED
- [ ] Close the Razorpay popup / use `failure@razorpay` → student is not enrolled, no jobs created

## Run everything with Docker (includes pgAdmin)
```bash
cp .env.example .env      # edit secrets
docker compose up --build
```
App: http://localhost:3000 · API: http://localhost:8080/api · pgAdmin: http://localhost:5050
(pgAdmin: *Register → Server*, host `db`, port `5432`, DB `lms_db`, user/password from `.env`).

## Deployment (GitHub → Render backend + Postgres → Vercel frontend)
Environment variables are read by `application.properties`; **no secret is stored in the repository** (`.env` is git-ignored).

**1. Check the build locally**
```bash
cd backend  && mvn clean package -DskipTests      # creates target/lms-1.0.0.jar
cd ../frontend && npm install && npm run build    # creates dist/
```
**2. Push to GitHub**
```bash
git add . && git commit -m "LMS with payments, background jobs and deployment" && git branch -M main
git remote add origin <YOUR_GITHUB_REPOSITORY_URL> && git push -u origin main
```
GitHub Actions (`.github/workflows/ci.yml`) then builds both projects on every push.

**3. PostgreSQL on Render** – *New → PostgreSQL* (name `lms-db`, region Singapore, plan Free). Free databases expire after ~30 days,
so treat it as a demo database. Copy the **Internal** connection details.

**4. Backend on Render** – *New → Web Service* → your repository, language **Docker**, root directory `backend`
(or use the included `render.yaml` Blueprint), same region as the database. Environment variables:

| Variable | Value |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://<internal-host>:5432/<database>` (must start with `jdbc:`) |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | from the Render database |
| `JWT_SECRET` | any random string, 32+ characters |
| `FRONTEND_URL` | your Vercel URL, e.g. `https://my-lms.vercel.app` (comma-separate to also allow `http://localhost:5173`) |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | Razorpay **test** values |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Gmail address + App Password |

Health check path: `/actuator/health`. Your API URL will look like `https://lms-backend.onrender.com`.
Test it: `POST /api/auth/register`, `POST /api/auth/login`, then `GET /api/courses`.

**5. Frontend on Vercel** – *Add New → Project* → import the repository, **Root Directory `frontend`**, framework Vite
(build `npm run build`, output `dist`). Environment variable: `VITE_API_URL=https://<your-render-backend>/api` (note the `/api`).
`frontend/vercel.json` makes page refreshes work with React Router.

**6. Finish** – set the backend's `FRONTEND_URL` to the final Vercel URL (it controls CORS **and** the admin WebSocket origin),
and in Razorpay add the webhook `https://<your-render-backend>/api/payments/webhook`. Render's free tier sleeps when idle,
so the first request can take about a minute.

## Environment variables (summary)
`DATABASE_URL` `DATABASE_USERNAME` `DATABASE_PASSWORD` (older `DB_URL` `DB_USER` `DB_PASSWORD` also work) · `JWT_SECRET` · `FRONTEND_URL` ·
`RAZORPAY_KEY_ID` `RAZORPAY_KEY_SECRET` `RAZORPAY_WEBHOOK_SECRET` `RAZORPAY_CURRENCY` (default INR) · `MAIL_USERNAME` `MAIL_PASSWORD` `MAIL_FROM` ·
`INVOICE_DIR` · `PORT` · frontend: `VITE_API_URL`.

## API overview
`/api/auth/*` · `/api/courses` · `/api/courses/{id}/materials|assignments|assessments|posts|students|progress` · `/api/assignments/{id}/submit` ·
`/api/submissions/{id}/grade` · `/api/assessments/{id}/attempt` · `/api/posts/{id}/replies` · `/api/certificates/*` ·
`/api/payments/checkout/{courseId}` · `/api/payments/verify` · `/api/payments/webhook` · `/api/payments/{id}/invoice` · `/api/jobs` (admin) · WebSocket `/ws-lms` → `/topic/jobs`

## Project structure
```
backend/   Spring Boot (config, dto, model, repo, security, service, web)
frontend/  React + TS (pages, payment.ts, api client, auth context)
docker-compose.yml  Postgres + pgAdmin + backend + frontend
render.yaml         Render Blueprint (backend + database)
.github/workflows   CI
```
