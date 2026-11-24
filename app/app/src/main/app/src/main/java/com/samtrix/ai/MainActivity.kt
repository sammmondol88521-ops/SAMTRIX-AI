package com.samtrix.ai

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val gold = Color.rgb(220, 177, 83)
    private val bg = Color.rgb(10, 10, 12)
    private val panel = Color.rgb(27, 27, 31)
    private val worker = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var apiKey: EditText
    private lateinit var symbol: EditText
    private lateinit var signal: TextView
    private lateinit var status: TextView
    private lateinit var details: TextView
    private lateinit var timer: TextView

    private var seconds = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        createScreen()
        startTimer()
    }

    private fun createScreen() {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 28)
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "SAMTRIX AI"
            textSize = 30f
            setTextColor(gold)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "PREMIUM AI CHART ANALYZER"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 25)
        })

        val timerBox = card(root)
        timer = text("NEXT ANALYSIS IN 01:00", 16, gold)
        timer.gravity = Gravity.CENTER
        timerBox.addView(timer)

        val signalBox = card(root)
        signalBox.addView(text("CURRENT SIGNAL", 13, Color.LTGRAY).apply {
            gravity = Gravity.CENTER
        })
        signal = text("WAIT", 38, Color.YELLOW).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 12, 0, 12)
        }
        signalBox.addView(signal)

        root.addView(text("API KEY", 14, gold).apply {
            setPadding(2, 18, 0, 6)
        })

        apiKey = EditText(this).apply {
            hint = "Enter Twelve Data API Key"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = 129
            setSingleLine(true)
            setPadding(14, 10, 14, 10)
            setBackgroundColor(panel)
        }
        root.addView(apiKey, params())

        root.addView(text("MARKET SYMBOL", 14, gold).apply {
            setPadding(2, 16, 0, 6)
        })

        symbol = EditText(this).apply {
            setText("EUR/USD")
            setTextColor(Color.WHITE)
            setSingleLine(true)
            setPadding(14, 10, 14, 10)
            setBackgroundColor(panel)
        }
        root.addView(symbol, params())

        root.addView(Button(this).apply {
            text = "ANALYZE NOW"
            setTextColor(Color.BLACK)
            setBackgroundColor(gold)
            setOnClickListener { analyze() }
        }, LinearLayout.LayoutParams(-1, 54).apply {
            topMargin = 20
        })

        status = text("Ready. Enter your API key.", 13, Color.LTGRAY)
        status.setPadding(2, 15, 2, 12)
        root.addView(status)

        details = text(
            "EMA 9: --\nEMA 21: --\nRSI 14: --\nMACD: --\n\nAnalysis only. No trades are placed.",
            14, Color.WHITE
        )
        details.setPadding(15, 18, 15, 18)
        details.setBackgroundColor(panel)
        root.addView(details, params())
    }

    private fun text(value: String, size: Int, color: Int) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
        }

    private fun card(root: LinearLayout): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(panel)
        }
        root.addView(box, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = 15
        })
        return box
    }

    private fun params() = LinearLayout.LayoutParams(-1, -2)

    private fun startTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                seconds--
                if (seconds <= 0) {
                    seconds = 60
                    if (::apiKey.isInitialized &&
                        apiKey.text.toString().isNotBlank()) {
                        analyze()
                    }
                }
                if (::timer.isInitialized) {
                    timer.text = "NEXT ANALYSIS IN %02d:%02d".format(
                        Locale.US, seconds / 60, seconds % 60
                    )
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun analyze() {
        val key = apiKey.text.toString().trim()
        val pair = symbol.text.toString().trim()

        if (key.isBlank()) {
            status.text = "Please enter your API key."
            return
        }

        status.text = "Fetching market data..."

        worker.execute {
            try {
                val url = URL(
                    "https://api.twelvedata.com/time_series" +
                    "?symbol=${URLEncoder.encode(pair, "UTF-8")}" +
                    "&interval=1min&outputsize=100" +
                    "&apikey=${URLEncoder.encode(key, "UTF-8")}"
                )

                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val response = connection.inputStream
                    .bufferedReader().use { it.readText() }

                connection.disconnect()

                val json = JSONObject(response)
                if (!json.has("values")) {
                    throw Exception(json.optString("message", "No market data"))
                }

                val values = json.getJSONArray("values")
                if (values.length() < 30) {
                    throw Exception("Not enough candle data")
                }

                val prices = mutableListOf<Double>()

                for (i in values.length() - 1 downTo 0) {
                    prices.add(values.getJSONObject(i).getDouble("close"))
                }

                val last = prices.last()
                val e9 = ema(prices, 9)
                val e21 = ema(prices, 21)
                val r = rsi(prices, 14)
                val m = ema(prices, 12) - ema(prices, 26)
                val ms = macdSignal(prices)

                val result = when {
                    last > e9 && e9 > e21 &&
                    r in 52.0..70.0 && m > ms -> "BUY"

                    last < e9 && e9 < e21 &&
                    r in 30.0..48.0 && m < ms -> "SELL"

                    else -> "WAIT"
                }

                runOnUiThread {
                    signal.text = result
                    signal.setTextColor(
                        when (result) {
                            "BUY" -> Color.rgb(60, 210, 120)
                            "SELL" -> Color.rgb(245, 75, 75)
                            else -> Color.YELLOW
                        }
                    )

                    status.text = "Analysis updated: $pair"

                    details.text =
                        "Price: ${fmt(last)}\n" +
                        "EMA 9: ${fmt(e9)}\n" +
                        "EMA 21: ${fmt(e21)}\n" +
                        "RSI 14: ${fmt(r)}\n" +
                        "MACD: ${fmt(m)}\n" +
                        "MACD Signal: ${fmt(ms)}\n\n" +
                        "Indicator-based signal, not a guaranteed prediction."
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun fmt(value: Double) =
        String.format(Locale.US, "%.5f", value)

    private fun ema(data: List<Double>, period: Int): Double {
        val k = 2.0 / (period + 1)
        var result = data.take(period).average()

        for (i in period until data.size) {
            result = data[i] * k + result * (1 - k)
        }
        return result
    }

    private fun rsi(data: List<Double>, period: Int): Double {
        var gain = 0.0
        var loss = 0.0

        for (i in data.size - period until data.size) {
            val change = data[i] - data[i - 1]
            if (change > 0) gain += change
            else loss -= change
        }

        if (loss == 0.0) return 100.0

        val rs = gain / loss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    private fun macdSignal(data: List<Double>): Double {
        val values = mutableListOf<Double>()

        for (i in 25 until data.size) {
            val slice = data.subList(0, i + 1)
            values.add(ema(slice, 12) - ema(slice, 26))
        }

        return ema(values, 9)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        worker.shutdownNow()
        super.onDestroy()
    }
}
