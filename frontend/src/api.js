const API_BASE = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

async function request(path, token, options = {}) {
  const headers = new Headers(options.headers || {});
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new Error(payload?.message || `Request failed (${response.status}).`);
  }
  if (response.status === 204) return null;
  return response;
}

async function json(path, token, options) {
  const response = await request(path, token, options);
  return response.json();
}

export function apiBase() { return API_BASE; }

export function authenticate(mode, email, password) {
  return json(`/api/auth/${mode}`, null, {
    method: 'POST', body: JSON.stringify({ email, password }),
  });
}

export function getMe(token) { return json('/api/auth/me', token); }

export function listRecordings(token, page = 0, size = 20) {
  return json(`/api/recordings?page=${page}&size=${size}`, token);
}

export function uploadRecording(token, file, deleteAudioAfterProcessing) {
  const body = new FormData();
  body.append('file', file, file.name);
  body.append('deleteAudioAfterProcessing', String(deleteAudioAfterProcessing));
  return json('/api/recordings', token, { method: 'POST', body });
}

export function getJob(token, jobId) { return json(`/api/jobs/${jobId}`, token); }
export function getTranscript(token, recordingId) { return json(`/api/recordings/${recordingId}/transcript`, token); }
export function getNotes(token, recordingId, includeDismissed = false) {
  return json(`/api/recordings/${recordingId}/notes?includeDismissed=${includeDismissed}`, token);
}
export function getDecisionRecords(token, recordingId) {
  return json(`/api/recordings/${recordingId}/decisions`, token);
}
export function updateDecisionRecord(token, recordingId, decisionId, fields) {
  return json(`/api/recordings/${recordingId}/decisions/${decisionId}`, token, {
    method: 'PATCH', body: JSON.stringify(fields),
  });
}
export function updateNote(token, recordingId, noteId, fields) {
  return json(`/api/recordings/${recordingId}/notes/${noteId}`, token, {
    method: 'PUT', body: JSON.stringify(fields),
  });
}
export function updateNoteState(token, recordingId, noteId, reviewed, dismissed) {
  return json(`/api/recordings/${recordingId}/notes/${noteId}/state`, token, {
    method: 'PATCH', body: JSON.stringify({ reviewed, dismissed }),
  });
}
export function deleteNote(token, recordingId, noteId) {
  return request(`/api/recordings/${recordingId}/notes/${noteId}`, token, { method: 'DELETE' });
}
export function createAudioLink(token, recordingId) {
  return json(`/api/recordings/${recordingId}/audio-link`, token, { method: 'POST' });
}

export async function downloadExport(token, recordingId, format) {
  const response = await request(`/api/recordings/${recordingId}/export?format=${format}`, token);
  const blob = await response.blob();
  const disposition = response.headers.get('Content-Disposition') || '';
  const filename = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
    ? decodeURIComponent(disposition.match(/filename\*=UTF-8''([^;]+)/i)[1])
    : `clearnotes-recording-${recordingId}.${format === 'pdf' ? 'pdf' : 'md'}`;
  return { blob, filename };
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
