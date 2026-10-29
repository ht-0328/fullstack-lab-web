package com.example.fullstacklab.bff.task

import com.example.fullstacklab.apiclient.apis.TasksApi
import com.example.fullstacklab.authzclient.apis.DecisionsApi
import com.example.fullstacklab.bff.auth.CurrentUser
import com.example.fullstacklab.bff.auth.UserType
import com.example.fullstacklab.bff.authz.AuthorizationUnavailableException
import com.example.fullstacklab.bff.authz.AuthzClient
import com.example.fullstacklab.bff.generated.model.CreateTaskInput
import com.example.fullstacklab.bff.generated.model.DueDateInfoStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.never
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * BFF → 認可 API / API の通信と、認可・集約・縮退のロジックを確認する。
 * 呼び出しには publish された生成クライアント（api-client / authz-client）を使い、HTTP 層を MockRestServiceServer で差し替える。
 */
class TaskViewServiceTest {
    private lateinit var api: MockRestServiceServer
    private lateinit var authz: MockRestServiceServer
    private lateinit var service: TaskViewService

    private val alice = CurrentUser("alice-id", "alice", UserType.CUSTOMER, setOf("customer"))
    private val carol = CurrentUser("carol-id", "carol", UserType.STAFF, setOf("task-viewer"))

    @BeforeEach
    fun setUp() {
        val apiBuilder = RestClient.builder().baseUrl("http://api.test")
        val authzBuilder = RestClient.builder().baseUrl("http://authz.test")
        api = MockRestServiceServer.bindTo(apiBuilder).build()
        authz = MockRestServiceServer.bindTo(authzBuilder).build()
        service = TaskViewService(TasksApi(apiBuilder.build()), AuthzClient(DecisionsApi(authzBuilder.build())))
    }

    private fun taskJson(dueDate: String?, owner: String = "alice-id") =
        """{"id":1,"title":"t","description":"d","dueDate":${dueDate?.let { "\"$it\"" }},""" +
            """"createdBy":"$owner","createdByName":"alice","createdAt":"2026-10-28T00:00:00Z"}"""

    private fun expectDecision(action: String, allowed: Boolean) {
        authz.expect(requestTo("http://authz.test/authz/v1/decisions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.action").value(action))
            .andRespond(withSuccess("""{"allowed":$allowed,"reason":"test"}""", MediaType.APPLICATION_JSON))
    }

    @Test
    fun `customer list is limited to own tasks`() {
        authz.expect(requestTo("http://authz.test/authz/v1/decisions"))
            .andExpect(jsonPath("$.resource.ownerId").value("alice-id"))
            .andRespond(withSuccess("""{"allowed":true,"reason":"own"}""", MediaType.APPLICATION_JSON))
        api.expect(requestTo("http://api.test/api/tasks?ownerId=alice-id"))
            .andRespond(withSuccess("[${taskJson(null)}]", MediaType.APPLICATION_JSON))

        assertEquals(1, service.list(alice).size)
        api.verify()
        authz.verify()
    }

    @Test
    fun `staff list is not filtered`() {
        expectDecision("task:list", true)
        api.expect(requestTo("http://api.test/api/tasks"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))

        service.list(carol)
        api.verify()
    }

    @Test
    fun `denied create does not call api`() {
        expectDecision("task:create", false)
        api.expect(never(), requestTo("http://api.test/api/tasks"))

        assertFailsWith<ForbiddenException> { service.create(carol, CreateTaskInput(title = "t")) }
        api.verify()
    }

    @Test
    fun `other's task is hidden as not found`() {
        api.expect(requestTo("http://api.test/api/tasks/1"))
            .andRespond(withSuccess(taskJson(null, owner = "bob-id"), MediaType.APPLICATION_JSON))
        expectDecision("task:read", false)

        assertFailsWith<TaskNotFoundException> { service.detail(alice, 1) }
    }

    @Test
    fun `fails closed when authz is unavailable`() {
        authz.expect(requestTo("http://authz.test/authz/v1/decisions"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertFailsWith<AuthorizationUnavailableException> { service.list(alice) }
    }

    @Test
    fun `aggregates task and due date info`() {
        api.expect(requestTo("http://api.test/api/tasks/1"))
            .andRespond(withSuccess(taskJson("2026-11-03"), MediaType.APPLICATION_JSON))
        expectDecision("task:read", true)
        api.expect(requestTo("http://api.test/api/tasks/1/due-date-info"))
            .andRespond(
                withSuccess(
                    """{"date":"2026-11-03","businessDay":false,"holidayName":"文化の日"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val view = service.detail(alice, 1)

        assertEquals(DueDateInfoStatus.AVAILABLE, view.dueDateInfoStatus)
        assertEquals("文化の日", view.dueDateInfo?.holidayName)
        api.verify()
    }

    @Test
    fun `degrades when due date info fails`() {
        api.expect(requestTo("http://api.test/api/tasks/1"))
            .andRespond(withSuccess(taskJson("2099-12-31"), MediaType.APPLICATION_JSON))
        expectDecision("task:read", true)
        api.expect(requestTo("http://api.test/api/tasks/1/due-date-info"))
            .andRespond(withStatus(HttpStatus.BAD_GATEWAY))

        val view = service.detail(alice, 1)

        assertEquals(DueDateInfoStatus.UNAVAILABLE, view.dueDateInfoStatus)
        assertNull(view.dueDateInfo)
    }

    @Test
    fun `skips due date info when task has no due date`() {
        api.expect(requestTo("http://api.test/api/tasks/1"))
            .andRespond(withSuccess(taskJson(null), MediaType.APPLICATION_JSON))
        expectDecision("task:read", true)
        api.expect(never(), requestTo("http://api.test/api/tasks/1/due-date-info"))

        val view = service.detail(alice, 1)

        assertEquals(DueDateInfoStatus.NO_DUE_DATE, view.dueDateInfoStatus)
        api.verify()
    }
}
