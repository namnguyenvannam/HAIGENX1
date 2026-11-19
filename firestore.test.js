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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

function validProfilePayload(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "Miner Alice",
    balanceHx1: 120.5,
    energy: 95.0,
    hashrateLevel: 1,
    tapPowerLevel: 0,
    energyLevel: 0,
    multiplierLevel: 1,
    turboMultiplierLevel: 0,
    boostExpiresAtMillis: 0,
    simulatedPriceUsdt: 0.000001,
    usdtVirtualIndex: 100.0,
    lastActiveMillis: Date.now() - 1000,
    createdAt: now,
    updatedAt: now,
  };
}

function validParticipantPayload(uid) {
  return {
    userId: uid,
    createdAt: new Date(Date.now() - 1000),
  };
}

function validTradePayload(uid) {
  return {
    userId: uid,
    direction: "BUY",
    leverage: 5,
    entryFeeHx1: 50.0,
    entryPriceUsdt: 0.000001,
    exitPriceUsdt: 0.0000022,
    outcome: "WIN",
    payoutHx1: 90.0,
    netPnlHx1: 40.0,
    timestampMillis: Date.now() - 1000,
    createdAt: new Date(Date.now() - 1000),
  };
}

function validMiningPayload(uid) {
  return {
    userId: uid,
    amountHx1: 2.5,
    sourceType: "MANUAL",
    hashrateSnapshot: 12.5,
    timestampMillis: Date.now() - 1000,
    createdAt: new Date(Date.now() - 1000),
  };
}

test("Unauthenticated user cannot read or write participants, profile, trades, or miningRecords", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("participants").get());
  await assertFails(unauthDb.collection("participants").doc(ALICE_UID).set(validParticipantPayload(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("trades").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("miningRecords").get());
});

test("Participants collection enforces once-per-UID create, read/count access, and forbids update/delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const alicePartRef = aliceDb.collection("participants").doc(ALICE_UID);

  // Alice can register once with matching uid
  await assertSucceeds(alicePartRef.set(validParticipantPayload(ALICE_UID)));

  // Alice cannot overwrite/update or delete her participant document
  await assertFails(alicePartRef.update({ userId: ALICE_UID }));
  await assertFails(alicePartRef.delete());

  // Alice cannot create a participant doc under Bob's UID or spoof userId
  await assertFails(aliceDb.collection("participants").doc(BOB_UID).set(validParticipantPayload(ALICE_UID)));
  await assertFails(aliceDb.collection("participants").doc(BOB_UID).set(validParticipantPayload(BOB_UID)));

  // Signed-in users can read/list/count participants
  await assertSucceeds(bobDb.collection("participants").get());
  await assertSucceeds(bobDb.collection("participants").doc(BOB_UID).set(validParticipantPayload(BOB_UID)));
});

test("Owner can create and partially update profile including multipliers while preserving createdAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(profileRef.get());

  await assertSucceeds(
    profileRef.update({
      balanceHx1: 155.75,
      hashrateLevel: 2,
      multiplierLevel: 3,
      turboMultiplierLevel: 1,
      displayName: "Master Miner",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / shadow update
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future client timestamp
  await assertFails(
    profileRef.update({
      balanceHx1: 200.0,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Owner cannot spoof userId or include @DocumentId id field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const spoofed = validProfilePayload(BOB_UID);
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(spoofed));

  const withIdField = { ...validTradePayload(ALICE_UID), id: "trade_1" };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("trades").doc("trade_1").set(withIdField)
  );
});

test("Cross-user isolation: Bob cannot read or write Alice data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID));
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("trades").doc("trade_1").set(validTradePayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ balanceHx1: 9999 }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("trades").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("trades").doc("trade_1").set(validTradePayload(BOB_UID)));
});

test("Owner can create & list trades and miningRecords with Kotlin-mirrored queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tradesCol = aliceDb.collection("users").doc(ALICE_UID).collection("trades");
  const miningCol = aliceDb.collection("users").doc(ALICE_UID).collection("miningRecords");

  // Empty subcollection list queries must succeed
  await assertSucceeds(tradesCol.orderBy("timestampMillis", "desc").limit(50).get());
  await assertSucceeds(miningCol.orderBy("timestampMillis", "desc").limit(50).get());

  // Valid creates succeed
  await assertSucceeds(tradesCol.doc("trade_1").set(validTradePayload(ALICE_UID)));
  await assertSucceeds(miningCol.doc("mine_1").set(validMiningPayload(ALICE_UID)));

  // Populated queries succeed
  await assertSucceeds(tradesCol.orderBy("timestampMillis", "desc").limit(50).get());
  await assertSucceeds(miningCol.orderBy("timestampMillis", "desc").limit(50).get());

  // Trades and mining records are immutable (updates rejected)
  await assertFails(tradesCol.doc("trade_1").update({ outcome: "WIN", payoutHx1: 999 }));
  await assertFails(miningCol.doc("mine_1").update({ amountHx1: 999 }));

  // Invalid leverage > 10 fails
  await assertFails(
    tradesCol.doc("trade_bad").set({ ...validTradePayload(ALICE_UID), leverage: 25 })
  );
});
