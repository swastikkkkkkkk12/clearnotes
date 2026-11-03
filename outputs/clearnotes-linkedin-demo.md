# ClearNotes LinkedIn demo

## Demo format

Record a 60–90 second screen walkthrough with a short voiceover. Use the synthetic sample recording in `outputs/clearnotes-design-review.wav` or `outputs/clearnotes-meeting-sample-30s.wav`. Do not use real meeting audio or show real account details.

## Before recording

1. Start Docker Desktop and the ClearNotes services with `docker compose up -d`.
2. Open the app at `http://127.0.0.1:5173/` and sign in. Use a demo-only account and sample data; keep personal email addresses and real meeting details off screen.
3. Upload one sample recording. Leave “Delete audio after processing” unchecked so playback can be shown.
4. Wait until the recording status is `DONE`. Open the review and make sure the transcript, notes, and decision tracker contain results before recording.
5. Hide personal email addresses, browser bookmarks, terminal secrets, and unrelated tabs. Keep a clean browser window at a readable zoom level.

If the transcript or notes are empty, stop and fix that before recording. Do not present placeholder or failed output as a successful AI result.

## Suggested screen sequence

| Time | Show | Narration cue |
| --- | --- | --- |
| 0:00–0:08 | ClearNotes landing page | “Meeting follow-through starts with a recording.” |
| 0:08–0:20 | Sign in, then upload the sample audio | “I can upload a file or record directly in the browser.” |
| 0:20–0:33 | Processing status, then the completed review | “ClearNotes transcribes the audio and prepares notes for review.” |
| 0:33–0:48 | Timestamped transcript and a decision or task | “Each note points back to transcript evidence, and clicking it takes me to that moment in the audio.” |
| 0:48–1:02 | Decision Tracker fields and follow-up status | “The decision log keeps the decision, approver, evidence, timestamp, and follow-up status together.” |
| 1:02–1:12 | Play audio, then briefly show export controls | “I can listen again and export the reviewed notes.” |
| 1:12–1:20 | ClearNotes title or landing page | “ClearNotes: a clearer path from conversation to follow-through.” |

The exact results depend on the sample transcript. If the recording does not contain an explicit owner or date, show “Not specified” rather than adding one yourself. Only show the Calendar action if ClearNotes produced it from a clearly stated date and time.

## Voiceover draft

“Meetings create decisions and action items, but details are easy to lose. ClearNotes turns a recording into a timestamped transcript and notes linked to the words that support them. I can upload audio or record in the browser. Once processing finishes, I can review decisions and tasks, jump back to their moments in the transcript, and listen again. The Decision Tracker keeps each decision, its approver, supporting evidence, timestamp, and follow-up status in one place. If an owner or date was not stated, ClearNotes leaves it unspecified. I built this portfolio project with React, Spring Boot, FastAPI, MySQL, Faster-Whisper, and Ollama.”

## LinkedIn post draft

I built **ClearNotes**, a meeting audio app that helps carry decisions and action items past the end of a conversation.

Upload a recording or capture one in the browser. ClearNotes creates a timestamped transcript, extracts decisions and tasks, and links notes back to transcript evidence so you can return to the exact moment. The Decision Tracker records the decision, approver, evidence, timestamp, and follow-up status. Missing details remain “Not specified.”

Built with React, Java 17 / Spring Boot, MySQL, FastAPI, Faster-Whisper, and Ollama. This demo uses synthetic sample audio; AI-generated notes should be reviewed against the transcript.

I built ClearNotes to explore evidence-linked AI, full-stack development, and practical meeting workflows. Feedback is welcome.

#BuildInPublic #Java #SpringBoot #React #AI #PortfolioProject

## Sharing checklist

- Record at 1080p if available; keep the cursor movements deliberate and pause briefly on the transcript evidence and decision tracker.
- Add captions to the video and use the voiceover draft as a starting point, not as a claim that every recording yields perfect notes.
- Use a short title card: **ClearNotes | From meeting audio to evidence-linked follow-through**.
- Link the GitHub repository in the post. Add a live demo link only after the Vercel frontend and its API connection are deployed and verified.
- The temporary Cloudflare Quick Tunnel URL changes when restarted and depends on your Mac staying online, so do not use it as a permanent portfolio link.

## Record the screen on Mac

1. Open the ClearNotes browser window and arrange it so the relevant controls and results fit on screen.
2. Press **Command + Shift + 5**.
3. Choose **Record Selected Portion** and frame the browser content, or choose **Record Entire Screen** if the window is already clean.
4. Under **Options**, choose the microphone if you want to narrate while recording. Otherwise, record the screen silently and add the voiceover later.
5. Click **Record**, follow the screen sequence above, then stop from the menu bar recording control.
6. Watch the exported clip once before posting. Confirm the transcript, evidence links, notes, audio playback, and decision tracker are legible and no private data is visible.
