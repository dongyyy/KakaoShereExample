package com.example.kakaoshereexample

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.example.kakaoshereexample.databinding.LayoutReturnShareDialogBinding
import com.kakao.sdk.common.util.KakaoCustomTabsClient
import com.kakao.sdk.link.LinkClient
import com.kakao.sdk.link.WebSharerClient
import com.kakao.sdk.link.model.LinkResult
import com.kakao.sdk.link.rx
import com.kakao.sdk.template.model.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.FileOutputStream

class ReturnShareBottomSheetDialog(context: Context) : BottomSheetDialogView(context) {
    private lateinit var binding: LayoutReturnShareDialogBinding

    private val TAG = ReturnShareBottomSheetDialog::class.simpleName
    private val moveUrl = "https://developers.kakao.com"
    private var disposables = CompositeDisposable()
    private val feedTemplateSubject = PublishSubject.create<FeedTemplate>()

    override fun inflateView(inflater: LayoutInflater): View {
        binding = LayoutReturnShareDialogBinding.inflate(inflater)
        val view = binding.root
        return view
    }

    //    fun initView(onReturnShareListener: () -> Unit) {
    fun initView() {


        binding.returnShareToKakaoAppButton.setOnClickListener {
//            onReturnShareListener()

            val imgUrl = uploadImage()

            val feedTemplate = imgUrl.map { imgUrl -> getFeedTemplate(imgUrl) }

            feedTemplate.subscribe { feedTemplate ->
                feedTemplateSubject.onNext(feedTemplate)
            }

            val appLinkResult = feedTemplateSubject
                .share()
                .filter { LinkClient.instance.isKakaoLinkAvailable(context) }
                .flatMapSingle { feedTemplate -> startKakaoLinkToApp(feedTemplate) }

            val webLinkResult = feedTemplateSubject
                .share()
                .filter { !LinkClient.instance.isKakaoLinkAvailable(context) }

            appLinkResult
                .subscribe { linkResult ->
                    context.startActivity(linkResult.intent)
                    dismiss()
                }.addTo(disposables)

            webLinkResult
                .subscribe { feedTemplate ->
                    startKakaoLinkWeb(feedTemplate)
                    dismiss()
                }.addTo(disposables)

        }

        //정식 로직은 아님(테스트용)
        binding.returnShareToKakaoWebButton.setOnClickListener {

            val imgUrl = uploadImage()

            val feedTemplate = imgUrl.map { imgUrl -> getFeedTemplate(imgUrl) }

            feedTemplate.subscribe { feedTemplate ->
                feedTemplateSubject.onNext(feedTemplate)
            }

            feedTemplateSubject
                .subscribe { feedTemplate ->
                    startKakaoLinkWeb(feedTemplate)
                    dismiss()
                }.addTo(disposables)
        }

    }

    private fun startKakaoLinkToApp(feedTemplate: FeedTemplate): Single<LinkResult> {
        // 피드 메시지 보내기
        return LinkClient.rx.defaultTemplate(context, feedTemplate)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { linkResult ->
                Log.d(TAG, "카카오링크 보내기 성공 ${linkResult.intent}")

                // 카카오링크 보내기에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                Log.w(TAG, "Warning Msg: ${linkResult.warningMsg}")
                Log.w(TAG, "Argument Msg: ${linkResult.argumentMsg}")
            }
            .doOnError { error ->
                Log.e(TAG, "카카오링크 보내기 실패 ", error)
            }
    }

    private fun startKakaoLinkWeb(feedTemplate: FeedTemplate) {
        //카카오톡 미설치: 웹 공유 사용 권장
        // 웹 공유 예시 코드
        val sharerUrl = WebSharerClient.instance.defaultTemplateUri(feedTemplate)

        // CustomTabs으로 웹 브라우저 열기

        // 1. CustomTabs으로 Chrome 브라우저 열기
        try {
            KakaoCustomTabsClient.openWithDefault(context, sharerUrl)
        } catch (e: UnsupportedOperationException) {
            // Chrome 브라우저가 없을 때 예외처리

            // 2. CustomTabs으로 디바이스 기본 브라우저 열기
            try {
                KakaoCustomTabsClient.open(context, sharerUrl)
            } catch (e: ActivityNotFoundException) {
                // 인터넷 브라우저가 없을 때 예외처리
            }
        }
    }

    private fun getBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        view.draw(c)
        return bitmap
    }

    private fun uploadImage(): Single<String> {
        // 로컬 이미지 파일
        // 이 샘플에서는 프로젝트 리소스로 추가한 이미지 파일을 사용했습니다. 갤러리 등 서비스 니즈에 맞는 사진 파일을 준비하세요.
//        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.kakaolink40_original)
        val bitmap = getBitmap(binding.shareLayout)
        val file = File(context.cacheDir, "sample1.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        // 카카오 이미지 서버로 업로드
        return LinkClient.rx.uploadImage(file)
            .doOnSuccess { imageUrl -> Log.i(TAG, "이미지 업로드 성공 \n${imageUrl}") }
            .doOnError { error -> Log.e(TAG, "이미지 업로드 실패", error) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { imageUploadResult -> Single.just(imageUploadResult.infos.original.url) }
    }

    private fun getFeedTemplate(imgUrl: String): FeedTemplate {
        return FeedTemplate(
            content = Content(
                title = "미니스탁 공유하기",
                description = "친구에게 수익률을 공유해보세요~!",
                imageUrl = imgUrl,
                link = Link(
                    webUrl = moveUrl, //PC 버전 카카오톡에서 사용하는 웹 링크 URL.
                    mobileWebUrl = moveUrl, //모바일 카카오톡에서 사용하는 웹 링크 URL
                ),
                imageWidth = binding.shareLayout.width,
                imageHeight= binding.shareLayout.height
            ),
            social = Social(
                likeCount = 286,
                commentCount = 45,
                sharedCount = 845
            ),
            buttons = listOf(
                Button(
                    "웹으로 이동",
                    Link(
                        webUrl = moveUrl,
                        mobileWebUrl = moveUrl
                    )
                ),
                Button(
                    "미니스탁 다운로드", //https://developers.kakao.com/console/app/622442/config/platform 에 마켓 URL 설정
                    Link(
                        androidExecutionParams = mapOf(
                            "key1" to "\"미니스탁 다운로드\"시 파라미터 전달",
                            "key2" to "value2"
                        ), //앱
                        iosExecutionParams = mapOf("key1" to "value1", "key2" to "value2")
                    )
                )
            )
        )
    }

    override fun dismiss() {
        super.dismiss()
        disposables.dispose()
    }

}