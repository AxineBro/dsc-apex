# DSK Chat Widget — Frontend

Embeddable AI consultant for the DSK website. `React 19` + `Vite`, single-file `widget.js` build, style isolation via `iframe`.

## What Is Built

- `closed` / `floating` / `full` modes + `mobile <=640px` adaptive
- `floating` — compact 400x680 chat with header, quick chips, copy / edit / regenerate
- `full` — modal with 24px offset + dimming, `SideMenu` 280px, `welcome-screen` with 4 cards
- Suggestions system: 20 prompts in 4 groups, 1 random pick per group
- `ThinkingIndicator`: 9 statuses rotated every 4.5-5.5s, first is always `Thinking…`
- `Markdown` tables, `sys-info` pill for manager handoff, `error` bubble
- Backend integration: `GET /api/v1/chat/init` + `POST /api/v1/chat/message` + `X-Session-Id`
- DSK homepage copy in `index.html` + `public/CopyAssetsDSK/` for the `1 line and it works` demo

## Cool Architectural Decisions

| Decision | Why |
| :--- | :--- |
| `iframe` without `src` (`about:blank`) | Copy styles and widget styles never collide, `origin` is inherited from parent |
| All `CSS` via `import.meta.glob` + `?inline` | Single `<style>` inside `iframe`, no external files to carry |
| `IIFE` lib build to single `widget.js` | `React` bundled inside, logos as `base64`, one-file deploy |
| `postMessage({ type: 'CHAT_MODE_CHANGE', mode })` | Parent switches `iframe` `className` and locks body scroll in `full` |
| Relative `fetch('/api/...')` | Dev goes `vite proxy -> :8080`, prod goes `nginx -> backend:8080`, no rebuild needed |
| Separate `isThinking` vs `isLoading` | Indicator hides the moment `reply` arrives, button unlocks after typewriter finishes |
| `welcome` as message + filter in `full` | `floating` keeps the bubble on top, `full` shows only `welcome-screen`, no jumps |
| `typewriter` streaming imitation | Backend returns whole `JSON`, we print it in chunks for UX, no real `SSE` |

## Structure

```text
frontend/
  index.html                  # DSK homepage copy, dev tag /src/main.tsx
  public/CopyAssetsDSK/       # copy assets, vite copies to dist/
  src/main.tsx                # entry, iframe mount + createRoot
  src/InitWidget.css          # iframe sizes closed/floating/full
  src/api/chatApi.ts          # initSession + sendMessage + isManagerReply
  src/Components/
    ChatWidget/               # mode, sid, messages, isLoading/isThinking
    ChatLayout/               # floating header vs full floating-controls
    ChatWindow/               # welcome-screen vs history
    MessagesHistory/          # welcome filter, autoscroll
    Messgae/Message.*         # bubbles + markdown + toolbar
    PromptInputField/         # autogrow 3/6 rows, disabled state
    SideMenu/                 # history stub, collapse animation
    Suggestions/              # suggestions.ts + chips/cards
    ThinkingIndicator/        # dots + icon + text, no bubble
    ChatButtons/              # open / resize / close
  dist/                       # build output, NOT in git
```

## Modes and Sizes

```text
closed   -> iframe 76x76, button 60px
floating -> iframe min(400px, 100vw-40) x min(680px, 100vh-40)
full     -> iframe 100vw x 100vh, inner card calc(100%-48px) + 100vmax shadow
mobile   -> floating opens full directly, 100vw x 100dvh with no offsets
```

## API Integration

```ts
// src/api/chatApi.ts
const API_BASE = ''; // empty = relative path
GET /api/v1/chat/init -> { sessionId }
POST /api/v1/chat/message + X-Session-Id + { message } -> { reply, sessionId }
```

`sid` is stored in `localStorage` under `dsk_sid`. `New chat` and `TO_MANAGER` run `removeItem`.

## Suggestions

```ts
// src/Components/Suggestions/suggestions.ts
SUGGESTION_GROUPS = [pick x5, mortgage x5, build x5, docs x5]
getRandomStarter() -> 4 items, 1 per group
```

To add or remove — edit only the array. Icons are mapped in `Suggestions.tsx` via `?react`.

## Build and Demo

```bash
npm ci
npm run dev          # http://localhost:5173, /api -> :8080 via proxy
npm run build        # tsc + vite + cp index.html dist/
npx serve dist -l 4173  # layout only, /api fails here - expected
```

Integration on any site:

```html
<script src="/widget.js"></script>
```

<details>
<summary>Troubleshooting</summary>

- `file://` never works for the copy (`BX`, `$`, `CORS`) — use only `http://`
- `process is not defined` — already fixed via `define` in `vite.config.ts`
- `Loading /src/main.tsx blocked` — remove dev tag from `dist/index.html`, it is swapped by the `build` script
- `NetworkError` via `serve` — normal, there is no proxy there, test AI via `dev`
- `dist/` is wiped on build — keep assets in `public/`, not in `dist/`

</details>
