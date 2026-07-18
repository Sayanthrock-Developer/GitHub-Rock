const encoder = new TextEncoder();

const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
  "x-content-type-options": "nosniff",
  "content-security-policy": "default-src 'none'",
};

const MAX_BODY_BYTES = 1_048_576;

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: JSON_HEADERS,
  });
}

function hexToBytes(hex) {
  if (!/^[0-9a-f]{64}$/i.test(hex)) {
    return null;
  }

  const bytes = new Uint8Array(hex.length / 2);
  for (let index = 0; index < hex.length; index += 2) {
    bytes[index / 2] = Number.parseInt(hex.slice(index, index + 2), 16);
  }
  return bytes;
}

export async function verifyGitHubSignature(rawBody, signatureHeader, secret) {
  if (!secret || !signatureHeader?.startsWith("sha256=")) {
    return false;
  }

  const expectedSignature = hexToBytes(signatureHeader.slice("sha256=".length));
  if (!expectedSignature) {
    return false;
  }

  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["verify"],
  );

  return crypto.subtle.verify(
    "HMAC",
    key,
    expectedSignature,
    encoder.encode(rawBody),
  );
}

export function parseGitHubPayload(rawBody, contentType = "") {
  if (contentType.toLowerCase().includes("application/x-www-form-urlencoded")) {
    const payload = new URLSearchParams(rawBody).get("payload");
    if (!payload) {
      throw new Error("Missing form payload");
    }
    return JSON.parse(payload);
  }

  return JSON.parse(rawBody);
}

async function handleWebhook(request, env) {
  const configuredSecret = env.MARKETPLACE_WEBHOOK_SECRET?.trim();
  if (!configuredSecret) {
    return jsonResponse({ error: "Webhook secret is not configured" }, 503);
  }

  const contentLength = Number.parseInt(request.headers.get("content-length") ?? "0", 10);
  if (Number.isFinite(contentLength) && contentLength > MAX_BODY_BYTES) {
    return jsonResponse({ error: "Payload too large" }, 413);
  }

  const rawBody = await request.text();
  if (encoder.encode(rawBody).byteLength > MAX_BODY_BYTES) {
    return jsonResponse({ error: "Payload too large" }, 413);
  }

  const signature = request.headers.get("x-hub-signature-256");
  const isValid = await verifyGitHubSignature(rawBody, signature, configuredSecret);
  if (!isValid) {
    return jsonResponse({ error: "Invalid webhook signature" }, 401);
  }

  const event = request.headers.get("x-github-event")?.trim();
  const deliveryId = request.headers.get("x-github-delivery")?.trim();
  if (!event || !deliveryId) {
    return jsonResponse({ error: "Missing GitHub event headers" }, 400);
  }

  let payload;
  try {
    payload = parseGitHubPayload(rawBody, request.headers.get("content-type") ?? "");
  } catch {
    return jsonResponse({ error: "Invalid webhook payload" }, 400);
  }

  if (event === "ping") {
    return jsonResponse({ ok: true, event, deliveryId });
  }

  if (event !== "marketplace_purchase") {
    return jsonResponse({ ok: true, ignored: true, event, deliveryId }, 202);
  }

  const action = typeof payload?.action === "string" ? payload.action : "unknown";

  // The current Marketplace plan is free, so no entitlement database is required.
  // Do not log the payload, OAuth tokens, email addresses, or account details.
  console.log(JSON.stringify({ service: "github-rock-marketplace", event, action, deliveryId }));

  return jsonResponse({
    ok: true,
    accepted: true,
    event,
    action,
    deliveryId,
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/health") {
      return jsonResponse({
        status: "ok",
        service: "github-rock-marketplace-webhook",
      });
    }

    if (request.method === "POST" && url.pathname === "/github/marketplace/webhook") {
      return handleWebhook(request, env);
    }

    return jsonResponse({ error: "Not found" }, 404);
  },
};
