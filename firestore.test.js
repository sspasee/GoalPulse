const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

test("Unauthenticated user: cannot read or create savings goals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("goals").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: can create and query own savings goal with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const goalRef = aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("goal_1");

  await assertSucceeds(
    goalRef.set({
      userId: ALICE_UID,
      title: "Отпуск на море",
      targetAmount: 150000,
      currentAmount: 35000,
      currencySymbol: "₽",
      colorHex: "#10B981",
      targetDateMillis: Date.now() + 86400000 * 60,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("goals").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: cannot read or update another user's savings goal", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("goals").doc("bob_goal").set({
      userId: BOB_UID,
      title: "Bob Car",
      targetAmount: 500000,
      currentAmount: 10000,
      currencySymbol: "₽",
      colorHex: "#3B82F6",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("goals").doc("bob_goal").get()
  );
});

test("Shadow update test: rejects ghost fields on goal creation and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const goalRef = aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("goal_ghost");

  await assertFails(
    goalRef.set({
      userId: ALICE_UID,
      title: "Ghost Goal",
      targetAmount: 10000,
      currentAmount: 0,
      currencySymbol: "₽",
      colorHex: "#10B981",
      isVerifiedAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: can create and query own transactions and cannot update them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1");

  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      goalId: "goal_1",
      amount: 5000,
      note: "Пополнение с зарплаты",
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").where("userId", "==", ALICE_UID).get()
  );

  await assertFails(
    txRef.update({ amount: 999999 })
  );
});
