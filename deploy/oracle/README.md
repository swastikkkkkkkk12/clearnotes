# ClearNotes on an Oracle Always Free VM

Vercel hosts the React website. This VM runs the existing Spring Boot API,
MySQL, FastAPI/Whisper, Ollama, and persistent audio files. The database,
audio, and downloaded AI models stay on the VM's attached storage.

## 1. Create the VM

In Oracle Cloud, create an Ubuntu ARM64 VM using the Always Free A1 shape with
2 OCPUs and 12 GB RAM. If Oracle offers only a paid shape or paid capacity,
stop instead of upgrading. Assign a public IPv4 address.

In the VM's network security rules, allow inbound TCP 22 from your current
public IP, and TCP 80 and 443 from the internet. Do not open ports 3306, 8000,
8080, or 11434; Compose keeps MySQL, Whisper, the API, and Ollama private.

## 2. Prepare the VM

Install Docker Engine and the Docker Compose plugin using Docker's official
Ubuntu instructions. Then clone the ClearNotes repository onto the VM and
enter the project folder.

```sh
git clone https://github.com/swastikkkkkkkk12/clearnotes.git
cd clearnotes
```

The repository must include this `deploy/oracle` directory before cloning.
From the project folder, create the private environment file:

```sh
cd deploy/oracle
cp .env.example .env
```

Edit `.env`. Replace both database password placeholders and `JWT_SECRET`
with separate outputs from `openssl rand -hex 32`. Set `API_DOMAIN` to the
VM's public IPv4 address followed by `.nip.io` (for example,
`203.0.113.10.nip.io`). Set `FRONTEND_ORIGIN` after the Vercel site has its
production URL. Never commit or share `.env`.

## 3. Start the backend services

From `deploy/oracle`, build and start the services:

```sh
docker compose up -d --build
```

Download the notes model once:

```sh
docker compose exec ollama ollama pull llama3.2:3b
```

Caddy will request an HTTPS certificate for `API_DOMAIN`. This requires the
VM's public address to resolve through nip.io and inbound ports 80 and 443 to
be open. Check the API health response:

```sh
curl https://YOUR_PUBLIC_IP.nip.io/actuator/health
```

Expected response: `{"status":"UP"}`.

## 4. Deploy the website on Vercel

In Vercel, set the project root to `frontend` (or deploy from that directory)
and add this production environment variable:

```text
VITE_API_BASE_URL=https://YOUR_PUBLIC_IP.nip.io
```

Deploy the Vercel project. Copy its production website origin, such as
`https://clearnotes.example.vercel.app`, into `FRONTEND_ORIGIN` in the VM's
`.env`, then restart only the API:

```sh
docker compose up -d backend
```

Now register an account and test an audio upload, transcription, notes, and
audio playback from the Vercel website.

## Ongoing use

Update from the project folder with `git pull`, then from `deploy/oracle` run
`docker compose up -d --build`. The named Compose volumes retain database
records, audio, Whisper's model cache, Ollama models, and Caddy certificates.
Keep a separate backup of important recordings and database data.
