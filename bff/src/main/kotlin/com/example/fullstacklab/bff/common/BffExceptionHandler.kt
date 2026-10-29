package com.example.fullstacklab.bff.common

import com.example.fullstacklab.bff.authz.AuthorizationUnavailableException
import com.example.fullstacklab.bff.task.ForbiddenException
import com.example.fullstacklab.bff.task.TaskNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException

@RestControllerAdvice
class BffExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(e: ForbiddenException): ProblemDetail {
        log.info("forbidden: {}", e.message)
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "この操作は許可されていません")
    }

    @ExceptionHandler(TaskNotFoundException::class)
    fun handleTaskNotFound(e: TaskNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "not found")

    // 認可を判定できないときは操作させない（fail-closed）
    @ExceptionHandler(AuthorizationUnavailableException::class)
    fun handleAuthzUnavailable(e: AuthorizationUnavailableException): ProblemDetail {
        log.error("authorization service unavailable: {}", e.cause?.message)
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "authorization service unavailable")
    }

    // API の 4xx（入力エラーなど）は、内容を画面側でも使えるようそのまま返す
    @ExceptionHandler(HttpClientErrorException::class)
    fun handleClientError(e: HttpClientErrorException): ResponseEntity<String> =
        ResponseEntity.status(e.statusCode)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(e.responseBodyAsString)

    // API の 5xx や接続失敗は内部情報を出さず 502 にまとめる
    @ExceptionHandler(RestClientException::class)
    fun handleUpstreamError(e: RestClientException): ProblemDetail {
        log.error("api call failed: {}", e.message)
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "upstream api error")
    }
}
