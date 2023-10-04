/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.samples.socialite.data.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 *  This function return a readable date based on the timestamp of the last message
 *  @return a String (Readable time/date)
 *  */
fun Long.toReadableString(): String {
    val currentDate = Date(this)
    val cal = Calendar.getInstance()
    cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // If timestamp is of today, return hh:mm format
    if (currentDate.after(cal.time)) {
        return SimpleDateFormat("kk:mm", Locale.getDefault()).format(currentDate)
    }

    cal.add(Calendar.DATE, -1)
    return if (currentDate.after(cal.time)) {
        // If timestamp is of yesterday return "Yesterday"
        "Yesterday"
    } else {
        // If timestamp is older return the date, for eg. "Jul 05"
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(currentDate)
    }
}
