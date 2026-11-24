package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AgentTask
import com.example.model.AutomationRule
import com.example.model.ConnectorCatalogItem
import com.example.model.ConnectorState
import com.example.model.StudioProject
import com.example.model.TaskMessage
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AlSaherRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth? = try {
        Firebase.auth
    } catch (_: Throwable) {
        null
    }
) {
    constructor(context: Context) : this(
        db = resolveRepositoryFirestore(context)
    )

    private fun requireUserId(): String {
        val uid = auth?.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً.")
        }
        return uid
    }

    // --- USER PROFILE & CREDITS ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(userId: String, displayName: String, email: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "عدم تطابق معرّف المستخدم" }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "مستخدم الساحر" }.take(100)
                val safeEmail = email.trim().take(120)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "plan" to "FREE",
                    "creditsBalance" to 1000L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deductCredits(userId: String, currentBalance: Long, amount: Long): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val nextBalance = (currentBalance - amount).coerceAtLeast(0L)
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "creditsBalance" to nextBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upgradePlan(userId: String, plan: String, newBalance: Long): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(plan in listOf("FREE", "PRO", "TEAM"))
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "plan" to plan,
                        "creditsBalance" to newBalance.coerceIn(0L, 1_000_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfileInfo(userId: String, displayName: String, email: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeName = displayName.trim().take(100)
            require(safeName.isNotEmpty()) { "الاسم المعروض لا يمكن أن يكون فارغاً." }
            val safeEmail = email.trim().take(120)
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- TASKS & MESSAGES ---
    fun observeTasks(userId: String): Flow<List<AgentTask>> = flow {
        val path = "users/$userId/tasks"
        emitAll(
            db.collection("users").document(userId).collection("tasks")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AgentTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTaskMessages(userId: String, taskId: String): Flow<List<TaskMessage>> = flow {
        val path = "users/$userId/tasks/$taskId/messages"
        emitAll(
            db.collection("users").document(userId).collection("tasks")
                .document(taskId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TaskMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getTaskMessagesOnce(userId: String, taskId: String): Result<List<TaskMessage>> {
        val path = "users/$userId/tasks/$taskId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val snapshot = db.collection("users").document(uid).collection("tasks")
                .document(taskId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
            val messages = snapshot.toObjects(
                TaskMessage::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(messages)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun createTask(
        userId: String,
        title: String,
        mode: String,
        category: String,
        status: String,
        prompt: String,
        stepTitles: List<String>,
        creditsUsed: Long
    ): Result<String> {
        var docPath = "users/$userId/tasks"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("tasks").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to title.trim().ifBlank { "مهمة جديدة" }.take(140),
                "mode" to mode,
                "category" to category,
                "status" to status,
                "prompt" to prompt.trim().ifBlank { title }.take(4000),
                "summary" to "",
                "stepTitles" to stepTitles.take(15),
                "currentStepIndex" to 0L,
                "deliverableTitle" to "",
                "deliverableContent" to "",
                "creditsUsed" to creditsUsed.coerceIn(0L, 10000L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTaskProgress(
        userId: String,
        taskId: String,
        status: String,
        currentStepIndex: Long
    ): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(taskId.isNotBlank())
            db.collection("users").document(uid).collection("tasks").document(taskId)
                .update(
                    mapOf(
                        "status" to status,
                        "currentStepIndex" to currentStepIndex.coerceIn(0L, 20L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTaskEdit(
        userId: String,
        taskId: String,
        title: String,
        prompt: String,
        mode: String,
        status: String,
        stepTitles: List<String>,
        currentStepIndex: Long
    ): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(taskId.isNotBlank())
            db.collection("users").document(uid).collection("tasks").document(taskId)
                .update(
                    mapOf(
                        "title" to title.trim().ifBlank { "مهمة" }.take(140),
                        "prompt" to prompt.trim().ifBlank { title }.take(4000),
                        "mode" to mode,
                        "status" to status,
                        "stepTitles" to stepTitles.take(15),
                        "currentStepIndex" to currentStepIndex.coerceIn(0L, 20L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun completeTask(
        userId: String,
        taskId: String,
        status: String,
        summary: String,
        stepTitles: List<String>,
        currentStepIndex: Long,
        deliverableTitle: String,
        deliverableContent: String,
        creditsUsed: Long
    ): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(taskId.isNotBlank())
            db.collection("users").document(uid).collection("tasks").document(taskId)
                .update(
                    mapOf(
                        "status" to status,
                        "summary" to summary.take(12000),
                        "stepTitles" to stepTitles.take(15),
                        "currentStepIndex" to currentStepIndex.coerceIn(0L, 20L),
                        "deliverableTitle" to deliverableTitle.take(200),
                        "deliverableContent" to deliverableContent.take(24000),
                        "creditsUsed" to creditsUsed.coerceIn(0L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("tasks").document(taskId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addMessage(
        userId: String,
        taskId: String,
        role: String,
        content: String,
        mode: String
    ): Result<String> {
        var docPath = "users/$userId/tasks/$taskId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("tasks")
                .document(taskId).collection("messages").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "role" to role,
                    "content" to content.trim().ifBlank { "..." }.take(16000),
                    "mode" to mode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- STUDIO PROJECTS ---
    fun observeProjects(userId: String): Flow<List<StudioProject>> = flow {
        val path = "users/$userId/projects"
        emitAll(
            db.collection("users").document(userId).collection("projects")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudioProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveProject(
        userId: String,
        title: String,
        studioType: String,
        subType: String,
        prompt: String,
        content: String,
        tracksSummary: String,
        status: String = "READY"
    ): Result<String> {
        var docPath = "users/$userId/projects"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("projects").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.trim().ifBlank { "مشروع جديد" }.take(140),
                    "studioType" to studioType,
                    "subType" to subType.take(80),
                    "prompt" to prompt.take(4000),
                    "content" to content.take(24000),
                    "tracksSummary" to tracksSummary.take(8000),
                    "status" to status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProject(
        userId: String,
        projectId: String,
        title: String,
        subType: String,
        prompt: String,
        content: String,
        tracksSummary: String,
        status: String
    ): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("projects").document(projectId)
                .update(
                    mapOf(
                        "title" to title.trim().ifBlank { "مشروع" }.take(140),
                        "subType" to subType.take(80),
                        "prompt" to prompt.take(4000),
                        "content" to content.take(24000),
                        "tracksSummary" to tracksSummary.take(8000),
                        "status" to status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- AUTOMATIONS ---
    fun observeAutomations(userId: String): Flow<List<AutomationRule>> = flow {
        val path = "users/$userId/automations"
        emitAll(
            db.collection("users").document(userId).collection("automations")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AutomationRule::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createAutomation(
        userId: String,
        title: String,
        triggerType: String,
        triggerDetail: String,
        actionPrompt: String
    ): Result<String> {
        var docPath = "users/$userId/automations"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("automations").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.trim().ifBlank { "أتمتة جديدة" }.take(120),
                    "triggerType" to triggerType,
                    "triggerDetail" to triggerDetail.trim().ifBlank { "تشغيل تلقائي" }.take(200),
                    "actionPrompt" to actionPrompt.trim().ifBlank { "تنفيذ المهمة" }.take(2000),
                    "active" to true,
                    "runsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleAutomation(userId: String, automationId: String, active: Boolean): Result<Unit> {
        val docPath = "users/$userId/automations/$automationId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("automations").document(automationId)
                .update(
                    mapOf(
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun incrementAutomationRun(userId: String, automationId: String, nextCount: Long): Result<Unit> {
        val docPath = "users/$userId/automations/$automationId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("automations").document(automationId)
                .update(
                    mapOf(
                        "runsCount" to nextCount.coerceIn(0L, 100000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAutomation(userId: String, automationId: String): Result<Unit> {
        val docPath = "users/$userId/automations/$automationId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("automations").document(automationId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- CONNECTORS ---
    fun observeConnectors(userId: String): Flow<List<ConnectorState>> = flow {
        val path = "users/$userId/connectors"
        emitAll(
            db.collection("users").document(userId).collection("connectors")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ConnectorState::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun setConnectorState(
        userId: String,
        item: ConnectorCatalogItem,
        connected: Boolean,
        accountLabel: String
    ): Result<Unit> {
        val docPath = "users/$userId/connectors/${item.key}"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("connectors").document(item.key)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "connected" to connected,
                        "accountLabel" to accountLabel.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "connectorKey" to item.key,
                        "displayName" to item.name,
                        "connected" to connected,
                        "accountLabel" to accountLabel.take(120),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        private fun resolveRepositoryFirestore(context: Context): FirebaseFirestore {
            return try {
                val dbId = context.applicationContext.getString(R.string.firestore_database_id).trim()
                if (dbId.isNotEmpty()) {
                    FirebaseFirestore.getInstance(dbId)
                } else {
                    FirebaseFirestore.getInstance()
                }
            } catch (_: Throwable) {
                FirebaseFirestore.getInstance()
            }
        }
    }
}
