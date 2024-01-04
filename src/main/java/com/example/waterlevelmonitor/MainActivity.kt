package com.example.waterlevelmonitor

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class MainActivity : ComponentActivity() {
    private lateinit var textView : TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarRed: ProgressBar
    private lateinit var progressBarGreen: ProgressBar
    private lateinit var onButton: Button
    private lateinit var offButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.valueText)
        progressBar = findViewById(R.id.progress_bar)
        progressBarRed = findViewById(R.id.progress_bar_red)
        progressBarGreen = findViewById(R.id.progress_bar_green)
        onButton = findViewById(R.id.on_button)
        offButton = findViewById(R.id.off_button)

        onButton.setOnClickListener {
            turnOnOff(1)
        }

        offButton.setOnClickListener {
            turnOnOff(0)
        }

        update()

    }

    private fun update(){
        GlobalScope.launch {
            while (true){

                // For Water Level
                var url = "https://api.thingspeak.com/channels/2281285/feeds.json?api_key=ZXIGU7MAJXM7D6Q3&results=1"

                var value = 0

                var jsonObjectRequest = JsonObjectRequest(
                    Request.Method.GET, url, null,
                    { response ->
                        run {
                            val valueString = response.getJSONArray("feeds").getJSONObject(0).getString("field1")

                            if(isNumeric(valueString))
                                value = Integer.parseInt(valueString)

                            val percentage = "$value%"

                            textView.text = percentage
                            updateProgressBar(value)

                        }
                    },
                    { error -> Toast.makeText(this@MainActivity,"Trying but failing to update level",Toast.LENGTH_SHORT).show()
                    }
                )

                // Access the RequestQueue through your singleton class.
                MySingleton.getInstance(this@MainActivity).addToRequestQueue(jsonObjectRequest)
                delay(1000)

                // For on/off status
                url = "https://api.thingspeak.com/channels/2286514/feeds.json?api_key=3C8IAS79AZHMFGH0&results=1"

                var onOffStatus = 0;

                jsonObjectRequest = JsonObjectRequest(
                    Request.Method.GET, url, null,
                    { response ->
                        run {
                            val isOnString = response.getJSONArray("feeds").getJSONObject(0).getString("field1")

                            if(isNumeric(isOnString))
                                onOffStatus = Integer.parseInt(isOnString)

                            updateButtons(onOffStatus)

                        }
                    },
                    { error -> Toast.makeText(this@MainActivity,"Trying but failing to update ON/OFF",Toast.LENGTH_SHORT).show()
                    }
                )

                // Access the RequestQueue through your singleton class.
                MySingleton.getInstance(this@MainActivity).addToRequestQueue(jsonObjectRequest)
                delay(4000)
            }
        }
    }

    private fun turnOnOff(value: Int){
        updateButtons(value)
        GlobalScope.launch {
            val url = "https://api.thingspeak.com/update?api_key=3W3KLIHQZMVGF0X5&field1=$value"

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.GET, url, null,
                { response -> Toast.makeText(this@MainActivity,response.toString(),Toast.LENGTH_LONG).show()
                },
                { error -> Toast.makeText(this@MainActivity,"Trying but failing to access tank",Toast.LENGTH_SHORT).show()
                }
            )
            // Access the RequestQueue through your singleton class.
            MySingleton.getInstance(this@MainActivity).addToRequestQueue(jsonObjectRequest)
        }
    }

    private fun updateProgressBar(value: Int) {
        if(value<=20){
            progressBarGreen.visibility = View.GONE
            progressBar.visibility = View.GONE
            progressBarRed.visibility = View.VISIBLE
            progressBarRed.progress = value
        }
        else if(value>=90){
            progressBarRed.visibility = View.GONE
            progressBar.visibility = View.GONE
            progressBarGreen.visibility = View.VISIBLE
            progressBarGreen.progress = value
        }
        else{
            progressBarGreen.visibility = View.GONE
            progressBarRed.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            progressBar.progress = value
        }
    }

    private fun updateButtons(isOn: Int){
        if(isOn!=0){
            onButton.visibility = View.GONE
            offButton.visibility = View.VISIBLE
        }
        else{
            onButton.visibility = View.VISIBLE
            offButton.visibility = View.GONE
        }
    }

    private fun isNumeric(toCheck: String): Boolean {
        return toCheck.all { char -> char.isDigit() }
    }
}

