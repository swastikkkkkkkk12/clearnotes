import { useEffect, useRef, useState } from 'react';

const FORMATS = [
  { mime: 'audio/webm;codecs=opus', extension: 'webm' },
  { mime: 'audio/mp4', extension: 'm4a' },
  { mime: 'audio/ogg;codecs=opus', extension: 'ogg' },
];

export default function AudioCapture({ onRecorded, disabled = false }) {
  const recorderRef = useRef(null);
  const streamRef = useRef(null);
  const chunksRef = useRef([]);
  const [recording, setRecording] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!recording) return undefined;
    const timer = window.setInterval(() => setSeconds((value) => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [recording]);

  useEffect(() => () => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
  }, []);

  async function startRecording() {
    setError('');
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
      setError('This browser cannot record audio. Choose an audio file instead.');
      return;
    }
    const format = FORMATS.find(({ mime }) => MediaRecorder.isTypeSupported(mime));
    if (!format) {
      setError('This browser does not offer a supported recording format. Choose an audio file instead.');
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      chunksRef.current = [];
      const recorder = new MediaRecorder(stream, { mimeType: format.mime });
      recorderRef.current = recorder;
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) chunksRef.current.push(event.data);
      };
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || format.mime });
        const file = new File([blob], `clearnotes-recording-${Date.now()}.${format.extension}`, {
          type: recorder.mimeType || format.mime,
        });
        onRecorded(file);
        stream.getTracks().forEach((track) => track.stop());
        streamRef.current = null;
      };
      recorder.start(1000);
      setSeconds(0);
      setRecording(true);
    } catch {
      setError('Microphone permission was not granted. Allow microphone access or choose a file.');
    }
  }

  function stopRecording() {
    recorderRef.current?.stop();
    setRecording(false);
  }

  const time = `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;

  return (
    <div className="capture-block">
      <button className={recording ? 'button button-recording' : 'button button-light'} disabled={disabled}
        type="button" onClick={recording ? stopRecording : startRecording}>
        <span className="mic-dot" aria-hidden="true">{recording ? '■' : '●'}</span>
        {recording ? `Stop recording · ${time}` : 'Record with microphone'}
      </button>
      <p className="field-hint">Your browser will ask for microphone permission.</p>
      {error && <p className="inline-error" role="alert">{error}</p>}
    </div>
  );
}
