package com.example.skaner_kodow.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Witaj w aplikacji Skaner Kodu"
    }

    val text: LiveData<String> = _text

    fun updateText(newText: String) {
        _text.value = newText
    }
}
