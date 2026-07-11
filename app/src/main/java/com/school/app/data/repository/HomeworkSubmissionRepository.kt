package com.school.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.HomeworkSubmission
import com.school.app.domain.model.HomeworkSubmissionGradeRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeworkSubmissionRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) {
    suspend fun submit(homeworkId: String, studentId: String, fileUri: Uri): Outcome<HomeworkSubmission> =
        safeApiCall {
            withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val bytes = resolver.openInputStream(fileUri)?.use { it.readBytes() }
                    ?: throw IOException("Could not read the selected file")
                val fileName = queryFileName(resolver, fileUri) ?: "submission"
                val mediaType = (resolver.getType(fileUri) ?: "application/octet-stream").toMediaType()
                val filePart = MultipartBody.Part.createFormData("file", fileName, bytes.toRequestBody(mediaType))
                val studentIdPart = studentId.toRequestBody("text/plain".toMediaType())
                api.submitHomework(homeworkId, studentIdPart, filePart)
            }
        }

    suspend fun grade(id: String, grade: String, feedback: String?): Outcome<HomeworkSubmission> =
        safeApiCall { api.gradeHomeworkSubmission(id, HomeworkSubmissionGradeRequest(feedback, grade)) }

    suspend fun byHomework(homeworkId: String): Outcome<List<HomeworkSubmission>> =
        safeApiCall { api.submissionsByHomework(homeworkId) }

    suspend fun byStudent(studentId: String): Outcome<List<HomeworkSubmission>> =
        safeApiCall { api.submissionsByStudent(studentId) }

    /** Downloads a submission's file into the app's cache dir, ready to share via FileProvider. */
    suspend fun downloadFile(submission: HomeworkSubmission): Outcome<File> = safeApiCall {
        val body = api.downloadSubmissionFile(submission.id)
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "downloads").apply { mkdirs() }
            val file = File(dir, submission.fileName)
            body.byteStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            file
        }
    }

    private fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) return cursor.getString(nameIndex)
        }
        return null
    }
}
