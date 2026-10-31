package com.example.fullstacklab.bff.task

import com.example.fullstacklab.apiclient.apis.TasksApi
import com.example.fullstacklab.apiclient.models.CreateTaskRequest
import com.example.fullstacklab.apiclient.models.DueDateInfo as ApiDueDateInfo
import com.example.fullstacklab.apiclient.models.Task as ApiTask
import com.example.fullstacklab.bff.auth.CurrentUser
import com.example.fullstacklab.bff.auth.UserType
import com.example.fullstacklab.bff.authz.AuthzClient
import com.example.fullstacklab.bff.generated.model.CreateTaskInput
import com.example.fullstacklab.bff.generated.model.DueDateInfo
import com.example.fullstacklab.bff.generated.model.DueDateInfoStatus
import com.example.fullstacklab.bff.generated.model.TaskDetailView
import com.example.fullstacklab.bff.generated.model.TaskSummaryView
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException

class ForbiddenException(message: String) : RuntimeException(message)
class TaskNotFoundException(id: Long) : RuntimeException("task $id not found")

/**
 * 画面の都合に合わせて API のデータを組み立てる（BFF の中心的な責務）。
 * 入力は api-client の DTO、出力は画面との契約（bff.yaml）から生成した DTO。
 * 各操作の前に認可 API へ問い合わせ、許可されたときだけ API を呼ぶ。
 * 補足情報は「あれば表示する」扱いなので、取得に失敗しても詳細画面全体はエラーにしない。
 */
@Service
class TaskViewService(
    private val tasksApi: TasksApi,
    private val authzClient: AuthzClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 画面の出し分け用（登録ボタン）。実際の守りは create() の認可 */
    fun canCreateTask(user: CurrentUser): Boolean = authzClient.decide(user, "task:create", user.id).allowed

    fun list(user: CurrentUser): List<TaskSummaryView> {
        // 一般利用者は自分のタスクだけ、社員は全件を対象にし、その範囲で一覧してよいかを判定する
        val ownerFilter = if (user.userType == UserType.CUSTOMER) user.id else null
        authorize(user, "task:list", ownerFilter)
        return tasksApi.listTasks(ownerFilter).map { it.toSummary() }
    }

    fun detail(user: CurrentUser, id: Long): TaskDetailView {
        val task = findTask(id)
        // 他人のタスクは「存在しない」と同じ応答にし、ID の存在自体を推測させない
        val decision = authzClient.decide(user, "task:read", task.createdBy)
        if (!decision.allowed) {
            log.info("task {} hidden from {}: {}", id, user.id, decision.reason)
            throw TaskNotFoundException(id)
        }
        return toDetailView(user, task)
    }

    /**
     * タスクを完了にする。所有者を知るために先に API から取得し、認可 API が許可したときだけ完了を呼ぶ。
     * 既に完了していれば API が何もせず現状を返す（冪等）ので、BFF では区別しない
     */
    fun complete(user: CurrentUser, id: Long): TaskDetailView {
        val task = findTask(id)
        authorize(user, "task:complete", task.createdBy)
        return toDetailView(user, tasksApi.completeTask(id))
    }

    fun create(user: CurrentUser, input: CreateTaskInput): TaskSummaryView {
        authorize(user, "task:create", user.id)
        val request = CreateTaskRequest(title = input.title, description = input.description, dueDate = input.dueDate)
        return tasksApi.createTask(xUserId = user.id, xUserName = user.name, createTaskRequest = request).toSummary()
    }

    private fun authorize(user: CurrentUser, action: String, ownerId: String?) {
        val decision = authzClient.decide(user, action, ownerId)
        if (!decision.allowed) throw ForbiddenException("$action denied: ${decision.reason}")
    }

    private fun findTask(id: Long): ApiTask = try {
        tasksApi.getTask(id)
    } catch (e: HttpClientErrorException.NotFound) {
        throw TaskNotFoundException(id)
    }

    /** 詳細画面向けに、期限日の補足情報（縮退あり）と、完了ボタンの出し分け（canComplete）を組み立てる */
    private fun toDetailView(user: CurrentUser, task: ApiTask): TaskDetailView {
        val (status, info) = when {
            task.dueDate == null -> DueDateInfoStatus.NO_DUE_DATE to null
            else -> try {
                DueDateInfoStatus.AVAILABLE to tasksApi.getTaskDueDateInfo(task.id).toView()
            } catch (e: RestClientException) {
                log.warn("due date info unavailable for task {}: {}", task.id, e.message)
                DueDateInfoStatus.UNAVAILABLE to null
            }
        }
        // 画面の出し分け用。実際の守りは complete() の認可。完了済みなら認可 API に聞かずに false
        val canComplete = task.completedAt == null && authzClient.decide(user, "task:complete", task.createdBy).allowed
        return TaskDetailView(
            id = task.id,
            title = task.title,
            description = task.description,
            dueDate = task.dueDate,
            createdByName = task.createdByName,
            createdAt = task.createdAt,
            completedAt = task.completedAt,
            canComplete = canComplete,
            dueDateInfoStatus = status,
            dueDateInfo = info,
        )
    }

    private fun ApiTask.toSummary() =
        TaskSummaryView(id = id, title = title, dueDate = dueDate, createdByName = createdByName, completedAt = completedAt)

    private fun ApiDueDateInfo.toView() = DueDateInfo(date = date, businessDay = businessDay, holidayName = holidayName)
}
