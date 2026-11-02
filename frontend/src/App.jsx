import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import AudioCapture from './components/AudioCapture.jsx';
import {
  apiBase, authenticate, createAudioLink, deleteNote, downloadBlob, downloadExport,
  getJob, getMe, getNotes, getTranscript, listRecordings, updateNote, updateNoteState,
  uploadRecording,
} from './api.js';

const ACTIVE = new Set(['QUEUED', 'TRANSCRIBING', 'EXTRACTING']);
const EMPTY_EDIT = { content: '', owner: '', dueDate: '', meetingDate: '', meetingStartTime: '', meetingEndTime: '' };
const ERROR_TEXT = (error) => error instanceof Error ? error.message : 'Something went wrong. Please try again.';

function dateLabel(value) {
  return value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value)) : 'Not specified';
}

function timestamp(value) {
  const seconds = Math.max(0, Number(value) || 0);
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(Math.floor(seconds % 60)).padStart(2, '0')}`;
}

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem('clearnotes-token') || '');
  const [user, setUser] = useState(null);
  const [authMode, setAuthMode] = useState('login');
  const [authVisible, setAuthVisible] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [recordings, setRecordings] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [notes, setNotes] = useState([]);
  const [transcript, setTranscript] = useState([]);
  const [showDismissed, setShowDismissed] = useState(false);
  const [audioUrl, setAudioUrl] = useState('');
  const [deleteAudio, setDeleteAudio] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [busyReview, setBusyReview] = useState(false);
  const [busyAuth, setBusyAuth] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [highlight, setHighlight] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [edit, setEdit] = useState(EMPTY_EDIT);
  const [page, setPage] = useState({ page: 0, totalPages: 0, totalElements: 0 });
  const audioRef = useRef(null);
  const recordingsRef = useRef([]);
  const fileRef = useRef(null);
  const segmentRefs = useRef(new Map());
  const selected = useMemo(() => recordings.find((row) => row.id === selectedId) || null, [recordings, selectedId]);
  const selectedStatus = selected?.status;

  useEffect(() => { recordingsRef.current = recordings; }, [recordings]);

  const refreshList = useCallback(async (auth = token, pageNo = 0) => {
    const data = await listRecordings(auth, pageNo);
    setRecordings(data.content || []);
    setPage({ page: data.page, totalPages: data.totalPages, totalElements: data.totalElements });
    return data.content || [];
  }, [token]);

  const refreshReview = useCallback(async (id = selectedId, includeDismissed = showDismissed) => {
    if (!token || !id) return;
    setBusyReview(true); setError('');
    try {
      const [noteData, transcriptData] = await Promise.all([
        getNotes(token, id, includeDismissed), getTranscript(token, id),
      ]);
      setNotes(noteData || []); setTranscript(transcriptData || []); setHighlight(null);
      const row = recordingsRef.current.find((item) => item.id === id);
      if (row?.audioDeleted) setAudioUrl('');
      else {
        const link = await createAudioLink(token, id);
        setAudioUrl(new URL(link.url, apiBase()).toString());
      }
    } catch (cause) {
      setError(ERROR_TEXT(cause)); setNotes([]); setTranscript([]); setAudioUrl('');
    } finally { setBusyReview(false); }
  }, [selectedId, showDismissed, token]);

  useEffect(() => {
    if (!token) return undefined;
    let alive = true;
    (async () => {
      try {
        const current = await getMe(token);
        if (!alive) return;
        setUser(current);
        const items = await refreshList(token, 0);
        if (alive && items.length) setSelectedId((value) => value || items[0].id);
      } catch { if (alive) signOut(); }
    })();
    return () => { alive = false; };
    // Token changes represent a new authenticated session.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  useEffect(() => {
    if (token && selectedId && selectedStatus && !ACTIVE.has(selectedStatus)) {
      refreshReview(selectedId, showDismissed);
    }
  }, [token, selectedId, selectedStatus, showDismissed, refreshReview]);

  useEffect(() => {
    if (!token || !recordings.some((row) => ACTIVE.has(row.status))) return undefined;
    const timer = window.setInterval(() => {
      listRecordings(token, page.page).then((data) => setRecordings(data.content || [])).catch(() => {});
    }, 3000);
    return () => window.clearInterval(timer);
  }, [token, recordings, page.page]);

  function signOut() {
    localStorage.removeItem('clearnotes-token'); setToken(''); setUser(null);
    setRecordings([]); setSelectedId(null); setNotes([]); setTranscript([]); setAudioUrl(''); setPassword('');
    setAuthVisible(false);
  }

  async function submitAuth(event) {
    event.preventDefault(); setBusyAuth(true); setError('');
    try {
      const data = await authenticate(authMode, email.trim(), password);
      localStorage.setItem('clearnotes-token', data.accessToken); setToken(data.accessToken); setUser(data.user); setPassword('');
    } catch (cause) { setError(ERROR_TEXT(cause)); }
    finally { setBusyAuth(false); }
  }

  async function uploadFile(file) {
    if (!file) return;
    setSelectedFile(file); setError(''); setNotice('');
    const ext = file.name.toLowerCase().split('.').pop();
    if (!['mp3', 'wav', 'm4a', 'ogg', 'webm', 'flac'].includes(ext)) {
      setError('Choose an MP3, WAV, M4A, OGG, WebM, or FLAC audio file.'); return;
    }
    if (file.size > 100 * 1024 * 1024) { setError('Audio files must be 100 MB or smaller.'); return; }
    setUploading(true);
    try {
      const result = await uploadRecording(token, file, deleteAudio);
      setNotice(`“${result.originalFilename}” uploaded. ClearNotes is transcribing it and creating notes…`);
      setRecordings((current) => [{
        id: result.recordingId, originalFilename: result.originalFilename, fileSizeBytes: result.fileSizeBytes,
        createdAt: result.createdAt, audioDeleted: false, jobId: result.jobId, status: result.status,
      }, ...current.filter((row) => row.id !== result.recordingId)]);
      setSelectedId(result.recordingId);
      for (let i = 0; i < 240; i += 1) {
        await new Promise((resolve) => window.setTimeout(resolve, 2500));
        const job = await getJob(token, result.jobId);
        setRecordings((current) => current.map((row) => row.id === job.recordingId
          ? { ...row, status: job.status, audioDeleted: job.status === 'DONE' && deleteAudio } : row));
        if (!ACTIVE.has(job.status)) {
          if (job.status === 'FAILED') throw new Error(job.errorMessage || 'Processing failed. Please try another recording.');
          await refreshList(token, page.page); setNotice('Your transcript and notes are ready.'); return;
        }
      }
      await refreshList(token, page.page); setNotice('Processing is still running. It will appear in My recordings when ready.');
    } catch (cause) { setError(ERROR_TEXT(cause)); }
    finally { setUploading(false); }
  }

  function chooseFile(event) {
    const file = event.target.files?.[0];
    if (file) uploadFile(file);
    event.target.value = '';
  }

  function seek(segment) {
    setHighlight(segment.id);
    if (audioRef.current && audioUrl) {
      audioRef.current.currentTime = Number(segment.startSeconds);
      audioRef.current.play().catch(() => {});
    }
    segmentRefs.current.get(segment.id)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  function beginEdit(note) {
    setEditingId(note.id);
    setEdit({
      content: note.content || '', owner: note.owner || '', dueDate: note.dueDate || '',
      meetingDate: note.meetingDate || '', meetingStartTime: (note.meetingStartTime || '').slice(0, 5),
      meetingEndTime: (note.meetingEndTime || '').slice(0, 5),
    });
  }

  async function saveEdit(note) {
    setError('');
    try {
      const saved = await updateNote(token, selectedId, note.id, {
        content: edit.content, owner: edit.owner || null, dueDate: edit.dueDate || null,
        meetingDate: note.type === 'MEETING' ? edit.meetingDate || null : null,
        meetingStartTime: note.type === 'MEETING' ? edit.meetingStartTime || null : null,
        meetingEndTime: note.type === 'MEETING' ? edit.meetingEndTime || null : null,
      });
      setNotes((current) => current.map((item) => item.id === note.id ? saved : item));
      setEditingId(null); setNotice('Note changes saved.');
    } catch (cause) { setError(ERROR_TEXT(cause)); }
  }

  async function setNoteState(note, reviewed, dismissed) {
    setError('');
    try {
      await updateNoteState(token, selectedId, note.id, reviewed, dismissed);
      await refreshReview(selectedId, showDismissed);
      setNotice(dismissed ? 'Note dismissed.' : reviewed ? 'Note marked reviewed.' : 'Note restored.');
    } catch (cause) { setError(ERROR_TEXT(cause)); }
  }

  async function removeNote(note) {
    if (!window.confirm('Delete this note permanently?')) return;
    try {
      await deleteNote(token, selectedId, note.id);
      setNotes((current) => current.filter((item) => item.id !== note.id)); setNotice('Note deleted.');
    } catch (cause) { setError(ERROR_TEXT(cause)); }
  }

  async function exportAs(format) {
    try {
      const result = await downloadExport(token, selectedId, format);
      downloadBlob(result.blob, result.filename);
    } catch (cause) { setError(ERROR_TEXT(cause)); }
  }

  if (!token) {
    if (!authVisible) return <LandingPage
      onLogin={() => { setAuthMode('login'); setError(''); setAuthVisible(true); }}
      onRegister={() => { setAuthMode('register'); setError(''); setAuthVisible(true); }}
    />;
    return <main className="auth-shell"><section className="auth-card">
      <button className="text-button auth-back" onClick={() => { setAuthVisible(false); setError(''); }}>← Back to ClearNotes</button>
      <Brand /><p className="eyebrow">A clearer way to follow up</p>
      <h1>{authMode === 'login' ? 'Welcome back' : 'Create your account'}</h1>
      <p className="muted">Turn meeting audio into evidence-linked notes and actions.</p>
      <form className="stack-form" onSubmit={submitAuth}>
        <label>Email address<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required /></label>
        <label>Password<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={authMode === 'register' ? 12 : undefined} autoComplete={authMode === 'register' ? 'new-password' : 'current-password'} required /></label>
        {authMode === 'register' && <p className="field-hint">Use at least 12 characters.</p>}
        {error && <p className="inline-error" role="alert">{error}</p>}
        <button className="button button-primary full-button" disabled={busyAuth}>{busyAuth ? 'Please wait…' : authMode === 'login' ? 'Sign in' : 'Create account'}</button>
      </form>
      <button className="text-button auth-switch" onClick={() => { setAuthMode(authMode === 'login' ? 'register' : 'login'); setError(''); }}>
        {authMode === 'login' ? 'New to ClearNotes? Create an account' : 'Already have an account? Sign in'}
      </button>
    </section></main>;
  }
  if (!user) return <main className="auth-shell"><div className="loading-line"><span className="spinner" /> Opening your workspace…</div></main>;

  const groups = [
    { type: 'DECISION', label: 'Decisions' }, { type: 'TASK', label: 'Tasks' }, { type: 'MEETING', label: 'Meetings' },
  ].map((group) => ({ ...group, items: notes.filter((note) => note.type === group.type) }));

  return <div className="app-shell dashboard-shell" id="top">
    <aside className="dashboard-sidebar">
      <Brand />
      <p className="sidebar-label">WORKSPACE</p>
      <nav className="sidebar-nav" aria-label="Workspace navigation">
        <a className="sidebar-link sidebar-link-active" href="#overview"><span>⌂</span> Overview</a>
        <a className="sidebar-link" href="#recording-library"><span>♫</span> My recordings</a>
        <a className="sidebar-link" href="#capture-recording"><span>＋</span> New recording</a>
        <a className="sidebar-link" href="#recording-review"><span>▤</span> Meeting notes</a>
      </nav>
      <div className="sidebar-account"><span className="account-avatar">{user.email?.[0]?.toUpperCase() || 'U'}</span><span className="sidebar-email" title={user.email}>{user.email}</span></div>
    </aside>
    <div className="dashboard-main">
    <header className="topbar"><span className="workspace-context">ClearNotes workspace</span><div className="account-controls"><span>{user.email}</span><button className="text-button" onClick={signOut}>Sign out</button></div></header>
    <main className="main-content">
      <section className="welcome-row" id="overview"><div><p className="eyebrow">Your meeting workspace</p><h1>Good notes start here.</h1><p className="muted">Upload a recording or capture one now. ClearNotes will transcribe it and organize the follow-ups.</p></div><span className="recording-count">{page.totalElements} recording{page.totalElements === 1 ? '' : 's'}</span></section>

      <section className="upload-card" id="capture-recording" onDragOver={(e) => e.preventDefault()} onDrop={(e) => { e.preventDefault(); uploadFile(e.dataTransfer.files?.[0]); }}>
        <div className="upload-icon" aria-hidden="true">↥</div><div className="upload-copy"><h2>Start with a recording</h2><p className="muted">Drop an audio file here or choose one from your device. MP3, WAV, M4A, OGG, WebM, or FLAC · up to 100 MB.</p></div>
        <input ref={fileRef} className="visually-hidden" type="file" accept="audio/*,.flac" onChange={chooseFile} />
        <button className="button button-primary" onClick={() => fileRef.current?.click()} disabled={uploading}>{uploading ? 'Processing audio…' : 'Choose audio file'}</button>
        <div className="upload-divider"><span />or<span /></div>
        <AudioCapture onRecorded={uploadFile} disabled={uploading} />
        <label className="check-row"><input type="checkbox" checked={deleteAudio} onChange={(e) => setDeleteAudio(e.target.checked)} /> Delete the original audio after processing</label>
        {selectedFile && <p className="field-hint selected-file">Selected: {selectedFile.name}</p>}
      </section>

      {(error || notice) && <div className={error ? 'banner banner-error' : 'banner banner-notice'} role={error ? 'alert' : 'status'}>{error || notice}<button className="banner-close" onClick={() => { setError(''); setNotice(''); }} aria-label="Dismiss">×</button></div>}

      <section className="workspace-grid">
        <aside className="recordings-panel panel" id="recording-library">
          <div className="panel-heading"><div><p className="eyebrow">Library</p><h2>My recordings</h2></div><button className="icon-button" onClick={() => refreshList(token, page.page)} aria-label="Refresh recordings">↻</button></div>
          {!recordings.length ? <div className="empty-state"><div className="empty-art">♫</div><h3>No recordings yet</h3><p>Your audio and notes will appear here.</p></div> : <div className="recording-list">{recordings.map((row) =>
            <button key={row.id} className={`recording-row ${row.id === selectedId ? 'recording-row-active' : ''}`} onClick={() => setSelectedId(row.id)}>
              <span className="recording-mark">♫</span><span className="recording-info"><strong>{row.originalFilename}</strong><small>{dateLabel(row.createdAt)}</small><small className={`status status-${String(row.status || '').toLowerCase()}`}>{row.status || 'UPLOADED'}{ACTIVE.has(row.status) ? ' · working' : ''}</small></span><span className="row-arrow">›</span>
            </button>)}</div>}
          {page.totalPages > 1 && <div className="pagination"><button disabled={page.page === 0} onClick={() => refreshList(token, page.page - 1)}>Previous</button><span>{page.page + 1} / {page.totalPages}</span><button disabled={page.page + 1 >= page.totalPages} onClick={() => refreshList(token, page.page + 1)}>Next</button></div>}
        </aside>

        <section className="review-panel panel" id="recording-review">
          {!selected ? <div className="empty-review"><div className="empty-art">✦</div><h2>Choose a recording</h2><p className="muted">Your decisions, tasks, and transcript will show up here.</p></div> : <>
            <div className="review-header"><div className="review-title"><p className="eyebrow">Recording review</p><h2 title={selected.originalFilename}>{selected.originalFilename}</h2><p className="muted">Added {dateLabel(selected.createdAt)} <span className="dot-separator">·</span> {selected.status}</p></div><div className="export-actions"><button className="button button-light button-small" onClick={() => exportAs('markdown')}>Export .md</button><button className="button button-light button-small" onClick={() => exportAs('pdf')}>Export PDF</button></div></div>
            {busyReview ? <div className="loading-line"><span className="spinner" /> Loading transcript and notes…</div> : <>
              <div className="notes-heading"><h3>Meeting notes</h3><label className="check-row compact-check"><input type="checkbox" checked={showDismissed} onChange={(e) => setShowDismissed(e.target.checked)} /> Show dismissed</label></div>
              <div className="notes-grid">{groups.map((group) => <section className="note-group" key={group.type}>
                <div className="group-heading"><h4>{group.label}</h4><span className="group-count">{group.items.length}</span></div>
                {group.items.length ? group.items.map((note) => <article className={`note-card ${note.dismissedAt ? 'note-card-dismissed' : ''}`} key={note.id}>
                  {editingId === note.id ? <div className="edit-form">
                    <label>Note<textarea rows="3" value={edit.content} onChange={(e) => setEdit({ ...edit, content: e.target.value })} /></label>
                    {note.type === 'TASK' && <><label>Owner<input value={edit.owner} placeholder="Not specified" onChange={(e) => setEdit({ ...edit, owner: e.target.value })} /></label><label>Due date<input type="date" value={edit.dueDate} onChange={(e) => setEdit({ ...edit, dueDate: e.target.value })} /></label></>}
                    {note.type === 'MEETING' && <><label>Date<input type="date" value={edit.meetingDate} onChange={(e) => setEdit({ ...edit, meetingDate: e.target.value })} /></label><div className="time-fields"><label>Starts<input type="time" value={edit.meetingStartTime} onChange={(e) => setEdit({ ...edit, meetingStartTime: e.target.value })} /></label><label>Ends<input type="time" value={edit.meetingEndTime} onChange={(e) => setEdit({ ...edit, meetingEndTime: e.target.value })} /></label></div></>}
                    <div className="note-actions"><button className="button button-primary button-small" onClick={() => saveEdit(note)}>Save</button><button className="text-button" onClick={() => setEditingId(null)}>Cancel</button></div>
                  </div> : <>
                    <p className="note-content">{note.content}</p>
                    {note.type === 'TASK' && <p className="note-meta">Owner: {note.owner || 'Not specified'} <span>·</span> Due: {dateLabel(note.dueDate)}</p>}
                    {note.type === 'MEETING' && <p className="note-meta">{dateLabel(note.meetingDate)} <span>·</span> {note.meetingStartTime || 'Time not specified'}{note.meetingEndTime ? `–${note.meetingEndTime}` : ''}</p>}
                    {note.reviewedAt && <span className="reviewed-label">✓ Reviewed</span>}
                    <div className="note-actions">
                      <button className="text-button evidence-button" onClick={() => { const source = transcript.find((segment) => segment.id === note.sourceSegmentId); if (source) seek(source); }}>↗ Show in transcript</button>
                      {note.calendarUrl && <a className="text-button calendar-link" href={note.calendarUrl} target="_blank" rel="noreferrer">＋ Google Calendar</a>}
                      <button className="text-button" onClick={() => beginEdit(note)}>Edit</button>
                      {note.dismissedAt ? <button className="text-button" onClick={() => setNoteState(note, Boolean(note.reviewedAt), false)}>Restore</button> : <button className="text-button" onClick={() => setNoteState(note, Boolean(note.reviewedAt), true)}>Dismiss</button>}
                      <button className="text-button danger-text" onClick={() => removeNote(note)}>Delete</button>
                      {!note.reviewedAt && <button className="button button-outline button-small" onClick={() => setNoteState(note, true, Boolean(note.dismissedAt))}>Mark reviewed</button>}
                    </div>
                  </>}
                </article>) : <p className="empty-group">No {group.label.toLowerCase()} found in this recording.</p>}
              </section>)}</div>

              <section className="transcript-section"><div className="transcript-heading"><div><p className="eyebrow">Source material</p><h3>Transcript</h3></div><span className="segment-count">{transcript.length} segments</span></div>
                {selected.audioDeleted && <div className="audio-unavailable"><span>Audio deleted after processing</span><small>The transcript and notes are still available.</small></div>}
                {transcript.length ? <div className="transcript-list">{transcript.map((segment) => <button key={segment.id} ref={(node) => { if (node) segmentRefs.current.set(segment.id, node); else segmentRefs.current.delete(segment.id); }} className={`transcript-line ${highlight === segment.id ? 'transcript-line-active' : ''}`} onClick={() => seek(segment)}><span className="timestamp">{timestamp(segment.startSeconds)}</span><span>{segment.text}</span><span className="seek-icon">▶</span></button>)}</div> : <div className="empty-transcript"><h4>{ACTIVE.has(selected.status) ? 'Transcript is on its way' : 'No transcript available'}</h4><p>{ACTIVE.has(selected.status) ? 'This recording is still being processed.' : selected.status === 'FAILED' ? 'Processing failed. Check the message above and try again.' : 'The transcript will appear here when processing is complete.'}</p></div>}
              </section>
            </>}
          </>}
        </section>
      </section>
    </main>
    {audioUrl && selected && <div className="audio-dock"><span className="dock-title">Now reviewing</span><strong>{selected.originalFilename}</strong><audio ref={audioRef} controls preload="none" src={audioUrl} onError={() => setError('Audio playback is unavailable for this recording.')} /></div>}
    <footer className="footer-note">ClearNotes organizes what was said. Missing details stay marked as “Not specified.”</footer>
    </div>
  </div>;
}

function LandingPage({ onLogin, onRegister }) {
  const [quoteIndex, setQuoteIndex] = useState(0);
  const quotes = [
    'The meeting ends. The clarity stays.',
    'Keep the decision close to the words that led to it.',
    'A follow-up is easier when the details are still there.',
  ];
  useEffect(() => {
    const timer = window.setInterval(() => setQuoteIndex((value) => (value + 1) % quotes.length), 5200);
    return () => window.clearInterval(timer);
  }, []);
  return <div className="landing-shell" id="top">
    <header className="landing-nav">
      <Brand />
      <nav className="landing-links" aria-label="Main navigation"><a href="#how-it-works">How it works</a><a href="#why-clearnotes">Why ClearNotes</a></nav>
      <div className="landing-actions"><button className="text-button landing-login" onClick={onLogin}>Sign in</button><button className="button button-primary" onClick={onRegister}>Get started</button></div>
    </header>
    <main>
      <section className="landing-hero">
        <div className="hero-copy"><p className="eyebrow">MEETING NOTES, GROUNDED IN THE RECORDING</p>
          <h1>Meetings move fast.<br /><span>Keep what matters.</span></h1>
          <p className="hero-description">ClearNotes turns a recording into a transcript, decisions, and next steps—with each note linked to its moment in the conversation.</p>
          <div className="hero-actions"><button className="button button-primary" onClick={onRegister}>Create your workspace <span aria-hidden="true">→</span></button><button className="text-button" onClick={onLogin}>I already have an account</button></div>
        </div>
        <div className="hero-visual" aria-label="ClearNotes turns recordings into transcript-linked notes">
          <div className="hero-visual-top"><span className="visual-status"><i /> A clearer record</span><span className="visual-wave" aria-hidden="true">▂ ▅ ▃ ▇ ▄ ▆ ▂ ▅ ▇ ▃ ▅ ▂ ▆ ▄</span></div>
          <div className="visual-quote"><span className="quote-mark">“</span><p>{quotes[quoteIndex]}</p><span className="quote-progress"><i style={{ transform: `translateX(${quoteIndex * 100}%)` }} /></span></div>
          <div className="visual-flow"><div className="flow-step"><span className="flow-icon">♫</span><span>Recording</span></div><span className="flow-connector" /><div className="flow-step"><span className="flow-icon">▤</span><span>Transcript</span></div><span className="flow-connector" /><div className="flow-step"><span className="flow-icon">✦</span><span>Notes</span></div></div>
          <div className="visual-tags"><span>Decision</span><span>Task</span><span>Meeting</span><span>Evidence linked</span></div>
        </div>
      </section>
      <section className="landing-proof" id="how-it-works"><p className="eyebrow">FROM AUDIO TO FOLLOW-THROUGH</p><h2>One recording. A clearer next step.</h2>
        <div className="feature-grid">
          <article className="feature-card"><span className="feature-number">01</span><div className="feature-icon">↥</div><h3>Bring the conversation</h3><p>Upload an audio file or record directly with your microphone.</p></article>
          <article className="feature-card"><span className="feature-number">02</span><div className="feature-icon">▤</div><h3>Keep the full context</h3><p>Read the transcript with timestamps and return to the moment behind a note.</p></article>
          <article className="feature-card"><span className="feature-number">03</span><div className="feature-icon">✦</div><h3>See what comes next</h3><p>Review decisions, tasks, and meeting details. Missing owners or dates stay “Not specified.”</p></article>
        </div>
      </section>
      <section className="landing-quote-band" id="why-clearnotes"><span className="quote-band-mark">“</span><p>Useful notes should help you remember what happened and show you where to listen again.</p><span className="quote-band-caption">A ClearNotes principle</span><button className="button button-light" onClick={onRegister}>Start with a recording <span aria-hidden="true">→</span></button></section>
    </main>
    <footer className="landing-footer"><Brand /><span>ClearNotes keeps each note connected to its source.</span><button className="text-button" onClick={onLogin}>Sign in</button></footer>
  </div>;
}

function Brand() {
  return <a className="brand" href="#top" aria-label="ClearNotes home"><span className="brand-icon">c</span><span>clear<span>notes</span></span></a>;
}
