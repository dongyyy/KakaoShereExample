package com.example.kakaoshereexample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kakaoshereexample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //"앱으로 보기"시 파라미터 전달 로직
        if (intent.action == Intent.ACTION_VIEW) {
            val key1 = intent.data?.getQueryParameter("key1")
            Toast.makeText(this, key1, Toast.LENGTH_SHORT).show()
        }

        binding.startDialogTextView.setOnClickListener {
            val dialog = ReturnShareBottomSheetDialog(this)
//            dialog.initView { }
            dialog.initView()
            dialog.showDialog()
        }
    }
}
