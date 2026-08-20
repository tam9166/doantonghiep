# Security rules

Never expose passwords, hashes, secrets, token versions, bank data or unnecessary authorities. Derive self-service staff identity from the authenticated principal. Keep internal and external HTTP clients separate. Validate uploads by size, MIME, magic bytes and safe filename; authorize downloads. Do not trust X-Forwarded-For unless request came through a configured trusted proxy. Never log JWTs, passwords, unmasked payment data or unnecessary PII.
