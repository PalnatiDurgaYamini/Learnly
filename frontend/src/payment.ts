import { api } from './api';

/* Razorpay Checkout is loaded from index.html (checkout.razorpay.com/v1/checkout.js). */
declare global {
  interface Window { Razorpay: new (options: Record<string, unknown>) => { open(): void; on(event: string, cb: (r: { error: { description: string } }) => void): void } }
}

interface CheckoutOrder { keyId: string; razorpayOrderId: string; amount: number; currency: string; courseId: number; courseTitle: string }

/**
 * 1. backend creates the Razorpay order (amount comes from the database, never from the browser)
 * 2. Razorpay popup opens and the student pays
 * 3. we send the popup result to the backend, which verifies the signature and enrolls the student
 * Resolves when the payment is verified; rejects if it fails or the popup is closed.
 */
export async function payForCourse(courseId: number, name: string, email: string): Promise<void> {
  if (!window.Razorpay) throw new Error('Razorpay could not be loaded. Check your internet connection and refresh.');
  const o = await api<CheckoutOrder>(`/payments/checkout/${courseId}`, { method: 'POST' });
  await new Promise<void>((resolve, reject) => {
    let paying = false;   // set once Razorpay reports success, so closing the popup afterwards is not treated as a cancel
    const rzp = new window.Razorpay({
      key: o.keyId,
      amount: o.amount,
      currency: o.currency,
      name: 'Learnly LMS',
      description: o.courseTitle,
      order_id: o.razorpayOrderId,
      prefill: { name, email },
      theme: { color: '#0f766e' },
      handler: (r: { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature: string }) => {
        paying = true;
        api('/payments/verify', {
          method: 'POST',
          body: { razorpayOrderId: r.razorpay_order_id, razorpayPaymentId: r.razorpay_payment_id, razorpaySignature: r.razorpay_signature },
        }).then(() => resolve()).catch(reject);
      },
      modal: { ondismiss: () => { if (!paying) reject(new Error('Payment cancelled')); } },
    });
    rzp.on('payment.failed', r => reject(new Error(r.error.description || 'Payment failed')));
    rzp.open();
  });
}
