package com.example.reminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    val context = getApplication<Application>().applicationContext
}
