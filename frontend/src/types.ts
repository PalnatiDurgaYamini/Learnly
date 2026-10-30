export type Role = 'ADMIN' | 'INSTRUCTOR' | 'STUDENT';

export interface User { id: number; name: string; email: string; role: Role }
export interface Course { id: number; title: string; description: string; priceCents: number; instructor: User }
export interface Material { id: number; title: string; content: string; url: string }
export interface Assignment { id: number; title: string; description: string; dueDate: string | null; maxPoints: number }
export interface Submission {
  id: number; assignment: Assignment; student: User; content: string;
  submittedAt: string; grade: number | null; feedback: string | null;
}
export interface Assessment { id: number; title: string }
export interface Question { id: number; text: string; optionA: string; optionB: string; optionC: string; optionD: string }
export interface Attempt { id: number; score: number; total: number; student?: User }
export interface ForumPost { id: number; title: string | null; body: string; author: User; createdAt: string }
export interface Certificate { id: number; code: string; issuedAt: string; course: Course }
export interface Payment { id: number; status: 'PENDING' | 'PAID' | 'FAILED'; amountCents: number; currency: string; createdAt: string; student?: User; course?: Course }
export interface Job { jobId: number; paymentId: number; jobType: string; status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'; message: string; studentEmail: string; courseTitle: string; updatedAt: string | number[] }
export interface EnrolledCourse { course: Course; progress: number }
export interface StudentProgress { student: User; progress: number }
export interface AuthResponse { token: string; user: User }
