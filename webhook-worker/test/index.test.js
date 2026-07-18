import assert from "node:assert/strict";
import { createHmac, webcrypto } from "node:crypto";
import test from "node:test";

import {
  parseGitHubPayload,
  verifyGitHubSignature,
} from "../src/index.js";

if (!globalThis.crypto) {
  globalThis.crypto = webcrypto;
}

function signatureFor(body, secret) {
  return `sha256=${createHmac("sha256", secret).update(body).digest("hex")}`;
}

test("accepts a valid GitHub HMAC signature", async () => {
  const body = JSON.stringify({ action: "purchased" });
  const secret = "test-secret";

  assert.equal(
    await verifyGitHubSignature(body, signatureFor(body, secret), secret),
    true,
  );
});

test("rejects invalid or malformed signatures", async () => {
  const body = JSON.stringify({ action: "purchased" });

  assert.equal(
    await verifyGitHubSignature(body, signatureFor(body, "wrong"), "expected"),
    false,
  );
  assert.equal(await verifyGitHubSignature(body, "sha256=broken", "expected"), false);
  assert.equal(await verifyGitHubSignature(body, null, "expected"), false);
});

test("parses JSON payloads", () => {
  assert.deepEqual(
    parseGitHubPayload('{"action":"changed"}', "application/json"),
    { action: "changed" },
  );
});

test("parses form-encoded GitHub payloads", () => {
  const payload = encodeURIComponent('{"action":"cancelled"}');

  assert.deepEqual(
    parseGitHubPayload(`payload=${payload}`, "application/x-www-form-urlencoded"),
    { action: "cancelled" },
  );
});
