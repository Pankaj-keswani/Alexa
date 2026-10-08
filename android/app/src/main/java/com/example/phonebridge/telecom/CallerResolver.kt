package com.example.phonebridge.telecom

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object CallerResolver {

    /**
     * Resolves caller name for a given phone number using ContactsContract.
     * Does NOT scan or dump the user's contacts. Only queries for the single incoming number.
     * Respects Android permissions.
     */
    fun resolveCallerName(context: Context, rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) {
            return "Unknown caller"
        }

        // Check if READ_CONTACTS is granted
        val hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPermission) {
            return formatNumberForDisplay(rawNumber)
        }

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(rawNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            return name
                        }
                    }
                }
            }
            formatNumberForDisplay(rawNumber)
        } catch (e: Exception) {
            formatNumberForDisplay(rawNumber)
        }
    }

    fun resolvePhoneNumberByName(context: Context, contactName: String): String? {
        val hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPermission) return null

        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$contactName%")

            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex != -1) {
                        return cursor.getString(numberIndex)
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun formatNumberForDisplay(number: String): String {
        return if (number.isNotBlank()) number else "Unknown caller"
    }
}
