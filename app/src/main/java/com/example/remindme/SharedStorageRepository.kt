package com.example.remindme

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class SharedStorageRepository(private val storage: FirebaseStorage) {

    suspend fun uploadFile(localPath: String, remotePath: String): String? = try {
        val file = Uri.fromFile(File(localPath))
        val storageRef = storage.reference.child(remotePath)
        storageRef.putFile(file).await()
        storageRef.downloadUrl.await().toString()
    } catch (e: Exception) {
        android.util.Log.e("StorageRepo", "Upload failed: ${e.message}")
        null
    }
}
