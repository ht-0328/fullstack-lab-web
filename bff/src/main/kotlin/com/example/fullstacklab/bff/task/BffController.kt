package com.example.fullstacklab.bff.task

import com.example.fullstacklab.bff.account.AccountService
import com.example.fullstacklab.bff.auth.CurrentUserFactory
import com.example.fullstacklab.bff.generated.api.MeApi
import com.example.fullstacklab.bff.generated.api.TasksApi
import com.example.fullstacklab.bff.generated.model.Capabilities
import com.example.fullstacklab.bff.generated.model.ChangePasswordInput
import com.example.fullstacklab.bff.generated.model.CreateTaskInput
import com.example.fullstacklab.bff.generated.model.MeView
import com.example.fullstacklab.bff.generated.model.TaskDetailView
import com.example.fullstacklab.bff.generated.model.TaskSummaryView
import com.example.fullstacklab.bff.generated.model.UserType
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * 画面との契約（openapi/bff.yaml）から生成した MeApi / TasksApi を実装する。
 * ブラウザ（Nginx/ALB 経由）と Nuxt の SSR（コンテナ間で直接）の両方から呼ばれる。
 * 認証（JWT 検証）は SecurityConfig、認可は各サービスが認可 API に問い合わせて行う。
 * 会員登録（未認証）は SignupController。
 */
@RestController
class BffController(
    private val taskViewService: TaskViewService,
    private val accountService: AccountService,
    private val currentUserFactory: CurrentUserFactory,
) : MeApi, TasksApi {

    override fun getMe(): ResponseEntity<MeView> {
        val user = currentUserFactory.current()
        return ResponseEntity.ok(
            MeView(
                userId = user.id,
                name = user.name,
                userType = UserType.valueOf(user.userType.name),
                roles = user.roles.toList(),
                capabilities = Capabilities(
                    canCreateTask = taskViewService.canCreateTask(user),
                    canChangePassword = accountService.canChangePassword(user),
                ),
            ),
        )
    }

    override fun changePassword(changePasswordInput: ChangePasswordInput): ResponseEntity<Unit> {
        accountService.changePassword(currentUserFactory.current(), changePasswordInput)
        return ResponseEntity.noContent().build()
    }

    override fun listTasks(): ResponseEntity<List<TaskSummaryView>> =
        ResponseEntity.ok(taskViewService.list(currentUserFactory.current()))

    override fun getTaskDetail(id: Long): ResponseEntity<TaskDetailView> =
        ResponseEntity.ok(taskViewService.detail(currentUserFactory.current(), id))

    override fun createTask(createTaskInput: CreateTaskInput): ResponseEntity<TaskSummaryView> =
        ResponseEntity.status(HttpStatus.CREATED).body(taskViewService.create(currentUserFactory.current(), createTaskInput))
}
