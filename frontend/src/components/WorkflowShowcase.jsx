import { useEffect, useState } from 'react';

const steps = [
  { label: 'Capture', kicker: '01 / AUDIO IN', title: 'Start with the conversation.', copy: 'Upload a recording or capture audio with your microphone. ClearNotes keeps the original available for review unless you choose to delete it after processing.' },
  { label: 'Transcribe', kicker: '02 / WORDS WITH TIME', title: 'Hear every moment in context.', copy: 'Faster-Whisper turns speech into timestamped transcript segments, so the conversation stays easy to revisit.' },
  { label: 'Find the follow-through', kicker: '03 / NOTES WITH EVIDENCE', title: 'Turn what was said into next steps.', copy: 'Ollama helps identify decisions, tasks, and meeting details. Each note points back to its transcript source.' },
  { label: 'Review', kicker: '04 / HUMAN REVIEW', title: 'Check the evidence. Keep what matters.', copy: 'Review the decision log, approver, source moment, and follow-up status. Details the conversation did not state stay unspecified.' },
];

export default function WorkflowShowcase({ onGetStarted }) {
  const [active, setActive] = useState(0);
  const [playing, setPlaying] = useState(true);
  const [captureMode, setCaptureMode] = useState('Upload');

  useEffect(() => {
    if (!playing) return undefined;
    const timer = window.setInterval(() => setActive((value) => (value + 1) % steps.length), 5200);
    return () => window.clearInterval(timer);
  }, [playing]);

  const step = steps[active];
  const next = () => setActive((value) => (value + 1) % steps.length);
  const previous = () => setActive((value) => (value + steps.length - 1) % steps.length);

  return <section className="workflow-showcase" aria-labelledby="workflow-title">
    <div className="workflow-heading">
      <div><p className="eyebrow">A CLEARER MEETING WORKFLOW</p><h2 id="workflow-title">From spoken words to follow-through.</h2></div>
      <span className="workflow-live"><i /> INTERACTIVE WALKTHROUGH</span>
    </div>
    <div className="workflow-layout">
      <div className="workflow-stage" aria-live="polite" aria-atomic="true">
        <div className="workflow-stage-top"><span>{step.kicker}</span><span className="workflow-stage-count">{String(active + 1).padStart(2, '0')} <b>/ 04</b></span></div>
        <div className={`workflow-art workflow-art-${active}`} key={active}>
          {active === 0 && <div className="capture-art">
            <div className="capture-orb"><span className="capture-mic">{captureMode === 'Upload' ? '↥' : '●'}</span><div className="capture-rings" /></div>
            <div className="capture-file"><span className="file-icon">♫</span><span><b>{captureMode === 'Upload' ? 'team-sync.wav' : 'Live recording'}</b><small>{captureMode === 'Upload' ? 'Audio ready to process' : 'Microphone capture'}</small></span><i>READY</i></div>
            <div className="capture-modes" aria-label="Choose a capture example">{['Upload', 'Microphone'].map((mode) => <button key={mode} className={captureMode === mode ? 'capture-mode-active' : ''} onClick={() => setCaptureMode(mode)}>{mode === 'Upload' ? '↥' : '◉'} {mode}</button>)}</div>
          </div>}
          {active === 1 && <div className="transcript-art">
            <div className="mini-player"><span className="mini-play" aria-hidden="true">▶</span><div><b>Weekly team sync</b><small>00:14 / 00:32</small></div><div className="mini-wave" aria-hidden="true">▂ ▄ ▆ ▃ ▇ ▅ ▂ ▆ ▄ ▇ ▃ ▅</div></div>
            <div className="mini-transcript"><div className="transcript-progress" /><p><time>00:04</time><span>We agreed to move the launch to Monday.</span></p><p className="transcript-highlight"><time>00:12</time><span>Maya will share the final design by Thursday.</span><b>PLAYING</b></p><p><time>00:21</time><span>Let’s review progress in our next check-in.</span></p></div>
            <span className="art-footnote">TIMESTAMPS KEEP THE FULL CONTEXT</span>
          </div>}
          {active === 2 && <div className="notes-art">
            <div className="note-art-top"><span>MEETING NOTES</span><span>3 FOUND</span></div>
            <article className="mini-note mini-note-decision"><i>✓</i><div><small>DECISION</small><b>Move the launch to Monday</b><span>↗ 00:04 in transcript</span></div></article>
            <article className="mini-note mini-note-task"><i>↗</i><div><small>TASK · OWNER: MAYA</small><b>Share the final design</b><span>Due Thursday · 00:12</span></div></article>
            <article className="mini-note mini-note-meeting"><i>◷</i><div><small>FOLLOW-UP</small><b>Next check-in</b><span>Date not specified · 00:21</span></div></article>
          </div>}
          {active === 3 && <div className="review-art">
            <div className="review-art-top"><span>DECISION TRACKER</span><span className="open-pill">OPEN</span></div>
            <h3>Move the launch to Monday</h3>
            <div className="evidence-box"><small>SUPPORTING TRANSCRIPT · 00:04</small><p>“We agreed to move the launch to Monday.”</p><span>↗ Return to this moment</span></div>
            <div className="review-meta"><div><small>APPROVED BY</small><b>Not specified</b></div><div><small>FOLLOW-UP</small><b>Open</b></div></div>
            <div className="review-footer"><span><i /> Source linked</span><span>Ready for your review</span></div>
          </div>}
        </div>
        <div className="workflow-stage-caption"><div><h3>{step.title}</h3><p>{step.copy}</p></div><button className="workflow-start button button-primary" onClick={onGetStarted}>Try ClearNotes <span aria-hidden="true">→</span></button></div>
      </div>
      <div className="workflow-steps">
        <div className="workflow-step-list" role="tablist" aria-label="ClearNotes workflow stages">
          {steps.map((item, index) => <button key={item.label} role="tab" aria-selected={active === index} className={`workflow-step-button ${active === index ? 'is-active' : ''} ${index < active ? 'is-complete' : ''}`} onClick={() => setActive(index)}>
            <span className="workflow-step-index">{index < active ? '✓' : `0${index + 1}`}</span><span className="workflow-step-label">{item.label}</span><span className="workflow-step-arrow">↗</span>
          </button>)}
        </div>
        <div className="workflow-controls"><div className="workflow-controls-left"><button onClick={previous} aria-label="Previous workflow stage">←</button><button onClick={next} aria-label="Next workflow stage">→</button><span>{String(active + 1).padStart(2, '0')} / 04</span></div><button className="workflow-play" onClick={() => setPlaying((value) => !value)} aria-label={playing ? 'Pause workflow animation' : 'Play workflow animation'}>{playing ? 'Ⅱ Pause' : '▶ Play'}</button></div>
        <div className="workflow-progress" aria-hidden="true"><i style={{ transform: `scaleX(${(active + 1) / steps.length})` }} /></div>
        <p className="workflow-note">Built to keep decisions connected to what was actually said.</p>
      </div>
    </div>
  </section>;
}
