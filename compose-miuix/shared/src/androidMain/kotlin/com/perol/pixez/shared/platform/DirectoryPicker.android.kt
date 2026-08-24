package com.perol.pixez.shared.platform

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberDirectoryPicker(onResult: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}

            val resolvedPath = resolveTreeUriPath(uri)
            onResult(resolvedPath)
        } else {
            onResult(null)
        }
    }
    return {
        launcher.launch(null)
    }
}

private fun resolveTreeUriPath(uri: Uri): String {
    val docId = try {
        DocumentsContract.getTreeDocumentId(uri)
    } catch (_: Exception) {
        null
    }
    if (docId != null) {
        if (docId.startsWith("primary:")) {
            val sub = docId.removePrefix("primary:")
            return "${Environment.getExternalStorageDirectory().absolutePath}/$sub"
        } else if (docId.startsWith("raw:")) {
            return docId.removePrefix("raw:")
        }
    }
    val path = uri.path
    if (path != null) {
        if (path.contains("primary:")) {
            val sub = path.substringAfter("primary:")
            return "${Environment.getExternalStorageDirectory().absolutePath}/$sub"
        } else if (path.contains("raw:")) {
            return path.substringAfter("raw:")
        }
    }
    return uri.toString()
}
