const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const SHA256_ZERO = "a".repeat(64);

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("private_folders").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("prompts_personas").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("chat_backups").get()
  );
});

test("Owner can create and partially update UserAccountProfile; cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Minh Nguyen",
      folderKey: "MinhNguyen_alice_uid_123",
      linkedProviders: ["google.com"],
      preferredLanguageCode: "vi",
      biometricEnabled: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Minh Pro",
      biometricEnabled: true,
      recoveryEmail: "backup@example.com",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user read or write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Private folders and sub-1MiB chunks enforce parent existence and ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Orphan chunk without parent metadata doc must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("private_folders")
      .doc("item_1")
      .collection("chunks")
      .doc("chunk_0")
      .set({
        userId: ALICE_UID,
        itemId: "item_1",
        chunkIndex: 0,
        payloadBase64: "SGVsbG8=",
        chunkSha256: SHA256_ZERO,
        createdAt: past,
      })
  );

  // Create parent metadata item
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("private_folders")
      .doc("item_1")
      .set({
        userId: ALICE_UID,
        folderKey: "MinhNguyen_alice_uid_123",
        fileName: "notes.paq",
        mimeType: "application/x-paq",
        compressionMethod: "PAQ",
        originalSizeBytes: 2048,
        compressedSizeBytes: 512,
        sha256Hash: SHA256_ZERO,
        chunkCount: 1,
        category: "BACKUP",
        createdAt: past,
        updatedAt: past,
      })
  );

  // Valid chunk succeeds once parent exists
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("private_folders")
      .doc("item_1")
      .collection("chunks")
      .doc("chunk_0")
      .set({
        userId: ALICE_UID,
        itemId: "item_1",
        chunkIndex: 0,
        payloadBase64: "SGVsbG8=",
        chunkSha256: SHA256_ZERO,
        createdAt: past,
      })
  );

  // List subcollection succeeds for owner even when querying
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private_folders").get()
  );

  // Bob cannot list or read Alice's private folder
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private_folders").get()
  );
});

test("Prompts/personas and chat backups enforce schema and reject future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("prompts_personas")
      .doc("tmpl_1")
      .set({
        userId: ALICE_UID,
        kind: "PROMPT",
        title: "Kotlin Clean Code",
        category: "Lập trình",
        content: "Viết mã Kotlin chuẩn MVVM kèm kiểm thử.",
        preferredLanguageTag: "kotlin",
        createdAt: past,
        updatedAt: past,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chat_backups")
      .doc("sess_1")
      .set({
        userId: ALICE_UID,
        title: "Kiến trúc Super Smart AI",
        modelLabel: "Gemini 3.8 Flash",
        messageCount: 12,
        paqSummaryBase64: "UEFR",
        sha256Hash: SHA256_ZERO,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chat_backups")
      .doc("sess_future")
      .set({
        userId: ALICE_UID,
        title: "Future",
        modelLabel: "Gemini",
        messageCount: 1,
        paqSummaryBase64: "UEFR",
        sha256Hash: SHA256_ZERO,
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );
});

test("Security locks enforce owner access, salted hash schema, grid sizes 3..10, and immutable target keys", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 5000);

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("security_locks").get()
  );

  // Valid App Lock (Pattern 10x10)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("security_locks")
      .doc("app_lock")
      .set({
        userId: ALICE_UID,
        targetType: "APP",
        targetId: "APP",
        passwordMode: "PATTERN",
        patternGridSize: 10,
        saltBase64: "c2FsdDEyMzQ1Njc4OTA=",
        hashBase64: "aGFzaDEyMzQ1Njc4OTBhYmNkZWY=",
        unlimitedAttempts: false,
        maxAttempts: 999999,
        failedAttempts: 0,
        lockedOut: false,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Valid partial update incrementing failedAttempts
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("security_locks")
      .doc("app_lock")
      .update({
        failedAttempts: 1,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // List security locks as owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("security_locks").get()
  );

  // Reject invalid patternGridSize (11 > 10 or 2 < 3)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("security_locks")
      .doc("chat_s1")
      .set({
        userId: ALICE_UID,
        targetType: "CHAT",
        targetId: "s1",
        passwordMode: "PATTERN",
        patternGridSize: 11,
        saltBase64: "c2FsdA==",
        hashBase64: "aGFzaA==",
        unlimitedAttempts: true,
        maxAttempts: 0,
        failedAttempts: 0,
        lockedOut: false,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Reject tampering targetType on update
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("security_locks")
      .doc("app_lock")
      .update({
        targetType: "CHAT",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Reject Bob reading or writing Alice's security locks
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("security_locks").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("security_locks")
      .doc("app_lock")
      .delete()
  );
});

test("Security locks support CUSTOM passwordMode and bounded autoLockTimeoutMillis policy", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Valid CUSTOM per-chat password lock
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("security_locks")
      .doc("chat_custom_1")
      .set({
        userId: ALICE_UID,
        targetType: "CHAT",
        targetId: "custom_1",
        passwordMode: "CUSTOM",
        patternGridSize: 0,
        saltBase64: "c2FsdDEyMzQ1Njc4OTA=",
        hashBase64: "aGFzaDEyMzQ1Njc4OTBhYmNkZWY=",
        unlimitedAttempts: true,
        maxAttempts: 0,
        failedAttempts: 0,
        lockedOut: false,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Valid lock_policy with autoLockTimeoutMillis (5 minutes = 300000ms)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("security_locks")
      .doc("lock_policy")
      .set({
        userId: ALICE_UID,
        targetType: "POLICY",
        targetId: "GLOBAL_POLICY",
        passwordMode: "POLICY",
        patternGridSize: 0,
        saltBase64: "",
        hashBase64: "",
        unlimitedAttempts: true,
        maxAttempts: 0,
        failedAttempts: 0,
        lockedOut: false,
        autoLockTimeoutMillis: 300000,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Valid update to never auto-lock (-1) and immediate lock (0)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("security_locks")
      .doc("lock_policy")
      .update({
        autoLockTimeoutMillis: -1,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject out-of-range autoLockTimeoutMillis (-2)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("security_locks")
      .doc("lock_policy")
      .update({
        autoLockTimeoutMillis: -2,
        updatedAt: new Date(Date.now() - 500),
      })
  );
});

