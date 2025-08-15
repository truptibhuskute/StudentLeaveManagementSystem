// frontend/validator.js
const API_BASE = 'http://localhost:8080/api'; // backend endpoint
const form = document.getElementById('leaveForm');
const statusEl = document.getElementById('status');

function show(msg, ok = false) {
  statusEl.textContent = msg;
  statusEl.style.color = ok ? 'green' : 'crimson';
}

function validDates(from, to) {
  if (!from || !to) return false;
  return new Date(from) <= new Date(to);
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  show('Validating...');
  const data = {
    student_name: document.getElementById('student_name').value.trim(),
    roll_no: document.getElementById('roll_no').value.trim(),
    from_date: document.getElementById('from_date').value,
    to_date: document.getElementById('to_date').value,
    reason: document.getElementById('reason').value.trim()
  };

  if (!data.student_name || !data.roll_no) return show('Name and roll no are required');
  if (!validDates(data.from_date, data.to_date)) return show('Invalid date range');
  if (data.reason.length < 8) return show('Reason too short (min 8 chars)');

  show('Submitting to server...');
  try {
    const resp = await fetch(`${API_BASE}/leave`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    if (!resp.ok) {
      const txt = await resp.text();
      throw new Error(txt || resp.statusText);
    }
    const json = await resp.json();
    show('Submitted successfully! Request ID: ' + json.id, true);
    form.reset();
  } catch (err) {
    show('Submission failed: ' + err.message);
  }
});
