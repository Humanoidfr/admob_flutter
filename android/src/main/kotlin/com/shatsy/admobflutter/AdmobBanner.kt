package com.shatsy.admobflutter

import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdView
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.platform.PlatformView


class AdmobBanner(context: Context, messenger: BinaryMessenger, id: Int, args: HashMap<*, *>) : PlatformView, MethodCallHandler {
    private val channel: MethodChannel = MethodChannel(messenger, "admob_flutter/banner_$id")

    // Only one view with be non-null
    private var adView: AdView? = null
    private var publisherAdView: PublisherAdView? = null

    init {
        channel.setMethodCallHandler(this)

        if (args.containsKey("customTargeting")) {
            publisherAdView = PublisherAdView(context).apply {
                setAdSizes(getSize(context, args["adSize"] as HashMap<*, *>))
                adUnitId = args["adUnitId"] as String?
            }

            val adRequestBuilder = PublisherAdRequest.Builder()

            val npa: Boolean? = args["nonPersonalizedAds"] as Boolean?
            if (npa == true) {
                val extras = Bundle()
                extras.putString("npa", "1")
                adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            }

            for ((k, v) in args["customTargeting"] as HashMap<*, *>) {
                if (k is String && v is String) {
                    adRequestBuilder.addCustomTargeting(k, v)
                }
            }

            publisherAdView!!.loadAd(adRequestBuilder.build())
        } else {
            adView = AdView(context).apply {
                adSize = getSize(context, args["adSize"] as HashMap<*, *>)
                adUnitId = args["adUnitId"] as String?
            }

            val adRequestBuilder = AdRequest.Builder()

            val npa: Boolean? = args["nonPersonalizedAds"] as Boolean?
            if (npa == true) {
                val extras = Bundle()
                extras.putString("npa", "1")
                adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            }

            adView!!.loadAd(adRequestBuilder.build())
        }
    }

    private fun getSize(context: Context, size: HashMap<*, *>): AdSize {
        val width = size["width"] as Int
        val height = size["height"] as Int
        val name = size["name"] as String

        return when (name) {
            "BANNER" -> AdSize.BANNER
            "LARGE_BANNER" -> AdSize.LARGE_BANNER
            "MEDIUM_RECTANGLE" -> AdSize.MEDIUM_RECTANGLE
            "FULL_BANNER" -> AdSize.FULL_BANNER
            "LEADERBOARD" -> AdSize.LEADERBOARD
            "SMART_BANNER" -> AdSize.SMART_BANNER
            "ADAPTIVE_BANNER" -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width)
            else -> AdSize(width, height)
        }
    }

    override fun getView(): View {
        return publisherAdView ?: adView!!
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "setListener" -> {
                publisherAdView?.adListener = createAdListener(channel)
                adView?.adListener = createAdListener(channel)
            }
            "dispose" -> dispose()
            else -> result.notImplemented()
        }
    }

    override fun dispose() {
        publisherAdView?.run {
            visibility = View.GONE
            destroy()
        }

        adView?.run {
            visibility = View.GONE
            destroy()
        }

        channel.setMethodCallHandler(null)
    }
}