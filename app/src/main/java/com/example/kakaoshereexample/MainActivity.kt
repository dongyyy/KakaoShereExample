package com.example.kakaoshereexample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kakaoshereexample.databinding.ActivityMainBinding
import com.kakao.sdk.common.util.KakaoCustomTabsClient
import com.kakao.sdk.link.LinkClient
import com.kakao.sdk.link.WebSharerClient
import com.kakao.sdk.link.rx
import com.kakao.sdk.template.model.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val TAG = MainActivity::class.simpleName
    private val url = "https://developers.kakao.com"
    private var disposables = CompositeDisposable()
    var imgUrl:String = ""

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

        // 로컬 이미지 파일
        // 이 샘플에서는 프로젝트 리소스로 추가한 이미지 파일을 사용했습니다. 갤러리 등 서비스 니즈에 맞는 사진 파일을 준비하세요.
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.kakaolink40_original)
        val file = File(this.cacheDir, "sample1.png")

        this.runOnUiThread {
            try {
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                // 카카오 이미지 서버로 업로드
                LinkClient.rx.uploadImage(file)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ imageUploadResult ->
                        Log.i(TAG, "이미지 업로드 성공 \n${imageUploadResult.infos.original}")
                        imgUrl = imageUploadResult.infos.original.url
                    }, { error ->
                        Log.e(TAG, "이미지 업로드 실패", error)
                    }).addTo(disposables)

            } catch (e: Exception) {

            }
        }

        binding.appShareTextView.setOnClickListener {
            val defaultFeed = getFeedTemplate()
            // 카카오톡 설치여부 확인
            if (LinkClient.instance.isKakaoLinkAvailable(this)) {

                // 피드 메시지 보내기
                LinkClient.rx.defaultTemplate(this, defaultFeed)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ linkResult ->
                        Log.d(TAG, "카카오링크 보내기 성공 ${linkResult.intent}")
                        startActivity(linkResult.intent)

                        // 카카오링크 보내기에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                        Log.w(TAG, "Warning Msg: ${linkResult.warningMsg}")
                        Log.w(TAG, "Argument Msg: ${linkResult.argumentMsg}")
                    }, { error ->
                        Log.e(TAG, "카카오링크 보내기 실패 ", error)
                    })
                    .addTo(disposables)

            } else {
                // 카카오톡 미설치: 웹 공유 사용 권장
                // 웹 공유 예시 코드
                val sharerUrl = WebSharerClient.instance.defaultTemplateUri(defaultFeed)

                // CustomTabs으로 웹 브라우저 열기

                // 1. CustomTabs으로 Chrome 브라우저 열기
                try {
                    KakaoCustomTabsClient.openWithDefault(this, sharerUrl)
                } catch (e: UnsupportedOperationException) {
                    // Chrome 브라우저가 없을 때 예외처리

                    // 2. CustomTabs으로 디바이스 기본 브라우저 열기
                    try {
                        KakaoCustomTabsClient.open(this, sharerUrl)
                    } catch (e: ActivityNotFoundException) {
                        // 인터넷 브라우저가 없을 때 예외처리
                    }
                }
            }
        }

        binding.webShareTextView.setOnClickListener {
            val defaultFeed = getFeedTemplate()
            // 카카오톡 미설치: 웹 공유 사용 권장
            // 웹 공유 예시 코드
            val sharerUrl = WebSharerClient.instance.defaultTemplateUri(defaultFeed)

            // CustomTabs으로 웹 브라우저 열기

            // 1. CustomTabs으로 Chrome 브라우저 열기
            try {
                KakaoCustomTabsClient.openWithDefault(this, sharerUrl)
            } catch (e: UnsupportedOperationException) {
                // Chrome 브라우저가 없을 때 예외처리

                // 2. CustomTabs으로 디바이스 기본 브라우저 열기
                try {
                    KakaoCustomTabsClient.open(this, sharerUrl)
                } catch (e: ActivityNotFoundException) {
                    // 인터넷 브라우저가 없을 때 예외처리
                }
            }
        }
    }

    private fun getFeedTemplate(): FeedTemplate {
        return FeedTemplate(
            content = Content(
                title = "딸기 치즈 케익",
                description = "#케익 #딸기 #삼평동 #카페 #분위기 #소개팅",
                imageUrl = imgUrl,
                link = Link(
                    webUrl = url, //PC 버전 카카오톡에서 사용하는 웹 링크 URL.
                    mobileWebUrl = url, //모바일 카카오톡에서 사용하는 웹 링크 URL
                )
            ),
            social = Social(
                likeCount = 286,
                commentCount = 45,
                sharedCount = 845
            ),
            buttons = listOf(
                Button(
                    "웹으로 보기",
                    Link(
                        webUrl = url,
                        mobileWebUrl = url
                    )
                ),
                Button(
                    "앱으로 보기", //https://developers.kakao.com/console/app/622442/config/platform 에 마켓 URL 설정
                    Link(
                        androidExecutionParams = mapOf(
                            "key1" to "\"앱으로 보기\"시 파라미터 전달",
                            "key2" to "value2"
                        ), //앱
                        iosExecutionParams = mapOf("key1" to "value1", "key2" to "value2")
                    )
                )
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }
}
