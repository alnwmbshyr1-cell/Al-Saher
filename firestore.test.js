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

test("Unauthenticated access is denied across all user collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      userId: ALICE_UID,
      title: "Test",
      mode: "AGENT",
      category: "GENERAL",
      status: "PLANNING",
      prompt: "Run task",
      summary: "",
      stepTitles: ["Plan"],
      currentStepIndex: 0,
      deliverableTitle: "",
      deliverableContent: "",
      creditsUsed: 0,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("User profile: signup 1000 credits FREE tier succeeds, tampering & cross-user fails", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "أحمد الساحر",
      email: "ahmed@example.com",
      plan: "FREE",
      creditsBalance: 1000,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject creating profile with pre-inflated credits or PRO plan on signup
  await assertFails(
    bob.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      plan: "PRO",
      creditsBalance: 99999,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Allow credit deduction update
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      creditsBalance: 980,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Allow plan upgrade update
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      plan: "PRO",
      creditsBalance: 5000,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Cross-user read/write denied
  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bob.collection("users").doc(ALICE_UID).update({ creditsBalance: 0, updatedAt: new Date(Date.now() - 100) })
  );
});

test("AgentTask & TaskMessage: Kotlin-aligned create, list, edit, stop/complete, and negative guards", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  const taskRef = alice.collection("users").doc(ALICE_UID).collection("tasks").doc("task_01");
  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      title: "حملة إعلانية لساعة ذكية",
      mode: "AGENT",
      category: "VIDEO",
      status: "PLANNING",
      prompt: "أنشئ فيديو إعلاني احترافي لساعة ذكية جديدة",
      summary: "",
      stepTitles: ["بحث الفكرة", "كتابة السيناريو", "توليد المشاهد", "المونتاج الصوتي"],
      currentStepIndex: 0,
      deliverableTitle: "",
      deliverableContent: "",
      creditsUsed: 15,
      createdAt: past,
      updatedAt: past,
    })
  );

  // List subcollection
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("tasks").get());

  // Complete / update deliverable
  await assertSucceeds(
    taskRef.update({
      status: "COMPLETED",
      summary: "تم إنشاء السيناريو والجدول الزمني",
      stepTitles: ["بحث الفكرة", "كتابة السيناريو", "توليد المشاهد", "المونتاج الصوتي"],
      currentStepIndex: 4,
      deliverableTitle: "مشروع فيديو الساعة الذكية",
      deliverableContent: "المشهد 1: لقطة افتتاحية...",
      creditsUsed: 25,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject @DocumentId `id` inside payload
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("tasks").doc("task_bad").set({
      id: "task_bad",
      userId: ALICE_UID,
      title: "Bad",
      mode: "CHAT",
      category: "GENERAL",
      status: "COMPLETED",
      prompt: "Hi",
      summary: "Hello",
      stepTitles: [],
      currentStepIndex: 0,
      deliverableTitle: "",
      deliverableContent: "",
      creditsUsed: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject future timestamp
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("tasks").doc("task_future").set({
      userId: ALICE_UID,
      title: "Future",
      mode: "CHAT",
      category: "GENERAL",
      status: "COMPLETED",
      prompt: "Hi",
      summary: "Hello",
      stepTitles: [],
      currentStepIndex: 0,
      deliverableTitle: "",
      deliverableContent: "",
      creditsUsed: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: past,
    })
  );

  // Messages subcollection create & list
  const msgRef = taskRef.collection("messages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      role: "USER",
      content: "أنشئ فيديو إعلاني احترافي",
      mode: "AGENT",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(taskRef.collection("messages").get());

  // Bob cannot list Alice's tasks or messages
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("tasks").doc("task_01").collection("messages").get());
});

test("StudioProject, AutomationRule, and ConnectorState: CRUD and isolation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 4000);

  // StudioProject
  const projRef = alice.collection("users").doc(ALICE_UID).collection("projects").doc("proj_vid_1");
  await assertSucceeds(
    projRef.set({
      userId: ALICE_UID,
      title: "إعلان إطلاق منتج",
      studioType: "VIDEO",
      subType: "Product Ad",
      prompt: "إعلان 30 ثانية",
      content: "السيناريو الكامل...",
      tracksSummary: "Track 1: Clips | Track 2: Voiceover | Track 3: Music",
      status: "READY",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    projRef.update({
      tracksSummary: "Track 1: Trimmed Clips | Track 2: Arabic VO",
      status: "EXPORTED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // AutomationRule
  const autoRef = alice.collection("users").doc(ALICE_UID).collection("automations").doc("auto_1");
  await assertSucceeds(
    autoRef.set({
      userId: ALICE_UID,
      title: "ملخص البريد الصباحي",
      triggerType: "NEW_EMAIL",
      triggerDetail: "عند وصول رسالة جديدة في Gmail",
      actionPrompt: "لخص الرسالة وأرسل التنبيه في Slack",
      active: true,
      runsCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    autoRef.update({
      active: false,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // ConnectorState
  const connRef = alice.collection("users").doc(ALICE_UID).collection("connectors").doc("gmail");
  await assertSucceeds(
    connRef.set({
      userId: ALICE_UID,
      connectorKey: "gmail",
      displayName: "Gmail",
      connected: true,
      accountLabel: "ahmed@gmail.com",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    connRef.update({
      connected: false,
      accountLabel: "",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Cross-user isolation
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("automations").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("connectors").get());
});
